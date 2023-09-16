package com.jhl.admin.service;

import com.jhl.admin.model.Account;
import com.jhl.admin.model.Stat;
import com.jhl.admin.repository.StatRepository;
import com.jhl.admin.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class StatService {
    @Autowired
    StatRepository statRepository;


    /**
     * Получает статистику для указанного аккаунта на текущую дату. Если статистика отсутствует, создает новую запись.
     * При создании новой записи учитывается цикл аккаунта и максимальная дата аккаунта.
     *
     * @param account Аккаунт, для которого необходимо получить или создать статистику.
     * @return Статистика аккаунта на текущую дату или null, если максимальная дата аккаунта меньше текущей даты.
     */
    public Stat createOrGetStat(Account account) {
        Date today = new Date();
        Stat stat = statRepository.findByAccountIdAndFromDateBeforeAndToDateAfter(account.getId(), today, today);
        if (stat == null) {
            synchronized (Utils.getInternersPoll().intern(account.getAccountNo())) {
                stat = statRepository.findByAccountIdAndFromDateBeforeAndToDateAfter(account.getId(), today, today);
                if (stat != null) return stat;
                Date fromDate = Utils.formatDate(today, null);

                Integer cycleNum = account.getCycle();
                Date maxToDate = account.getToDate();
                Date nextCycleDate = Utils.getDateBy(fromDate, cycleNum, Calendar.DAY_OF_YEAR);
                if (!maxToDate.after(fromDate)) return null;
                stat = new Stat();
                stat.setAccountId(account.getId());
                stat.setFromDate(fromDate);
                stat.setToDate(nextCycleDate);
                stat.setFlow(0L);
                statRepository.save(stat);
            }
        }
        return stat;
    }


}
