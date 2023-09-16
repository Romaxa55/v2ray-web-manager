package com.jhl.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Server extends BaseEntity implements Serializable {


    private String serverName;
    private String clientDomain;
    private Integer clientPort = 443;
    private Boolean supportTLS = true;

    //IP-порт управления промежуточным программным обеспечением прокси;

    private String proxyIp = "127.0.0.1";
    private Integer proxyPort = 8091;
    //v2ray открыть IP и порт
    private String v2rayIp = "127.0.0.1";
    private Integer v2rayManagerPort=62789;
    private Integer v2rayPort = 6001;




    private String protocol;

    //Несколько трафиков
    private Double multiple;

    //иллюстрировать
    private String desc;
    //Состояние сервера
    private Integer status;

    private String inboundTag;

    /**
     * Уровень сервера
     */
    @Column(  columnDefinition="smallint default 0" )
    private  Short level;


    //ws路径
    private String wsPath ="/ws/%s/";
    //默认0
    @Column(columnDefinition="smallint default 64" )
    private Integer alterId=64;

}

