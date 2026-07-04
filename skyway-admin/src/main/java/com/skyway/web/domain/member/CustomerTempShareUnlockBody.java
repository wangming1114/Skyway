package com.skyway.web.domain.member;

/**
 * 解锁客户订阅信息访问请求
 */
public class CustomerTempShareUnlockBody {

    private String accessPassword;

    public String getAccessPassword() {
        return accessPassword;
    }

    public void setAccessPassword(String accessPassword) {
        this.accessPassword = accessPassword;
    }
}
