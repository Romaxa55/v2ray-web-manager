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
public class StatVO extends BaseEntityVO implements Serializable {

    private Integer accountId;
    //Цикл расчета
    private Date fromDate;

    private Date toDate;
    //Максимальное значение трафика - 2^64, что эквивалентно 1 Тб, равному 2^40.
    private Long flow;
}

