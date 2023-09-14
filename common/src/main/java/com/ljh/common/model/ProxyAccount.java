package com.ljh.common.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class ProxyAccount implements Serializable {
    public static final  Long M = 1024 * 1024L;
    /**
     * ID учетной записи
     */
    private Integer accountId;

    /**
     * Домен, к которому обращается клиент, проверяется.
     */
    private String host;
    /**
     * Используется для маршрутизации V2Ray.
     */
    private String accountNo;
    /**
     * "id": "fcecbd2b-3a34-4201-bd3d-7c67d89c26ba"
     * uuid
     */
    private String id;
    /**
     * "alterId": 32
     * Совместимо с новой версией alterId 0.
     * AlterID - это функция безопасности в v2ray, предназначенная для предотвращения распознавания и вмешательства. Однако в последних версиях v2ray был внедрен более современный механизм безопасности, и теперь alterID уже не требуется.
     */
    private Integer alterId = 64;
    /**
     * 0
     */
    private Integer level = 0;

    private String email;

    private String inBoundTag;

    private Long upTrafficLimit = 1 * M;

    private Long downTrafficLimit = 1 * M;

    /**
     * Максимальное количество подключений
     */
    private Integer maxConnection = 30;

    private String v2rayHost = "127.0.0.1";
    private int v2rayPort = 6001;
    private int v2rayManagerPort = 62789;
    /**
     * IP прокси-сервера
     */
    private  String proxyIp;


}
