package com.jhl.admin.controller;

import com.google.common.collect.Lists;
import com.jhl.admin.Interceptor.PreAuth;
import com.jhl.admin.VO.AccountVO;
import com.jhl.admin.VO.ServerVO;
import com.jhl.admin.VO.UserVO;
import com.jhl.admin.cache.UserCache;
import com.jhl.admin.constant.KVConstant;
import com.jhl.admin.model.Server;
import com.jhl.admin.repository.ServerRepository;
import com.jhl.admin.service.AccountService;
import com.jhl.admin.service.ServerService;
import com.jhl.admin.util.Validator;
import com.ljh.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
public class ServerController {

    @Autowired
    ServerRepository serverRepository;
    @Autowired
    ServerService serverService;
    @Autowired
    UserCache userCache;
    @Autowired
    AccountService accountService;

    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/server/{id}")
    public Result get(@PathVariable Integer id) {
        Validator.isNotNull(id);
        Server server = serverRepository.findById(id).orElse(null);
        return Result.builder().code(Result.CODE_SUCCESS).obj(server==null?null:server.toVO(ServerVO.class)).build();
    }

    @PreAuth("admin")
    @ResponseBody
    @GetMapping("/server")
    public Result findByPage(Integer page, Integer pageSize, @CookieValue(KVConstant.COOKIE_NAME) String auth) {
        Validator.isNotNull(page);
        Validator.isNotNull(pageSize);
        Page<Server> all = serverRepository.findAll(Example.of(Server.builder().build()), PageRequest.of(page - 1, pageSize));
        ;
        ArrayList<Object> VOList = Lists.newArrayListWithCapacity(all.getContent().size());
        all.getContent().forEach(server -> {
            VOList.add(server.toVO(ServerVO.class));
        });
        return Result.buildPageObject(all.getTotalElements(), VOList);
    }

    @PreAuth("vip")
    @ResponseBody
    @GetMapping("/server/findServersForAccount")
    public Result findServersForAccount(@CookieValue(KVConstant.COOKIE_NAME) String auth) {

        UserVO user = userCache.getCache(auth);
        List<AccountVO> accounts = accountService.getAccounts(user.getId());
        if (accounts.size() != 1) return Result.builder().code(500).message("У пользователя несколько учетных записей/или учетная запись пуста").build();
        AccountVO account = accounts.get(0);
        Short level = account.getLevel();
        List<Server> servers = serverService.listByLevel(level);
        ArrayList<Object> VOList = Lists.newArrayListWithCapacity(servers.size());
        servers.forEach(server -> {
            VOList.add(server.toVO(ServerVO.class));
        });
        return Result.buildSuccess(VOList, null);
    }

    @PreAuth("admin")
    @ResponseBody
    @DeleteMapping("/server/{id}")
    public Result del(@PathVariable Integer id) {
        Validator.isNotNull(id);
        serverRepository.deleteById(id);
        return Result.builder().code(Result.CODE_SUCCESS).build();
    }

    /**
     * Новый
     *
     * @return Новый
     */
    @PreAuth("admin")
    @ResponseBody
    @PostMapping("/server")
    public Result insert(@RequestBody Server server) {
        Validator.isNotNull(server);
        serverService.save(server);
        return Result.doSuccess();
    }

    /**
     * Исправлять
     */
    @PreAuth("admin")
    @ResponseBody
    @PutMapping("/server")
    public Result update(@RequestBody Server server) {
        Validator.isNotNull(server);


       serverService.update(server);

        //todo Логика после изменения сервера 1. Обновить учетную запись 2. Отправить на промежуточное программное обеспечение
        return Result.builder().code(Result.CODE_SUCCESS).build();
    }
}
