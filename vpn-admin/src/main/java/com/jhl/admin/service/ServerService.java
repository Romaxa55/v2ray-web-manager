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
     * Сохраняет информацию о сервере. Перед добавлением проверяет наличие
     * совпадающего доменного имени в репозитории.
     *
     * @param server Информация о сервере, которую необходимо сохранить.
     * @throws IllegalArgumentException если доменное имя сервера уже существует.
     */
    public void save(Server server) {
        List<Server> all = serverRepository.findAll(Example.of(Server.builder().clientDomain(server.getClientDomain()).build()));
        if (all.size() > 0) {
            throw new IllegalArgumentException("Доменное имя доступа уже существует/the domain name already exists");
        }
        serverRepository.save(server);

    }

    /**
     * Поиск сервера по доменному имени и уровню.
     *
     * @param domain Доменное имя сервера для поиска.
     * @param level  Уровень сервера для фильтрации.
     * @return Информация о найденном сервере.
     * @throws IllegalArgumentException если найдено несколько серверов с одинаковым доменным именем.
     */
    public Server findByDomain(String domain, short level) {
        List<Server> all = serverRepository.findByLevelLessThanEqualAndStatusAndClientDomainOrderByLevelDesc(level, StatusEnum.SUCCESS.code(), domain);
        if (all.size() != 1)
            throw new IllegalArgumentException("1. Существует несколько одинаковых доменных имен, удалите повторяющиеся. 2. Поиск возвращает пустой параметр: домен." + domain + ",level:" + level);
        Server server = all.get(0);
        return server;
    }

    /**
     * Поиск сервера по его идентификатору и статусу.
     *
     * @param id     Идентификатор сервера.
     * @param status Статус сервера.
     * @return Информация о найденном сервере или null, если сервер не найден или его статус не совпадает.
     */
    public Server findByIdAndStatus(Integer id, Integer status) {
        if (status == null) status = StatusEnum.SUCCESS.code();
        Server server = serverRepository.findById(id).orElse(null);
        if (server != null && !server.getStatus().equals(status)) {
            server = null;
        }
        return server;
    }

    /**
     * Обновляет информацию о сервере. Перед обновлением проверяет наличие
     * сервера с аналогичным доменным именем в репозитории.
     *
     * @param server Информация о сервере для обновления.
     * @throws IllegalArgumentException если найден другой сервер с таким же доменным именем.
     */
    public void update(Server server) {
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
