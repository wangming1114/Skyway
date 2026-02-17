package com.skyway.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.constant.Constants;
import com.skyway.common.utils.StringUtils;
import com.skyway.web.domain.customer.CustomerLoginBody;
import com.skyway.web.domain.customer.CustomerLoginResult;
import com.skyway.web.domain.customer.CustomerRegisterBody;
import com.skyway.web.domain.customer.ResetPasswordBody;
import com.skyway.web.domain.customer.SendEmailCodeBody;
import com.skyway.web.service.CustomerAuthService;
import com.skyway.web.service.CustomerEmailCodeService;

/**
 * C 端认证（发送验证码、注册、登录）
 * 同时映射 /c-api/auth 与 /auth：nginx 将 /c-api/ 代理为 / 时后端收到 /auth/*
 *
 * @author ruoyi
 */
@RestController
@RequestMapping(value = { "/c-api/auth", "/auth" })
public class CustomerAuthController extends BaseController {

    @Autowired
    private CustomerEmailCodeService emailCodeService;

    @Autowired
    private CustomerAuthService authService;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendEmailCode")
    public AjaxResult sendEmailCode(@RequestBody SendEmailCodeBody body) {
        if (body == null || StringUtils.isEmpty(body.getEmail())) {
            return error("邮箱不能为空");
        }
        String msg = emailCodeService.sendCode(body.getEmail());
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    /**
     * C 端注册（邮箱 + 验证码 + 密码）
     */
    @PostMapping("/register")
    public AjaxResult register(@RequestBody CustomerRegisterBody body) {
        if (body == null) {
            return error("参数不能为空");
        }
        String msg = authService.register(body);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    /**
     * C 端登录（邮箱或用户名 + 密码）
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody CustomerLoginBody body) {
        if (body == null) {
            return error("参数不能为空");
        }
        CustomerLoginResult result = authService.login(body);
        if (!result.isSuccess()) {
            return error(result.getErrorMsg());
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, result.getToken());
        return ajax;
    }

    /**
     * 发送找回密码验证码
     */
    @PostMapping("/sendResetCode")
    public AjaxResult sendResetCode(@RequestBody SendEmailCodeBody body) {
        if (body == null || StringUtils.isEmpty(body.getEmail())) {
            return error("邮箱不能为空");
        }
        String msg = emailCodeService.sendResetCode(body.getEmail());
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    /**
     * 找回密码（邮箱 + 验证码 + 新密码）
     */
    @PostMapping("/resetPassword")
    public AjaxResult resetPassword(@RequestBody ResetPasswordBody body) {
        if (body == null) {
            return error("参数不能为空");
        }
        String msg = authService.resetPassword(body);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
