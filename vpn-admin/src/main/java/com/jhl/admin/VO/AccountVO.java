package com.jhl.admin.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AccountVO extends BaseEntityVO implements Serializable {
    @Transient
    public final static int G =1024;
    //Срок годности
    private Date fromDate;
    private Date toDate;
    //Тип периода day,week month year？
    private Integer cycle;

    private String accountNo;
    //Kb 1024*1024 =1M
    private Long speed;
    //M，Количество потоков за цикл
    private Integer bandwidth;
    /*//Тип аккаунта
    private  Integer type;*/
    //Связано с учетной записью сервера
    private String content;
    private Integer status;
    private Integer userId;
    private Integer serverId;
    //Максимальное количество подключений для одной учетной записи
    private Integer maxConnection;
    /**
     * 0~9
     * Максимум — 9. Верхний уровень должен иметь возможность получать данные следующего уровня.
     */
    private Short level;

    private  String subscriptionUrl;

    /**
     * идентификатор в v2rayAccount
     */
    private String uuid;
    //Последняя статистика трафика
    private StatVO statVO;
    private ServerVO serverVO;
    private UserVO userVO;





}

