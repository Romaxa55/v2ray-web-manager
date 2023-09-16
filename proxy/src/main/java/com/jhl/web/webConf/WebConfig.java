package com.jhl.web.webConf;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.google.common.collect.Lists;
import com.jhl.web.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Класс конфигурации веб-приложения.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    AuthInterceptor authInterceptor;

    /**
     * Метод для добавления интерцепторов.
     *
     * @param registry Реестр интерцепторов.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Добавляем интерцептор аутентификации ко всем путям.
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    /**
     * Метод для расширения стандартных конвертеров сообщений.
     *
     * @param converters Список конвертеров сообщений.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

        converters.add(0, fastJsonHttpMessageConverter());
    }

    /**
     * Бин для создания конвертера сообщений, использующего FastJson.
     *
     * @return FastJsonHttpMessageConverter.
     */
    @Bean
    public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
        fastJsonHttpMessageConverter.setSupportedMediaTypes(Lists.newArrayList(MediaType.APPLICATION_JSON));
        return fastJsonHttpMessageConverter;
    }

    /**
     * Бин для создания экземпляра RestTemplate с настройками для FastJson.
     *
     * @return Настроенный RestTemplate.
     */
    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Удаление конвертеров, связанных с JSON.
        restTemplate.getMessageConverters().removeIf(httpMessageConverter -> {
            String name = httpMessageConverter.getClass().getName();
            return name.contains("json");
        });
        // Добавление конвертера FastJson.
        restTemplate.getMessageConverters().add(fastJsonHttpMessageConverter());
        return restTemplate;
    }
}
