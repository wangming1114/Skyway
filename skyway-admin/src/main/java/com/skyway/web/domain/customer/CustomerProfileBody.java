package com.skyway.web.domain.customer;

import javax.validation.constraints.Size;

/**
 * C 端个人资料更新请求体（不含邮箱、密码）
 *
 * @author ruoyi
 */
public class CustomerProfileBody {

    @Size(min = 2, max = 20, message = "用户名长度必须在2到20个字符之间")
    private String username;

    private String avatar;

    private String phone;

    private String wechat;

    private String qq;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }
}
