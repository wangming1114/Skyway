package com.skyway.web.domain.member;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 创建客户订阅临时访问请求
 */
public class CustomerTempShareCreateBody {

    private String accessPassword;

    private Date expireTime;

    public String getAccessPassword() {
        return accessPassword;
    }

    public void setAccessPassword(String accessPassword) {
        this.accessPassword = accessPassword;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
}
