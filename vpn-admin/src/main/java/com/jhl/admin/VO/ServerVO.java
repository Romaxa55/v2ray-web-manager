package com.jhl.admin.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServerVO extends BaseEntityVO implements Serializable {


    private String serverName;
    private String clientDomain;
    private Integer clientPort = 443;
    private Boolean supportTLS = true;

    //IP и порт для управления прокси-посредником.

    private String proxyIp = "127.0.0.1";
    private Integer proxyPort = 8091;
    //Открытые IP и порты для V2ray
    private String v2rayIp = "127.0.0.1";
    private Integer v2rayManagerPort=62789;
    private Integer v2rayPort = 6001;




    private String protocol;

    //Множитель трафика."
    private Double multiple;

    //инструкциями
    private String desc;
    //Состояние сервера
    private Integer status;

    private String inboundTag;

    /**
     * Серверный статус
     */
    private  Short level;


    // WS-путь
    private String wsPath ="/ws/%s/";

    private Integer  alterId=64;


}

