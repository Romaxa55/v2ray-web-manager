package com.jhl.admin.runner;

import com.jhl.admin.model.User;
import com.jhl.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Восстановить пароль администратора на основе файла конфигурации。
 */
@Slf4j
@Component
public class RestPwdRunner implements CommandLineRunner {
    @Autowired
    UserService userService;
    @Value("${admin.email}")
    String email;
    @Value("${admin.password}")
    String password;

    private static final String COMMAND = "RESTPWD";

    @Override
    public void run(String... args) throws Exception {
        Arrays.stream(args).forEach(s -> {
                    if (s.toUpperCase().equals(COMMAND)) {
                        log.info("Выполнена операция сброса пароля администратора, логин в конфигурационном файле: {}, пароль: {}.", email, password);
                        if (!StringUtils.hasText(email)) {
                            log.info("Адрес электронной почты в файле конфигурации пуст. Конец команды сброса");
                            System.exit(-1);
                        }
                        User user = userService.getOneByAdmin(User.builder().email(email).build());

                        if (user == null || user.getId() == null) {
                            log.info("Учетная запись не существует. Создайте новую учетную запись администратора.");
                             user = User.builder().email(email).password(userService.encodePassword(password)).nickName("admin").role("admin").status(1).build();
                        } else {
                            user.setPassword(userService.encodePassword(password));
                        }
                        userService.create(user);
                        log.info("Команда restpwd выполнена успешно, и система завершит работу.");
                        System.exit(0);

                    }
                }
        );
    }
}
