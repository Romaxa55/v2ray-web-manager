package com.jhl.admin.controller;

import com.jhl.admin.constant.ProxyConstant;
import com.jhl.admin.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Controller
public class SubscriptionController {
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    private ProxyConstant proxyConstant;

    /**
     * Обрабатывает запросы на подписку и защищает от вмешательства посредников.
     *
     * @param code      уникальный код подписки
     * @param type      Тип подписки: 0 — общий, 1... — зарезервированные значения.
     * @param timestamp временная метка запроса
     * @param token     токен для аутентификации, полученный из комбинации code, timestamp и api.auth
     * @param response  объект HTTP-ответа для передачи данных
     * @throws NullPointerException если один из обязательных параметров (code, type, timestamp или token) не предоставлен
     * @throws RuntimeException если аутентификация токена не проходит проверку
     * @throws IOException если происходит ошибка при записи данных в ответ
     */

    @RequestMapping(value = "/subscribe/{code}")
    public void subscribe(@PathVariable String code, Integer type, Long timestamp, String token, HttpServletResponse response) throws IOException {
        if (code == null || type == null || timestamp == null || token == null)
            throw new NullPointerException("Ошибка параметра");

        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder tokenSrc = stringBuilder.append(code).append(timestamp).append(proxyConstant.getAuthPassword());
        if (!DigestUtils.md5Hex(tokenSrc.toString()).equals(token))
            throw new RuntimeException("Аутентификация не удалась");

        String result = subscriptionService.subscribe(code);
        byte[] bytes = result.getBytes();
        response.setHeader("Content-Length", String.valueOf(bytes.length));
        response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }
}
