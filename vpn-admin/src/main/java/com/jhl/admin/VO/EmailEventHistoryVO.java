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
public class EmailEventHistoryVO extends BaseEntityVO implements Serializable {

    private String email;

    private String event;

    /**
     * В следующий раз вы можете отправить
     */
    private Date  unlockDate;





}

