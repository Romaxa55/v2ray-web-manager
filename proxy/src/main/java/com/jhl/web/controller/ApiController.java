package com.jhl.web.controller;

import com.jhl.web.service.ProxyAccountService;
import com.jhl.v2ray.service.V2rayService;
import com.ljh.common.model.ProxyAccount;
import com.ljh.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequestMapping("/proxyApi")
public class ApiController {
    @Autowired
    V2rayService v2rayService;
    @Autowired
    ProxyAccountService proxyAccountService;


    @ResponseBody
    @PostMapping(value = "/account/del")
    public Result rmAccount(@RequestBody ProxyAccount proxyAccount) {
        try {
            if (proxyAccount == null) return Result.builder().code(405).message("accountNo пуст").build();
            String accountNo = proxyAccount.getAccountNo();
            proxyAccountService.rmProxyAccountCache(accountNo,proxyAccount.getHost());
            v2rayService.rmProxyAccount(proxyAccount.getV2rayHost(), proxyAccount.getV2rayManagerPort(), proxyAccount);
        } catch (Exception e) {
            log.error("rmAccount error :{}", e.getLocalizedMessage());
            return Result.builder().code(500).message(e.getMessage()).build();
        }
        return Result.doSuccess();

    }

}
