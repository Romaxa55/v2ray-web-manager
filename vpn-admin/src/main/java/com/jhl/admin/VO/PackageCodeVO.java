package com.jhl.admin.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PackageCodeVO extends BaseEntityVO implements Serializable {
    private String code;
    //code Действительное время
    private Date expire;
    //иллюстрировать
    private String desc;
    /**
     * -1: неверно
     *  0: Не используется
     *  1: Уже использовано
     */
    private Integer status;




}

