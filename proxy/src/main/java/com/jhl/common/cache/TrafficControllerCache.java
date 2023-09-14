package com.jhl.common.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jhl.common.utils.SynchronousPoolUtils;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
TrafficControllerService обеспечивает контроль за трафиком.
Каждая учетная запись владеет глобальным {@link GlobalTrafficShapingHandler} на протяжении своего срока службы.
Задача на будущее: распределенное управление трафиком.
 */
@Slf4j
public class TrafficControllerCache {

    private static final Cache<Object, GlobalTrafficShapingHandler> ACCOUNT_TRAFFIC_HANDLER_MAP = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();


    /**
     * Для каждой учетной записи добавляется или получается существующий {@link GlobalTrafficShapingHandler},
     *
     * @param accountId  accountId
     * @param executor   Пул потоков с возможностью установки периодических задач.
     * @param readLimit  Ограничение скорости чтения.
     * @param writeLimit Ограничение скорости записи.
     * @return GlobalTrafficShapingHandler
     */
    public static GlobalTrafficShapingHandler putIfAbsent(Object accountId, ScheduledExecutorService executor, Long readLimit, Long writeLimit) {
        Assert.notNull(accountId, "accountId must not be null");

        GlobalTrafficShapingHandler trafficShapingHandler = ACCOUNT_TRAFFIC_HANDLER_MAP.getIfPresent(accountId);
        if (trafficShapingHandler != null) return trafficShapingHandler;

        synchronized (SynchronousPoolUtils.getWeakReference(accountId + ":acquireGlobalTrafficShapingHandler")) {
            trafficShapingHandler = ACCOUNT_TRAFFIC_HANDLER_MAP.getIfPresent(accountId);
            if (trafficShapingHandler != null) return trafficShapingHandler;
            trafficShapingHandler = new GlobalTrafficShapingHandler(executor, writeLimit, readLimit);
            ACCOUNT_TRAFFIC_HANDLER_MAP.put(accountId, trafficShapingHandler);
        }
        return trafficShapingHandler;
    }


    /**
     * Должен получать блокировку, но это необязательно.
     *
     * @param accountId id
     * @return GlobalTrafficShapingHandler
     */
    public static GlobalTrafficShapingHandler getGlobalTrafficShapingHandler(Object accountId) {

        Assert.notNull(accountId, "accountId must not be null");
        return ACCOUNT_TRAFFIC_HANDLER_MAP.getIfPresent(accountId);
    }

    /**
     * @param accountId id

     */
    public static void releaseGroupGlobalTrafficShapingHandler(Object accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        GlobalTrafficShapingHandler globalTrafficShapingHandler = ACCOUNT_TRAFFIC_HANDLER_MAP.getIfPresent(accountId);
        if (globalTrafficShapingHandler != null) {
            globalTrafficShapingHandler.release();
        }
        ACCOUNT_TRAFFIC_HANDLER_MAP.invalidate(accountId);
    }

    public static Long getSize() {

      /*  if (size >= 20) {
            StringBuilder sb = new StringBuilder();
            ACCOUNT_TRAFFIC_HANDLER_MAP.asMap().forEach((key, value) -> sb.append(key.toString()).append("|"));
            log.warn("TrafficControllerCache is too big >=20 :{}", sb.toString());
        }*/

        return ACCOUNT_TRAFFIC_HANDLER_MAP.size();
    }
}
