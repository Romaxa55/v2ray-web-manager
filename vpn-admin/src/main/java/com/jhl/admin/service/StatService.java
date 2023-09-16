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
     * Текущий день является стандартным с даты
     * toDate, from+30= следующий
     * next меньше maxDate
     * Аккаунт @param
     * @возвращаться
     */
    public Stat createOrGetStat(Account account){
        Date today = new Date();
        Stat stat = statRepository.findByAccountIdAndFromDateBeforeAndToDateAfter(account.getId(), today,today);
            if (stat == null){
                synchronized (Utils.getInternersPoll().intern(account.getAccountNo())){
                    stat = statRepository.findByAccountIdAndFromDateBeforeAndToDateAfter(account.getId(), today,today);
                    if (stat != null) return stat;
                    Date fromDate =Utils.formatDate(today,null);

                    Integer cycleNum = account.getCycle();
                    Date maxToDate = account.getToDate();
                    Date nextCycleDate = Utils.getDateBy(fromDate,cycleNum, Calendar.DAY_OF_YEAR);
                        if (!maxToDate.after(fromDate)) return  null;
                    stat = new Stat();
                     stat.setAccountId(account.getId());
                     stat.setFromDate(fromDate);
                     stat.setToDate(nextCycleDate);
                     stat.setFlow(0l);
                     statRepository.save(stat);
                }
            }
            return stat;
    }


}
