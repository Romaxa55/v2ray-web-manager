package com.jhl.admin.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Table;
import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_transaction")
public class TransactionVO extends BaseEntityVO implements Serializable {

    //Тип операции: наличные или с использованием кода пакета
    private Integer transType;

    private Integer transStatus;
    private Long price;
}

