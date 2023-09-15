package com.jhl.admin.constant.enumObject;

import lombok.Getter;

@Getter
public enum WebsiteConfigEnum {


    IS_NEED_INVITE_CODE("Нужен ли мне код приглашения для регистрации？", "IS_NEED_INVITE_CODE", "false","config"),
    VIP_CAN_INVITE("Могут ли пользователи приглашать других зарегистрироваться?？", "VIP_CAN_INVITE", "false","config"),
    SUBSCRIPTION_ADDRESS_PREFIX("Префикс доступа к адресу подписки","SUBSCRIPTION_ADDRESS_PREFIX","http://127.0.0.1/api","config");
    private String name;

    private String key;

    private String value;

    private String scope;

    WebsiteConfigEnum(String name, String key, String value, String scope) {
        this.name = name;
        this.key = key;
        this.value = value;
        this.scope=scope;
    }




}
