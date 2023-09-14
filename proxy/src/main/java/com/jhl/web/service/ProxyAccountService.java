package com.jhl.web.service;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.jhl.common.constant.ManagerConstant;
import com.jhl.common.pojo.ProxyAccountWrapper;
import com.jhl.common.utils.SynchronousPoolUtils;
import com.jhl.v2ray.service.V2rayService;
import com.ljh.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сохранение во время выполнения
 */
@Slf4j
@Component
public class ProxyAccountService {

    @Autowired
    ManagerConstant managerConstant;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    V2rayService v2rayService;

    private static final Short BEGIN_BLOCK = 3;
    /**
     * Кэширование ProxyAccount
     * ключ: getKey(accountNo, host)
     * значение: ProxyAccount
     */
    public final  static Integer ACCOUNT_EXPIRE_TIME = 60;
    private final Cache<String, ProxyAccountWrapper> PA_MAP = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(ACCOUNT_EXPIRE_TIME, TimeUnit.MINUTES).build();
    /**
     * Предотвращение бесконечного запроса к административному интерфейсу
     */
    private final Cache<String, AtomicInteger> REQUEST_ERROR_COUNT = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(2, TimeUnit.MINUTES).build();

    public void addOrUpdate(ProxyAccountWrapper proxyAccount) {
        if (null == proxyAccount || proxyAccount.getAccountId() == null
        ) throw new NullPointerException("ProxyAccountWrapper is null");

        PA_MAP.put(getKey(proxyAccount.getAccountNo(), proxyAccount.getHost()), proxyAccount);
    }

    /**
     * Если кэш пуст, необходимо получить блокировку аккаунта, затем запросить данные удаленно.
     *
     */
    public ProxyAccountWrapper getProxyAccount(String accountNo, String host) {
        ProxyAccountWrapper proxyAccount = PA_MAP.getIfPresent(getKey(accountNo, host));

        AtomicInteger reqCountObj = REQUEST_ERROR_COUNT.getIfPresent(accountNo);
        int reqCount = reqCountObj == null ? 0 : reqCountObj.get();
        if (proxyAccount == null && reqCount < BEGIN_BLOCK) {
            synchronized (SynchronousPoolUtils.getWeakReference(getKey(accountNo, host + ":getRemotePAccount"))) {
                proxyAccount = PA_MAP.getIfPresent(getKey(accountNo, host));
                if (proxyAccount != null) return proxyAccount;
                // Удаленный запрос для получения информации
                proxyAccount = getRemotePAccount(accountNo, host);
                if (proxyAccount != null) proxyAccount.setVersion(System.currentTimeMillis());
                // Если аккаунт не может быть получен, увеличьте количество ошибок
                if (proxyAccount == null) {
                    AtomicInteger counter = REQUEST_ERROR_COUNT.getIfPresent(accountNo);
                    if (counter != null) {
                        counter.addAndGet(1);
                    } else {
                        REQUEST_ERROR_COUNT.put(accountNo, new AtomicInteger(1));
                    }
                } else {
                    addOrUpdate(proxyAccount);
                    try {
                        // Убедитесь, что аккаунт существует
                        v2rayService.addProxyAccount(proxyAccount.getV2rayHost(), proxyAccount.getV2rayManagerPort(), proxyAccount);
                    } catch (Exception e) {
                        log.warn("Добавление не удалось:{}", e.getLocalizedMessage());
                    }


                }
            }
        }
        if (reqCount >= BEGIN_BLOCK) log.info("Предотвращение удаленного запроса:{}", accountNo);


        return proxyAccount;
    }
    private ProxyAccountWrapper getRemotePAccount(String accountNo, String host) {
        log.info("getRemotePAccount:{}", getKey(accountNo, host));
        HashMap<String, Object> kvMap = Maps.newHashMap();
        kvMap.put("accountNo", accountNo);
        kvMap.put("domain", host);
        ResponseEntity<Result> entity = restTemplate.getForEntity(managerConstant.getGetProxyAccountUrl(),
                Result.class, kvMap);
        if (!entity.getStatusCode().is2xxSuccessful()) {
            log.error("Ошибка getRemotePAccount:{}", entity);
            return null;
        }
        Result result = entity.getBody();
        if (result ==null || result.getCode() != 200) {
            log.warn("getRemotePAccount  error:{}", JSON.toJSONString(result));
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(result.getObj()), ProxyAccountWrapper.class);
    }

    public void rmProxyAccountCache(String accountNo, String host) {
        String key = getKey(accountNo, host);
        PA_MAP.invalidate(key);
    }

    private String getKey(String accountNo, String host) {
        return accountNo + ":" + host;
    }


/*    public boolean containKey(String accountNo, String host) {
        return PA_MAP.getIfPresent(getKey(accountNo, host)) != null;
    }*/

    public boolean interrupted(String accountNo, String host, Long ctxContextVersion) {
        boolean result = true;
        try {
            ProxyAccountWrapper proxyAccountWrapper = PA_MAP.getIfPresent(getKey(accountNo, host));

            if (proxyAccountWrapper != null) {
                Long pxVersion = proxyAccountWrapper.getVersion();

                if (pxVersion != null && pxVersion.equals(ctxContextVersion)) result = false;
            }


        } finally {
            // Удаление кэша
            if (result) rmProxyAccountCache(accountNo, host);
        }
        return result;

    }

    public Long getSize() {
        return PA_MAP.size();
    }

}
