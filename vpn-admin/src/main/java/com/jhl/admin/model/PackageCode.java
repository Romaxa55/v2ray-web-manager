package com.jhl.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Date;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PackageCode extends BaseEntity implements Serializable {
    @Column(unique = true)
    private String code;
    //code Действительное время
    private Date expire;
    //иллюстрировать
    private String desc;
    /**
     * -1: неверно
     * 0: Не используется
     * 1: Уже использовано
     */
    private Integer status;




}

