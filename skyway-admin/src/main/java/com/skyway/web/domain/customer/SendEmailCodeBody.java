package com.skyway.web.domain.customer;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * C 端发送邮箱验证码请求体
 *
 * @author ruoyi
 */
public class SendEmailCodeBody {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
