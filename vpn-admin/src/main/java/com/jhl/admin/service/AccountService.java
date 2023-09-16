package com.jhl.admin.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jhl.admin.VO.AccountVO;
import com.jhl.admin.VO.StatVO;
import com.jhl.admin.VO.UserVO;
import com.jhl.admin.constant.KVConstant;
import com.jhl.admin.constant.ProxyConstant;
import com.jhl.admin.entity.V2rayAccount;
import com.jhl.admin.model.*;
import com.jhl.admin.repository.AccountRepository;
import com.jhl.admin.repository.ServerRepository;
import com.jhl.admin.repository.StatRepository;
import com.jhl.admin.service.v2ray.ProxyEvent;
import com.jhl.admin.service.v2ray.ProxyEventService;
import com.jhl.admin.service.v2ray.V2rayAccountService;
import com.jhl.admin.util.Utils;
import com.jhl.admin.util.Validator;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    ServerRepository serverRepository;

    @Autowired
    StatRepository statRepository;
    @Autowired
    ProxyEventService proxyEventService;
    @Autowired
    StatService statService;
    @Autowired
    V2rayAccountService v2rayAccountService;
    @Autowired
    UserService userService;
    @Autowired
    SubscriptionService subscriptionService;


    @Autowired
    private ProxyConstant proxyConstant;
    /**
     * Добавить новую учетную запись
     *
     * @param account account
     * @return Добавить новую учетную запись
     */
    public Account create(Account account) {
        Validator.isNotNull(account.getUserId());
        Date date = new Date();
        if (account.getBandwidth() == null) {
            account.setBandwidth(2);
        }

        account.setAccountNo(Utils.getCharAndNum(7));
        //1024kb/S
        if (account.getSpeed() == null) account.setSpeed(1024L);

        Date fromDate = Utils.formatDate(date, null);
        if (account.getFromDate() == null) account.setFromDate(fromDate);
        if (account.getCycle() == null) {
            account.setCycle(KVConstant.MONTH);
        }
        if (account.getMaxConnection() == null) account.setMaxConnection(64);
        if (account.getToDate() == null)
            account.setToDate(Utils.getDateBy(fromDate, KVConstant.DAY, Calendar.DAY_OF_YEAR));
        account.setStatus(1);
        if (account.getLevel()==null) account.setLevel((short) 0);
        accountRepository.save(account);

        return account;
    }

    /**
     * Обновление информации об учетной записи, не связанное с сервером/контентом.
     * @param account account
     */
    public void updateAccount(Account account) {
        Validator.isNotNull(account.getId());
        account.setContent(null);
        account.setServerId(null);

        accountRepository.save(account);
        Account account1 = accountRepository.findById(account.getId()).orElse(null);
        //Определите, нужно ли создавать новую статистику
        statService.createOrGetStat(accountRepository.getOne(account.getId()));
        //удалить событие
            proxyEventService.addProxyEvent(
                    proxyEventService.buildV2RayProxyEvent(account1, ProxyEvent.RM_EVENT));
    }
    @Deprecated
    @Transactional
    public void updateAccountServer(Account account) {
        Integer id = account.getId();
        Account dbAccount = accountRepository.findById(id).orElse(null);
        if (dbAccount ==null|| dbAccount.getStatus() == 0 || !dbAccount.getToDate().after(new Date())) {
            throw new IllegalStateException("Аккаунт недоступен");
        }



        Integer newServerId = account.getServerId();
        Server newServer = serverRepository.findById(newServerId).orElse(null);
        if (newServer == null) throw new NullPointerException("Сервер пуст");

        List<V2rayAccount> v2rayAccounts = v2rayAccountService.buildV2rayAccount(Lists.newArrayList(newServer), dbAccount);
        if (v2rayAccounts.size() != 1) throw new RuntimeException("Данные неверны");
        account.setContent(JSON.toJSONString(v2rayAccounts.get(0)));
        accountRepository.save(account);


    }

    /**
     * Получите аккаунт под пользователем и заполните его
     *
     * @param userId userId
     * @return Получите аккаунт под пользователем и заполните его
     */
    public List<AccountVO> getAccounts(Integer userId) {

        Date date = new Date();

        List<Account> accounts = accountRepository.findAll(Example.of(Account.builder().userId(userId).build()));
            List<AccountVO> accountVOList = Lists.newArrayListWithCapacity(accounts.size());
        accounts.forEach(account -> {
            AccountVO accountVO = account.toVO(AccountVO.class);
            fillAccount(date, accountVO);
            accountVOList.add(accountVO);

        });
        return accountVOList;
    }

    public Account getAccount(Integer userId) {


        List<Account> accounts = accountRepository.findAll(Example.of(Account.builder().userId(userId).build()));
        if (accounts.size() >1) throw new IllegalArgumentException("У пользователя несколько учетных записей, исправьте их.");
        return accounts.isEmpty()?null:accounts.get(0);
    }
    public void fillAccount(Date date, AccountVO account) {
        Integer accountId = account.getId();
        Stat stat = statRepository.findByAccountIdAndFromDateBeforeAndToDateAfter(accountId, date, date);

        Integer userId = account.getUserId();
        User user = userService.getUserButRemovePW(userId);
        if (user != null) account.setUserVO(user.toVO(UserVO.class));
        if (stat != null) account.setStatVO(stat.toVO(StatVO.class));
    }

    /**
     * @param accountId accountId
     * @param type type
     */

    public void generatorSubscriptionUrl(Integer accountId, Integer type) {
        Subscription    subscription = subscriptionService.findByAccountId(accountId);
            if (subscription==null){
                subscription = Subscription.builder().accountId(accountId).code(subscriptionService.generatorCode()).build();
            }else {
                subscription.setCode(subscriptionService.generatorCode());

            }
        subscriptionService.addSubscription(subscription);

      Account account = accountRepository.findById(accountId).orElse(null);
      Assert.notNull(account,"account is null");
        long timeStamp =System.currentTimeMillis();

        String token = DigestUtils.md5Hex(subscription.getCode()+timeStamp+proxyConstant.getAuthPassword());
        String url = String.format(proxyConstant.getSubscriptionTemplate(), subscription.getCode(), 0, timeStamp, token);
        account.setSubscriptionUrl(url);
        accountRepository.save(account);
    }

    public  Account findByAccountNo(String accountNo){
        Assert.notNull(accountNo,"accountNo must not be null");
        Account account = accountRepository.findOne(Example.of(Account.builder()
                .accountNo(accountNo).status(KVConstant.V_TRUE).build())).orElse(null);
        return account;

    }

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString());
    }


}
