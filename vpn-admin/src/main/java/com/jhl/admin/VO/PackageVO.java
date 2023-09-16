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
public class PackageVO extends BaseEntityVO implements Serializable {

    private String name;
    // Широкополосное соединение
    private Integer bandwidth;

    private Integer speed;

    private  Integer  connections;
    //цикл
    /**
     *0 1 30
     */
    private Integer cycle;
    //иллюстрировать
    private String description;

    private Integer status;

    private Integer price;

    private  Integer show;
    //объясните
    private  Integer interval;

    /**
     * Тип плана
     * standard (стандарт) - одинаковые планы могут быть накладываемыми друг на друга, разные планы не могут быть накладываемыми друг на друга.
     * plus (плюс) - можно добавлять дополнительные пакеты.
     */
    private String planType;

}

