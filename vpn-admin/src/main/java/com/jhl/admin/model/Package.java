package com.jhl.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Package extends BaseEntity implements Serializable {

    private String name;
    //широкополосный доступ
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
    //интервал
    private  Integer interval;

    /**
     * Тип плана
     * стандарт, один и тот же план должен накладываться напрямую, но разные планы не должны накладываться друг на друга.
     * плюс, можно штабелировать с заправочными пакетами
     */
    private String planType;

}

