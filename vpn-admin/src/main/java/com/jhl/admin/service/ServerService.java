package com.jhl.admin.service;

import com.jhl.admin.constant.enumObject.StatusEnum;
import com.jhl.admin.model.Server;
import com.jhl.admin.repository.ServerRepository;
import com.jhl.admin.util.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServerService {

    @Autowired
    ServerRepository serverRepository;


    public List<Server> listByLevel(Short level) {
        Validator.isNotNull(level);
        List<Server> all = serverRepository.findByLevelLessThanEqualAndStatusOrderByLevelDesc(level, StatusEnum.SUCCESS.code());
        return all;
    }

    /**
     * Перед добавлением сервера проверьте доменное имя.
     *
     * @param server
     */
    public void save(Server server) {
        List<Server> all = serverRepository.findAll(Example.of(Server.builder().clientDomain(server.getClientDomain()).build()));
        if (all.size() > 0) {
            throw new IllegalArgumentException("Доменное имя доступа уже существует/the domain name already exists");
        }
        serverRepository.save(server);

    }

    /**
     * Найти серверы по доменному имени
     *
     * @param domain
     * @return
     */
    public Server findByDomain(String domain, short level) {
        List<Server> all = serverRepository.findByLevelLessThanEqualAndStatusAndClientDomainOrderByLevelDesc(level, StatusEnum.SUCCESS.code(), domain);
        if (all.size() != 1)
            throw new IllegalArgumentException("1. Существует несколько одинаковых доменных имен, удалите повторяющиеся. 2. Поиск возвращает пустой параметр: домен." + domain + ",level:" + level);
        Server server = all.get(0);
        return server;
    }

    public Server findByIdAndStatus(Integer id, Integer status){
        if (status==null) status= StatusEnum.SUCCESS.code();
        Server server = serverRepository.findById(id).orElse(null);
        if (server !=null && !server.getStatus().equals(status)){
            server =null;
        }
        return  server;

    }    public void update(Server server) {
        Server checkServer = null;
        try {
            checkServer = findByDomain(server.getClientDomain(), (short) 9);
        } catch (Exception e) {

        }
        if (checkServer == null || checkServer.getId().equals(server.getId())) {
            serverRepository.save(server);
        } else {
            throw new IllegalArgumentException("Такое же доменное имя уже существует");
        }
    }
}
