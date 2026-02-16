package com.skyway.web.domain.customer;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * C 端修改密码请求体
 *
 * @author ruoyi
 */
public class CustomerChangePwdBody {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 5, max = 20, message = "新密码长度必须在5到20个字符之间")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
