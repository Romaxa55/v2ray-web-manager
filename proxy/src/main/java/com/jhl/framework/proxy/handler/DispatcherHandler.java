package com.jhl.framework.proxy.handler;

import com.jhl.common.cache.ConnectionStatsCache;
import com.jhl.common.cache.TrafficControllerCache;
import com.jhl.common.constant.ProxyConstant;
import com.jhl.common.pojo.ConnectionLimit;
import com.jhl.common.pojo.ProxyAccountWrapper;
import com.jhl.common.utils.SynchronousPoolUtils;
import com.jhl.framework.proxy.exception.ReleaseDirectMemoryException;
import com.jhl.framework.task.FlowStatTask;
import com.jhl.framework.task.TaskConnectionLimitDelayedTask;
import com.jhl.framework.task.service.TaskService;
import com.jhl.web.service.ProxyAccountService;
import com.ljh.common.model.FlowStat;
import com.ljh.common.model.ProxyAccount;
import com.ljh.common.utils.V2RayPathEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

@Slf4j
public class DispatcherHandler extends ChannelInboundHandlerAdapter {


    private static final String HOST = "HOST";
    private static final Long MAX_INTERVAL_REPORT_TIME_MS = 1000 * 60 * 5L;
    /**
     * Данные конфигурации на стороне прокси
     */
    final ProxyConstant proxyConstant;


    private Channel outboundChannel;

    private String accountNo;

    private ProxyAccountService proxyAccountService;

    private String host;

    private boolean isHandshaking = true;

    private Long version = null;

    private String proxyIp = null;

    public DispatcherHandler(ProxyConstant proxyConstant, ProxyAccountService proxyAccountService) {
        this.proxyConstant = proxyConstant;
        this.proxyAccountService = proxyAccountService;

    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        // log.info("Dispatcher len:"+((ByteBuf)msg).readableBytes()+"B");
        if (isHandshaking) {
            parse(ctx, msg);
            return;
        }

            try {
                if (proxyAccountService.interrupted(accountNo, host, version))
                    throw new ReleaseDirectMemoryException("[Текущая версия обновлена] Выдается исключение. Единый выпуск памяти");

                writeToOutBoundChannel(msg, ctx);
                //асинхронный
                ConnectionStatsCache.reportConnectionNum(accountNo, proxyIp);
                //асинхронный
                reportFlowStat();
            } catch (Exception e) {
                if (!(e instanceof ReleaseDirectMemoryException)) {
                    log.error("Исключение произошло при взаимодействии данных：", e);
                }
                release((ByteBuf) msg);
                closeOnFlush(ctx.channel(),outboundChannel);
            }




    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("active");
        ctx.read();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!(cause instanceof IOException)) log.error("exceptionCaught:", cause);

        closeOnFlush(ctx.channel(),outboundChannel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }

        if (accountNo == null) return;

        //Уменьшите количество ссылок на каналы
        ConnectionStatsCache.decrement(accountNo, host);

        if (proxyIp != null) ConnectionStatsCache.reportConnectionNum(accountNo, proxyIp);

       /* log.info("{}После того, как учетная запись закроет соединение: {}, сервер: {}, глобальный:{}", getAccountId(), accountConnections,
                ConnectionStatsCache.getBySeverInternal(accountNo),
                ConnectionStatsCache.getByGlobal(accountNo));*/


        if (ConnectionStatsCache.getByHost(accountNo, host) < 1) {
            GlobalTrafficShapingHandler globalTrafficShapingHandler = TrafficControllerCache.getGlobalTrafficShapingHandler(getAccountId());
            if (globalTrafficShapingHandler == null) return;
            TrafficCounter trafficCounter = globalTrafficShapingHandler.trafficCounter();
            long writtenBytes = trafficCounter.cumulativeWrittenBytes();
            long readBytes = trafficCounter.cumulativeReadBytes();
            //统计流量
            reportFlowStat(writtenBytes, readBytes);
            log.info("Аккаунт: {}, текущий сервер полностью отключен, накоплено байт: {}B", getAccountId(), writtenBytes + readBytes);
            //   log.info("当前{},累计读字节:{}", accountNo, readBytes);
            TrafficControllerCache.releaseGroupGlobalTrafficShapingHandler(getAccountId());
            // ConnectionStatsService.delete(getAccountId());
        }

