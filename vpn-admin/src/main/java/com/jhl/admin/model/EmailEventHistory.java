package com.jhl.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Date;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailEventHistory extends BaseEntity implements Serializable {

    private String email;

    private String event;

    /**
     * В следующий раз вы можете отправить
     */
    private Date  unlockDate;





}

