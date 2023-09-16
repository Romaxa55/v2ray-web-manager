package com.jhl.admin.constant;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@Slf4j
@ConfigurationProperties(prefix = "email")
public class EmailConstant {
    private String userName;
    private String password;
    private String host;
    //验证码消息模板
    private String vCodeTemplate;
    private  Integer port;
    private  String overdueDate;
    private  String exceedConnections;
    private  Boolean startTlsEnabled;

    public void setUserName(String userName) {
        log.info("The email you set is【The email you set is】:{}. If empty, there is a configuration problem. 【If empty, there is a configuration problem.】",userName);
        this.userName = userName;
    }
}
