package com.jhl.common.pojo;

import com.jhl.common.cache.ConnectionStatsCache;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Статистика подключения к аккаунту.
 * <p>
 * Асинхронное и отложенное учетное ведение статистики. Максимальное стремление к правильности,
 * разрешено грязное чтение данных.
 * </p>
 */
@Slf4j
public class AccountConnectionStat {
    // Константа времени: 5 минут в миллисекундах.
    public final static long _5MINUTE_MS = 5 * 60 * 1000L;
    // Обновление атомарного поля для connectionCounter
    private static final AtomicIntegerFieldUpdater<AccountConnectionStat> CONNECTION_COUNTER_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AccountConnectionStat.class, "connectionCounter");
    // Это поле содержит счетчики подключений по хостам.
    private volatile int connectionCounter;
    // Это поле содержит счетчики подключений по хостам.
    private final ConcurrentHashMap<String, AtomicInteger> hostCounter = new ConcurrentHashMap<>(3);

    // Общее число удаленных подключений.
    private volatile int remoteConnectionNum = 0;
    // Число подключений, сообщенное в последний раз.
    @Getter
    @Setter
    private volatile int lastReportNum = 0;
    // Время последней отчетности.
    @Getter
    @Setter
    private long lastReportTime = 0;
    // Время последнего превышения максимального числа подключений.
    @Getter
    private long interruptionTime = 0;


    /**
     * Добавляет или убавляет значение счетчика.
     *
     * @param count Число, на которое необходимо увеличить или уменьшить значение.
     * @param host Хост для учета соединения.
     */
    public void addAndGet(int count, String host) {
        CONNECTION_COUNTER_UPDATER.addAndGet(this, count);
        if (connectionCounter < 0) {
            log.warn("addAndGet: счетчик подключений к аккаунту меньше 1, возможны проблемы с параллелизмом");
            CONNECTION_COUNTER_UPDATER.set(this, 0);
        }
        //account --> host
        AtomicInteger hostCount = hostCounter.get(host);

        if (hostCount != null) {
            int i = hostCount.addAndGet(count);
            if (i < 0) {
                hostCounter.remove(host);
            }
        } else {
            if (count < 0) return;
            hostCount = new AtomicInteger(count);
            AtomicInteger old = hostCounter.putIfAbsent(host, hostCount);
            if (old != null) old.addAndGet(count);

        }
    }

    /**
     * Проверяет, достигнут ли предел подключений.
     *
     * @param maxConnections Максимальное допустимое число подключений.
     * @return true, если достигнут предел подключений, иначе false.
     */

    public boolean isFull(int maxConnections) {
        if (interruptionTime > 0 && (System.currentTimeMillis() - interruptionTime) < ConnectionStatsCache._1HOUR_MS) {
            return true;
        }
        boolean result = false;
        int total = getByGlobal();
        if (total > maxConnections) result = true;

        return result;
    }

    /**
     * Устанавливает время начала блокировки.
     *
     * @param interruptionTime Время начала блокировки.
     */
    public void setInterruptionTime(long interruptionTime) {
        this.interruptionTime = interruptionTime;
    }

    /**
     * Обновляет общее число удаленных подключений.
     *
     * @param count Общее число удаленных подключений.
     */
    public void updateRemoteConnectionNum(int count) {
        if (count < 1) return;
        this.remoteConnectionNum = count;
    }

    /**
     * Возвращает общее число подключений для всех серверов.
     *
     * @return Общее число подключений.
     */
    public int getByGlobal() {
        //小于5分钟内
        if ((System.currentTimeMillis() - lastReportTime) < _5MINUTE_MS) {
            synchronized (this) {
                int remote = (remoteConnectionNum - lastReportNum);
                if (remote < 0) remote = 0;
                return remote + getByServer();
            }
        }
        return getByServer();
    }

    /**
     * Возвращает число подключений для текущего сервера.
     *
     * @return Число подключений для текущего сервера.
     */
    public int getByServer() {
        return connectionCounter;
    }

    /**
     * Возвращает число подключений для указанного хоста.
     *
     * @param host Хост, для которого необходимо получить число подключений.
     * @return Число подключений для указанного хоста.
     */
    public int getByHost(String host) {
        AtomicInteger counter = hostCounter.get(host);
        return counter == null ? 0 : counter.get();
    }
}
