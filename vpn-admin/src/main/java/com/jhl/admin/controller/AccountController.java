package com.jhl.admin.controller;

import com.google.common.collect.Lists;
import com.jhl.admin.Interceptor.PreAuth;
import com.jhl.admin.VO.AccountVO;
import com.jhl.admin.VO.UserVO;
import com.jhl.admin.cache.UserCache;
import com.jhl.admin.constant.KVConstant;
import com.jhl.admin.constant.enumObject.StatusEnum;
import com.jhl.admin.constant.enumObject.WebsiteConfigEnum;
import com.jhl.admin.entity.V2rayAccount;
import com.jhl.admin.model.Account;
import com.jhl.admin.model.BaseEntity;
import com.jhl.admin.model.Server;
import com.jhl.admin.model.ServerConfig;
import com.jhl.admin.repository.AccountRepository;
import com.jhl.admin.repository.ServerRepository;
import com.jhl.admin.service.AccountService;
import com.jhl.admin.service.ServerConfigService;
import com.jhl.admin.service.ServerService;
import com.jhl.admin.service.SubscriptionService;
import com.jhl.admin.service.v2ray.V2rayAccountService;
import com.jhl.admin.util.Validator;
import com.ljh.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Slf4j
@Controller
public class AccountController {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AccountService accountService;
    @Autowired
    UserCache userCache;
    @Autowired
    ServerService serverService;
    @Autowired
    V2rayAccountService v2rayAccountService;
    @Autowired
    ServerRepository serverRepository;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    ServerConfigService serverConfigService;
    @Autowired
    SubscriptionService subscriptionService;
    /**
     * Завести аккаунт
     * @param account Интерфейс аккаунта
     * @return Завести аккаунт
     */
    @PreAuth("admin")
    @ResponseBody
    @PostMapping("/account")
    public Result createAccount(@RequestBody AccountVO account) {
        if (account == null || account.getUserId() == null) throw new NullPointerException("Не может быть пустым");
        accountService.create(account.toModel(Account.class));
        return Result.doSuccess();
    }

    /**
     * Обновить аккаунт
     * В течение срока действия аккаунта, но трафик пользователя превысил трафик текущего периода.
     * При повторном продлении подписки в принципе существующий платежный цикл не должен изменяться, то есть пользователь по-прежнему не может получить доступ к Интернету.
     * Однако размер пользовательского трафика можно временно изменить, чтобы пользователь мог продолжать пользоваться Интернетом.
     * Подождите, пока система автоматически сгенерирует записи для следующего цикла, а затем измените их обратно.
     *
     * @param account аккаунт
     * @return Успешно обновился
     */
    @PreAuth("admin")
    @ResponseBody
    @PutMapping("/account")
    public Result updateAccount(@RequestBody AccountVO account) {
        if (account == null || account.getId() == null) throw new NullPointerException("Не может быть пустым");
        accountService.updateAccount(account.toModel(Account.class));
        return Result.doSuccess();
    }


    /**
     * Получите учетную запись V2ray на базе сервера.
     *
     * @param serverId ID сервера
     * @return Вощвращаем сервис v2ray
     */
    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/account/v2rayAccount")
    public Result getV2rayAccount(Integer serverId, @CookieValue(KVConstant.COOKIE_NAME) String auth) {
        Validator.isNotNull(serverId);
        UserVO user = userCache.getCache(auth);
        Account account = accountService.getAccount(user.getId());
        if (account == null) return Result.builder().code(500).message("Аккаунт не существует").build();

        Server server = serverService.findByIdAndStatus(serverId, StatusEnum.SUCCESS.code());
        if (server == null) return Result.builder().code(500).message("Сервер не существует").build();

        List<V2rayAccount> v2rayAccounts = v2rayAccountService.buildV2rayAccount(Lists.newArrayList(server), account);
        return Result.buildSuccess(v2rayAccounts.get(0), null);
    }

    /**
     * Изменить учетную запись сервера
     *
     * @param account Изменить учетную запись сервера
     * @return Result.doSuccess
     */
    @PreAuth("vip")
    @ResponseBody
    @PutMapping("/account/server")
    public Result updateAccountServer(@RequestBody AccountVO account) {
        if (account == null || account.getId() == null) throw new NullPointerException("Не может быть пустым");
        accountService.updateAccountServer(account.toModel(Account.class));
        return Result.doSuccess();
    }

    /**
     * Получить список учетных записей пользователей
     *
     * @return Получить список учетных записей пользователей
     */
    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/account/{id}")
    public Result get(@CookieValue(KVConstant.COOKIE_NAME) String auth, @PathVariable Integer id) {
        if (auth == null || userCache.getCache(auth) == null) return Result.builder().code(500).message("认证失败").build();
        UserVO cacheUser = userCache.getCache(auth);
        Integer userId = cacheUser.getId();

       List<AccountVO> accounts = accountService.getAccounts(userId);
        AccountVO account = accounts.get(0);
        String subscriptionUrl = account.getSubscriptionUrl();
        if (StringUtils.isNoneBlank(subscriptionUrl)){
            ServerConfig serverConfig = serverConfigService.getServerConfig(WebsiteConfigEnum.SUBSCRIPTION_ADDRESS_PREFIX.getKey());
            account.setSubscriptionUrl(serverConfig.getValue()+subscriptionUrl);
        }
        return Result.buildSuccess(account,null);
    }

    /**
     * Получить все списки
     *
     * @param page Страница
     * @param pageSize Ее размер
     * @return Получить все списки
     */
    @PreAuth("admin")
    @ResponseBody
    @GetMapping("/account")
    public Result list(Integer page, Integer pageSize, String userEmail) {
        List<Account> accounts = Lists.newArrayList();
        long total = 0l;
        Date date = new Date();
        if (StringUtils.isBlank(userEmail)) {
            Page<Account> accountsPage = accountRepository.findAll(Example.of(Account.builder().build()),
                    PageRequest.of(page - 1, pageSize)
            );

            if (accountsPage.getSize() > 0) {
                accounts = accountsPage.getContent();
            }
            total = accountsPage.getTotalElements();
        } else {
            accounts = accountRepository.findByUserEmail("%" + userEmail + "%");
            total = accounts == null ? 0l : accounts.size();
        }
        List<AccountVO> accountVOList = BaseEntity.toVOList(accounts, AccountVO.class);
        accountVOList.forEach(account -> {
            accountService.fillAccount(date, account);
        });
        return Result.buildPageObject(total,accountVOList );
    }

    /**
     * Создать URL-адрес подписки
     *
     * @param type 0 является общим, 1 или более зарезервированы.
     * @return Создать URL-адрес подписки
     */
    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/account/generatorSubscriptionUrl")
    public Result generatorSubscriptionUrl(@CookieValue(KVConstant.COOKIE_NAME) String auth, Integer type) {
        UserVO user = userCache.getCache(auth);
        Integer accountId = accountService.getAccount(user.getId()).getId();
        accountService.generatorSubscriptionUrl(accountId,type);
        return Result.doSuccess();
    }
}
