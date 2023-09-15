package com.jhl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.security.AccessController;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@Slf4j
public class V2rayProxyApplication {
    private final static String V2RAY_RESTART_COMMAND = "systemctl restart v2ray";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            Runtime.getRuntime().exec(V2RAY_RESTART_COMMAND).waitFor(5, TimeUnit.SECONDS);
            log.info("Выполнение перезапуска v2ray: {}", V2RAY_RESTART_COMMAND);
        } catch (Exception e) {
            log.error("Ошибка при перезапуске v2ray. Если соединение не устанавливается, это, как правило, вызвано порядком запуска. Пожалуйста, перезапустите v2ray вручную.", e);
        }

        // Необходимо принимать args, иначе не будет загружена пользовательская конфигурация
        SpringApplication.run(V2rayProxyApplication.class, args);
    }


}
