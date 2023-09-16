package com.jhl.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data

public class Account extends BaseEntity implements Serializable {
    @Transient
    public final static int G =1024;
    //Срок годности
    private Date fromDate;
    private Date toDate;
    //Тип периода day,week month year？
    private Integer cycle;

    @Column(unique = true,nullable = false)
    private String accountNo;
    //Kb 1024*1024 =1M
    private Long speed;
    //M，Количество потоков за цикл
    private Integer bandwidth;
    /*//Тип аккаунта
    private  Integer type;*/
    //Связано с учетной записью сервера
    @Column(length = 512)
    private String content;
    private Integer status;
    private Integer userId;
    private Integer serverId;
    //Максимальное количество подключений для одной учетной записи
    private Integer maxConnection;
    /**
     * 0~9
     *Максимум — 9. Верхний уровень должен иметь возможность получать данные следующего уровня.
     */
    @Column(  columnDefinition="smallint default 0")
    private Short level;

    private  String subscriptionUrl;

    /**
     * v2rayAccount中的id
     */
    private String uuid;
    @Transient
    //Последняя статистика трафика
    private Stat stat;
    @Transient
    private Server server;
    @Transient
    private User user;





}

