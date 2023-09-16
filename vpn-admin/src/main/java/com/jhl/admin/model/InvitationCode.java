package com.jhl.admin.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Date;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
/**
 * объявление
 */
public class InvitationCode extends BaseEntity implements Serializable {

    private Integer generateUserId;
    private Integer regUserId;
    @Column(unique = true)
    private String  inviteCode;
    //Эффективное время
    @Column()
    private Date effectiveTime;
    private  Integer status;



}
