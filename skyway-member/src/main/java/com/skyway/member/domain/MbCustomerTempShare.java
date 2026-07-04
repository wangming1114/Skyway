package com.skyway.member.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyway.common.core.domain.BaseEntity;

/**
 * 客户订阅信息访问链接 mb_customer_temp_share
 */
public class MbCustomerTempShare extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long customerId;

    private String token;

    private String accessPassword;

    private Date expireTime;

    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
