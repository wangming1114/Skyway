package com.skyway.web.domain.customer;

/**
 * C 端登录请求体（邮箱或用户名 + 密码）
 *
 * @author ruoyi
 */
public class CustomerLoginBody {

    /** 邮箱或用户名 */
    private String account;

    /** 密码 */
    private String password;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
