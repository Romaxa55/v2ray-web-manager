package com.jhl.common.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jhl.common.pojo.AccountConnectionStat;
import com.jhl.common.utils.SynchronousPoolUtils;
import com.jhl.framework.task.GlobalConnectionStatTask;
import com.jhl.framework.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * Предоставление поддержки для подсчета соединений аккаунта.
 * <p>
 * Глобальная асинхронная синхронизация данных. Максимальное соблюдение корректности, допускаются несогласованные чтения.
 * Гарантируется только окончательная согласованность
 */
@Slf4j
public class ConnectionStatsCache {

    public final static long _1HOUR_MS = 3600000;
    private static final Cache<Object, AccountConnectionStat> ACCOUNT_CONNECTION_COUNT_STATS = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).build();
    private final static long _30S = 30_000;

    public static void incr(String accountId, String host) {
        Assert.notNull(accountId, "accountId must not be null");
        //Присутствует
        AccountConnectionStat accountConnectionStat = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        if (accountConnectionStat != null) {
            accountConnectionStat.addAndGet(1, host);
            return;
        }
        //Не существует.
        synchronized (SynchronousPoolUtils.getWeakReference(accountId + ":connection:" + host)) {

            accountConnectionStat = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
            if (accountConnectionStat != null) {
                accountConnectionStat.addAndGet(1, host);
            } else {
                accountConnectionStat = new AccountConnectionStat();
                accountConnectionStat.addAndGet(1, host);
                ACCOUNT_CONNECTION_COUNT_STATS.put(accountId, accountConnectionStat);
            }

        }


    }

    public static int getByGlobal(String accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        AccountConnectionStat accountConnectionStat = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        return accountConnectionStat == null ? 0 : accountConnectionStat.getByGlobal();
    }

    public static int getBySeverInternal(String accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        AccountConnectionStat accountConnectionStat = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        return accountConnectionStat == null ? 0 : accountConnectionStat.getByServer();
    }

  /*  public void delete(String accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        ACCOUNT_CONNECTION_COUNT_STATS.invalidate(accountId);
        // log.info("connectionCounter  size:{}", ACCOUNT_CONNECTION_COUNT_STATS.size());

    }*/

    public static int getByHost(String accountId, String host) {
        Assert.notNull(accountId, "accountId must not be null");
        AccountConnectionStat accountConnectionStat = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        return accountConnectionStat == null ? 0 : accountConnectionStat.getByHost(host);
    }

    /**
     * <p>Expired entries may be counted in {@link Cache#size}, but will never be visible to read or
     * write operations. Expired entries are cleaned up as part of the routine maintenance described
     * in the class javadoc.
     *
     * @return size
     */
    public static Long getSize() {
        return ACCOUNT_CONNECTION_COUNT_STATS.size();
    }

    public static void decrement(Object accountId, String host) {
        if (accountId == null) return;
        AccountConnectionStat connectionCounter = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        if (connectionCounter != null)
            connectionCounter.addAndGet(-1, host);

    }

    public static boolean isFull(String accountId, int maxConnections) {
        AccountConnectionStat connectionCounter = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
        if (connectionCounter != null) {
            return connectionCounter.isFull(maxConnections);
        }
        return false;
    }

    public static boolean canReport(String accountId) {
        synchronized (SynchronousPoolUtils.getWeakReference(accountId)) {
            AccountConnectionStat connectionCounter = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountId);
            if (connectionCounter != null) {
                long interruptionTime = connectionCounter.getInterruptionTime();
                //Через час после операции можно продолжить выполнение отчета.
                //interruptionTime =0 ok
                // interruptionTime !=0 ok
                if ((System.currentTimeMillis() - interruptionTime) > _1HOUR_MS) {
                    connectionCounter.setInterruptionTime(System.currentTimeMillis());
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Обновляет глобальную статистику соединений для указанной учетной записи.
     *
     * @param accountNo        номер учетной записи, для которой обновляется статистика соединений.
     * @param count            количество соединений для добавления или удаления.
     * @param interruptionTime время прерывания, которое следует установить для учетной записи.
     */
    public static void updateGlobalConnectionStat(String accountNo, int count, long interruptionTime) {
        AccountConnectionStat connectionCounter = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountNo);
        if (connectionCounter != null) {
            connectionCounter.updateRemoteConnectionNum(count);
            //Глобальное управление.
            if (interruptionTime > 0) connectionCounter.setInterruptionTime(interruptionTime);
        } else {
            throw new NullPointerException("AccountConnectionStat is null");
        }

    }

    /**
     * Сообщает текущее количество соединений для указанной учетной записи на текущем сервере.
     *
     * @param accountNo номер учетной записи, для которой сообщается количество соединений.
     * @param proxyIp   IP-адрес прокси-сервера, на котором происходит отчет.
     */
    public static void reportConnectionNum(String accountNo, String proxyIp) {
        AccountConnectionStat connectionCounter = ACCOUNT_CONNECTION_COUNT_STATS.getIfPresent(accountNo);


        if (connectionCounter != null && System.currentTimeMillis() - connectionCounter.getLastReportTime() > _30S) {
            int internalConnectionCount = connectionCounter.getByServer();
            GlobalConnectionStatTask globalConnectionStatTask =
                    new GlobalConnectionStatTask(accountNo, proxyIp, internalConnectionCount);
            TaskService.addTask(globalConnectionStatTask);
            // Обновление данных о последнем отчете.
            connectionCounter.setLastReportNum(internalConnectionCount);
            connectionCounter.setLastReportTime(System.currentTimeMillis());
            log.debug("Сообщить о выполненной задаче...");
        }
    }
}