        closeOnFlush(ctx.channel());
    }

    /**
     *
     * 解析
     * PooledUnsafeDirectByteBuf(ridx: 0, widx: 188, cap: 1024)
     * <p>
     * GET /ws/50001:token/ HTTP/1.1
     * Host: 127.0.0.1:8081
     * User-Agent: Go-http-client/1.1
     * Connection: Upgrade
     * Sec-WebSocket-Key: 90rYhIPctMP+ykUzA6QLrA==
     * Sec-WebSocket-Version: 13
     * Upgrade: websocket
     */
    private void parse(ChannelHandlerContext ctx, Object msg) {


        ByteBuf handshakeByteBuf;
        try {

            handshakeByteBuf = convert(ctx, msg);

        } catch (Exception e) {
            if (!(e instanceof ReleaseDirectMemoryException))
                log.warn("Произошла ошибка на этапе синтаксического анализа.:{},e:{}", ((ByteBuf) msg).toString(Charset.defaultCharset()), e.getLocalizedMessage());

            closeOnFlush(ctx.channel(),outboundChannel);
            return;
        } finally {
            //Освободите данные рукопожатия, чтобы предотвратить переполнение памяти.
            ReferenceCountUtil.release(msg);
        }
        //step2
        try {
            // Получить прокси-аккаунт
            ProxyAccountWrapper proxyAccount = getProxyAccount();

            if (proxyAccount == null || isFull(proxyAccount)) {
                    throw new IllegalAccessException("Невозможно получить учетную запись или количество подключений заполнено");
            }
            log.info("Текущий аккаунт: {}, количество подключений: {}, количество подключений к серверу: {}, количество глобальных подключений: {}", getAccountId(),
                    ConnectionStatsCache.getByHost(accountNo, host),
                    ConnectionStatsCache.getBySeverInternal(accountNo)
                    , ConnectionStatsCache.getByGlobal(accountNo));
            proxyIp = proxyAccount.getProxyIp();
            attachTrafficController(ctx, proxyAccount);

            sendNewPackageToClient(ctx, handshakeByteBuf, ctx.channel(), proxyAccount);

        } catch (Exception e) {
            log.error("Ошибка при установлении соединения с v2ray", e);
            release(handshakeByteBuf);
            closeOnFlush(ctx.channel(),outboundChannel);
        } finally {
            isHandshaking = false;
        }
    }

    private void release(ByteBuf msg) {
        if (msg == null) return;
        if (msg.refCnt() > 0) {
            msg.release(msg.refCnt());
        }
    }

    /**
     * Анализ данных рукопожатия и создание новых данных рукопожатия
     */
    private ByteBuf convert(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        ByteBuf byteBuf = ((ByteBuf) msg);
        String heads = byteBuf.toString(Charset.defaultCharset());

        String[] headRows = heads.split("\r\n");
        getHost(headRows);
        //GET /ws/50001:token/ HTTP/1.1
        String[] requestRow = headRows[0].split(" ");

        //50001:token/
        String[] accountNoAndToken = requestRow[1].split("/")[2].split(":");

        if (accountNoAndToken.length < 2) throw new NullPointerException("Доступ к старой версии больше не поддерживается.");

        accountNo = accountNoAndToken[0];

        String token = accountNoAndToken[1];

        checkToken(token);
        // /ws/50001:token/ ,Найти каталог
        String directory = requestRow[1];
        int directoryLen = directory.length();
        //+1 Потому что: приходится 1
        int tokenLen = token.length() + accountNo.length() + 1;
        // /ws/
        String newHeadPackage = heads.replaceAll(directory, directory.substring(0, directoryLen - (tokenLen + 1)));
        //Сформированные новые данные рукопожатия
        // log.info("dispatcher:{}",ctx.alloc().getClass() , ctx.alloc().buffer());
        return ctx.alloc().buffer().writeBytes(newHeadPackage.getBytes());
    }

    /**
     * Определить, превышено ли максимальное количество подключений
     *
     * @param proxyAccount ProxyAccount
     * @return true is full
     */
    private boolean isFull(ProxyAccount proxyAccount) {
        ConnectionStatsCache.incr(accountNo, host);
        ConnectionStatsCache.reportConnectionNum(accountNo, proxyAccount.getProxyIp());
        int globalConnections = ConnectionStatsCache.getByGlobal(accountNo);

        Integer maxConnection = proxyAccount.getMaxConnection();
        boolean full = ConnectionStatsCache.isFull(accountNo, maxConnection);
        int currentMaxConnection = full ? Integer.valueOf(maxConnection / 2) : maxConnection;

        if (globalConnections > currentMaxConnection) {
            reportConnectionLimit();
            log.warn("Сработал верхний предел максимального количества подключений. Текущее максимально допустимое значение: {}，" +
                    "Только половине максимального количества глобальных подключений в учетной записи будет разрешен доступ в течение следующего часа.", currentMaxConnection);
            return true;
        }
        return false;
    }

    private ProxyAccountWrapper getProxyAccount() {
        ProxyAccountWrapper proxyAccount = proxyAccountService.getProxyAccount(accountNo, host);
        if (proxyAccount == null) {
            log.warn("Невозможно получить аккаунт...");
            //ReferenceCountUtil.release(handshakeByteBuf);
            //  closeOnFlush(ctx.channel());
            return null;
        }
        version = proxyAccount.getVersion();
        return proxyAccount;
    }

    /**
     * Добавьте соответствующий TrafficController в канал
     *
     * @param ctx          ChannelHandlerContext
     * @param proxyAccount ProxyAccount
     */
    private void attachTrafficController(ChannelHandlerContext ctx, ProxyAccountWrapper proxyAccount) {
        Long readLimit = proxyAccount.getUpTrafficLimit() * 1000;
        Long writeLimit = proxyAccount.getDownTrafficLimit() * 1000;
        //Запускаем максимальное количество подключений и штрафно уменьшаем количество подключений на 1 час
         //Добавляем управление потоком
         //Сохранять глобальный контроль и не изменять ключ
        GlobalTrafficShapingHandler orSetGroupGlobalTrafficShapingHandler = TrafficControllerCache.putIfAbsent(getAccountId(), ctx.executor(), readLimit, writeLimit);
        //Потому что нет fireChannel
        ctx.pipeline().addFirst(orSetGroupGlobalTrafficShapingHandler);
    }

    /**
     * Отправьте данные рукопожатия и обновите протокол ws
     */
    private void sendNewPackageToClient(ChannelHandlerContext ctx, final ByteBuf handshakeByteBuf, Channel inboundChannel, ProxyAccount proxyAccount) {
        Bootstrap client = NettyClientFactory.getClient(inboundChannel.eventLoop());
        ChannelFuture f = client.connect(proxyAccount.getV2rayHost(), proxyAccount.getV2rayPort());
        outboundChannel = f.channel();
        outboundChannel.pipeline().addLast(new ReceiverHandler(inboundChannel));
        //Success or failure
        f.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                writeToOutBoundChannel(handshakeByteBuf, ctx);
            } else {
                release(handshakeByteBuf);
                closeOnFlush(inboundChannel,outboundChannel);

            }
        });
    }

    /*private Bootstrap getMuxClient(Channel inboundChannel) {

     *//* Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(//new SafeByteToMessageDecoder(),
                                new Receiver(inboundChannel));
                    }
                })
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                .option(ChannelOption.SO_RCVBUF, 32 * 1024)

                //32k/64k
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);

        return b;*//*

    }*/

    private static class NettyClientFactory {
        private static Bootstrap b = null;

        public static Bootstrap getClient(EventLoop eventLoop) {
            if (b != null) return b;
            synchronized (NettyClientFactory.class) {
                if (b != null) return b;
                b = new Bootstrap();
                b.group(eventLoop)
                        .channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter());
                    }
                })
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .option(ChannelOption.AUTO_READ, false)
                        .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                        .option(ChannelOption.SO_RCVBUF, 32 * 1024)
                        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                        .option(ChannelOption.TCP_NODELAY, true);
            }
            return b;

        }
    }

    /**
     * Получить информацию заголовка хоста
     */
    private void getHost(String[] headRows) {

        for (String head : headRows) {
            // :космос
            String[] headKV = head.split(": ");
            if (headKV.length != 2) continue;
            if (headKV[0].trim().toUpperCase().equals(HOST)) {
                host = headKV[1].trim();
                String[] ipAndPort = host.split(":");
                host = ipAndPort[0];
                break;
            }
        }

        if (host == null) throw new NullPointerException("Невозможно получить информацию о хосте");
    }

    private void checkToken(String requestToken) throws IllegalAccessException {
        String token = V2RayPathEncoder.encoder(accountNo, host, proxyConstant.getAuthPassword());
        if (!requestToken.equals(token)) throw new IllegalAccessException("Недопустимый доступ, проверка токена не прошла.");
    }

    private String getAccountId() {
        return accountNo + ":" + host + ":" + version;
    }


    private void writeToOutBoundChannel(Object msg, final ChannelHandlerContext ctx) {
        outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
            } else {
                //out
              closeOnFlush(future.channel(),ctx.channel());

            }
        });
    }


    private void reportConnectionLimit() {

        if (ConnectionStatsCache.canReport(accountNo)) {
            //Предупреждение о ограничении соединений.
            TaskConnectionLimitDelayedTask reportConnectionLimitTask =
                    new TaskConnectionLimitDelayedTask(ConnectionLimit.builder().accountNo(accountNo).build());
            TaskService.addTask(reportConnectionLimitTask);
        }
    }


    /**
     * Поступление трафика по частям.
     */
    private void reportFlowStat() {


        if (ConnectionStatsCache.getByHost(accountNo, host) < 1) return;
        TrafficCounter trafficCounter = TrafficControllerCache.getGlobalTrafficShapingHandler(getAccountId()).trafficCounter();
        if (System.currentTimeMillis() - trafficCounter.lastCumulativeTime() >= MAX_INTERVAL_REPORT_TIME_MS) {

            synchronized (SynchronousPoolUtils.getWeakReference(accountNo + ":reportStat")) {
                if (System.currentTimeMillis() - trafficCounter.lastCumulativeTime() >= MAX_INTERVAL_REPORT_TIME_MS) {
                    long writtenBytes = trafficCounter.cumulativeWrittenBytes();
                    long readBytes = trafficCounter.cumulativeReadBytes();
                    reportFlowStat(writtenBytes, readBytes);
                    //重置
                    trafficCounter.resetCumulativeTime();
                    log.info("Аккаунт: {}, соединение превышает 5 минут. Загрузка статистики данных по сегментам трафика:{}B", getAccountId(), writtenBytes + readBytes);
                }

            }

        }
    }


    private void reportFlowStat(long writtenBytes, long readBytes) {
        FlowStat flowStat = new FlowStat();
        flowStat.setDomain(host);
        flowStat.setAccountNo(accountNo);
        flowStat.setUsed(writtenBytes + readBytes);
        flowStat.setUniqueId(UUID.randomUUID().toString());
        FlowStatTask reportFlowStatTask = new FlowStatTask(flowStat);
        TaskService.addTask(reportFlowStatTask);
    }


    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel... chs) {
        if (chs ==null) return;

        for (Channel ch:chs){
        if (ch!=null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        }
    }


}
