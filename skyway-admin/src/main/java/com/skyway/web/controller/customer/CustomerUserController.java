package com.skyway.web.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.skyway.common.constant.UserConstants;
import com.skyway.common.core.controller.BaseController;
import com.skyway.common.core.domain.AjaxResult;
import com.skyway.common.utils.SecurityUtils;
import com.skyway.common.utils.StringUtils;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.mapper.MbCustomerMapper;
import com.skyway.web.utils.CustomerUtils;
import com.skyway.web.domain.customer.CustomerChangePwdBody;
import com.skyway.web.domain.customer.CustomerProfileBody;

/**
 * C 端用户信息与个人中心
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/c-api/user")
public class CustomerUserController extends BaseController {

    @Autowired
    private MbCustomerMapper mbCustomerMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取当前登录客户信息（脱敏，不返回密码）
     */
    @GetMapping("/info")
    public AjaxResult info() {
        MbCustomer customer = CustomerUtils.getLoginCustomer();
        if (customer == null) {
            return AjaxResult.error(401, "未登录或登录已过期");
        }
        return success(customer);
    }

    /**
     * 更新个人资料（用户名、头像、手机、微信、QQ）
     */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody CustomerProfileBody body) {
        MbCustomer customer = CustomerUtils.getLoginCustomer();
        if (customer == null) {
            return AjaxResult.error(401, "未登录或登录已过期");
        }
        if (body == null) {
            return error("参数不能为空");
        }
        if (StringUtils.isNotEmpty(body.getUsername())) {
            if (body.getUsername().length() < UserConstants.USERNAME_MIN_LENGTH
                    || body.getUsername().length() > UserConstants.USERNAME_MAX_LENGTH) {
                return error("用户名长度必须在2到20个字符之间");
            }
            if (mbCustomerMapper.checkUsernameUnique(body.getUsername(), customer.getId()) != 0) {
                return error("用户名已存在");
            }
            customer.setUsername(body.getUsername());
        }
        if (body.getAvatar() != null) {
            customer.setAvatar(body.getAvatar());
        }
        if (body.getPhone() != null) {
            customer.setPhone(body.getPhone());
        }
        if (body.getWechat() != null) {
            customer.setWechat(body.getWechat());
        }
        if (body.getQq() != null) {
            customer.setQq(body.getQq());
        }
        customer.setUpdateBy("customer");
        mbCustomerMapper.update(customer);
        return success();
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePwd")
    public AjaxResult changePwd(@RequestBody CustomerChangePwdBody body) {
        MbCustomer customer = CustomerUtils.getLoginCustomer();
        if (customer == null) {
            return AjaxResult.error(401, "未登录或登录已过期");
        }
        if (body == null || StringUtils.isEmpty(body.getOldPassword()) || StringUtils.isEmpty(body.getNewPassword())) {
            return error("旧密码和新密码不能为空");
        }
        if (body.getNewPassword().length() < UserConstants.PASSWORD_MIN_LENGTH
                || body.getNewPassword().length() > UserConstants.PASSWORD_MAX_LENGTH) {
            return error("新密码长度必须在5到20个字符之间");
        }
        if (!passwordEncoder.matches(body.getOldPassword(), customer.getPassword())) {
            return error("旧密码错误");
        }
        String encoded = SecurityUtils.encryptPassword(body.getNewPassword());
        mbCustomerMapper.updatePassword(customer.getId(), encoded, "customer");
        return success();
    }
}
