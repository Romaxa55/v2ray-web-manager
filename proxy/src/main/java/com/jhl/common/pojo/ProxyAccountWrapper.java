package com.jhl.common.pojo;

import com.ljh.common.model.ProxyAccount;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProxyAccountWrapper extends ProxyAccount {
    /**
     * Номер версии
     */
    private  Long version;



}
