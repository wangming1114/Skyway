package com.skyway.web.domain.customer;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * C 端注册请求体（用户名 + 邮箱 + 验证码 + 密码）
 *
 * @author ruoyi
 */
public class CustomerRegisterBody {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 5, max = 16, message = "用户名长度为5-16位")
    @Pattern(regexp = "^[a-zA-Z]{5,16}$", message = "用户名只能为英文字母，5-16位")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8到20个字符之间")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
