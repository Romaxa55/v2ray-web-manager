package com.jhl.framework.task.inteface;

import com.alibaba.fastjson.JSON;
import com.jhl.common.constant.ManagerConstant;
import com.jhl.framework.task.TaskCondition;
import com.jhl.framework.task.service.MonitorService;
import com.jhl.framework.task.service.TaskService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Абстрактный класс задачи с отсрочкой. Представляет собой модель актера, где каждая задача отправляется через очередь.
 * Каждая задача внутри этой модели не имеет условий гонки.
 * Поддерживает повторное выполнение и бесконечное выполнение через метод setNextRunTime() {@link MonitorService#init()}.
 * Жизненный цикл следующий: beforeRun -> runTask -> done -> catchException.
 */
@Slf4j
public abstract class AbstractDelayedTask implements Delayed {

    protected String taskName;

    @Setter
    private  Long nextRunTime =System.currentTimeMillis();
    @Setter
    @Getter
    protected TaskCondition taskCondition = TaskCondition.builder().build();




    @Override
    public long getDelay(TimeUnit unit) {

        return unit.convert(nextRunTime-System.currentTimeMillis(), TimeUnit.MILLISECONDS);

    }

    @Override
    public int compareTo(Delayed o) {
        return (int) (this.nextRunTime - ((AbstractDelayedTask) o).nextRunTime);
    }

    /**
     * Выполняется перед запуском задачи.
     */
    public abstract void beforeRun();

    /**
     * Основной метод выполнения задачи.
     *
     * @param restTemplate     инструмент для выполнения HTTP-запросов.
     * @param managerConstant  менеджер констант.
     */
    public abstract void runTask(RestTemplate restTemplate, ManagerConstant managerConstant);

    /**
     * Выполняется после успешного завершения задачи.
     */
    public abstract void done();

    /**
     * Применяет условия выполнения задачи.
     */
    public void attachCondition() {
        TaskCondition taskCondition = getTaskCondition();
        if (taskCondition == null) taskCondition = TaskCondition.builder().build();
        setCondition(taskCondition);
    }

    /**
     * Обрабатывает исключения, возникающие во время выполнения задачи.
     *
     * @param e исключение, которое нужно обработать.
     */
    public abstract void catchException(Exception e);

    /**
     * Устанавливает условия выполнения для задачи.
     *
     * @param taskCondition условия выполнения задачи.
     */
    public abstract void setCondition(TaskCondition taskCondition);

    /**
     * Пытается повторно выполнить задачу в случае неудачи.
     *
     * @param condition условия выполнения для повторного запуска.
     */
    public void tryAgain(TaskCondition condition) {
        // Если задача настроена на бесконечное выполнение, просто пропускаем это.
        if (condition.getMaxFailureTimes() > 0) {
            // Если количество попыток превышает максимальное количество разрешенных попыток.
            if (condition.getFailureTimes() > condition.getMaxFailureTimes()) return;

            condition.setFailureTimes(condition.getFailureTimes() + 1);
        }
        setNextRunTime(System.currentTimeMillis() + condition.computeDelay());

        log.debug("Задача:{}, содержание:{} будет выполнена снова", this.taskName, JSON.toJSONString(this));
        TaskService.addTask(this);
    }

}
