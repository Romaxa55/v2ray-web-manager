package com.jhl.admin.VO;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class GenerateInviteCodeVO implements Serializable {
    /**
     * Количество
     */
    @Range(min = 1,max = 100,message = "Количество сгенерированных пригласительных кодов должно быть в диапазоне от 1 до 100.")
    Integer quantity;
    /**
     * Срок действия
     */
    @NotNull
    private Date effectiveTime;

}
