package com.jhl.framework.proxy;


import com.jhl.common.constant.ProxyConstant;
import com.jhl.framework.proxy.handler.DispatcherHandler;
import com.jhl.framework.task.service.TaskService;
import com.jhl.web.service.ProxyAccountService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * a proxyServer starter
 */
@Slf4j
@Component
public final class ProxyServer {

    @Autowired
    ProxyConstant proxyConstant;
    @Autowired
    ProxyAccountService proxyAccountService;
    private static EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("boss"));
    private static EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));

    @PostConstruct
    public void initNettyServer() {


        // Configure the bootstrap.
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    //TCP уровень: буфер отправки размером 32 Кб
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)
                    //Уровень TCP: буфер приема размером 32 Кб
                    .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                    //Параметры кэширования на уровне сервиса Netty.
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                    // Нет гарантии приоритетной пересылки
                    .childOption(ChannelOption.IP_TOS, 0xB8);
            //
            //В качестве промежуточного программного обеспечения контрольный сигнал не включается и не делегируется реальному сервису.
            // Heartbeat требует поддержания активности на транспортном уровне + обнаружение пульса на уровне приложения.
            // .childOption(ChannelOption.SO_KEEPALIVE,true)
            //.childOption(NioChannelOption.of(StandardSocketOptions.SO_KEEPALIVE),true);
            // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //    .handler(new LoggingHandler(LogLevel.ERROR))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new DispatcherHandler(proxyConstant, proxyAccountService));
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(proxyConstant.getLocalPort()).sync()
                    .addListener((ChannelFutureListener) future -> log.info("Proxying on:" + proxyConstant.getLocalPort() + " ..."));


        } catch (Exception e) {
            log.error("netty start exception:", e);
        }
    }


    @PreDestroy
    public void preDestroy() throws InterruptedException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully().addListener(future -> {
            log.warn("ReportService Закрыто...");
            TaskService.destroy();
        });
        workerGroup.awaitTermination(3, TimeUnit.SECONDS);
        log.warn("netty Закрыто...");


    }
}
