package com.skyway.web.service;

import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.skyway.common.constant.UserConstants;
import com.skyway.common.utils.SecurityUtils;
import com.skyway.common.utils.StringUtils;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.mapper.MbCustomerMapper;
import com.skyway.web.domain.customer.CustomerLoginBody;
import com.skyway.web.domain.customer.CustomerLoginResult;
import com.skyway.web.domain.customer.CustomerLoginUser;
import com.skyway.web.domain.customer.CustomerRegisterBody;
import com.skyway.web.domain.customer.ResetPasswordBody;

/**
 * C 端认证（注册、登录）
 *
 * @author ruoyi
 */
@Service
public class CustomerAuthService {

    @Autowired
    private MbCustomerMapper mbCustomerMapper;

    @Autowired
    private CustomerEmailCodeService emailCodeService;

    @Autowired
    private CustomerTokenService tokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 三级等保：密码 8-20 位，且包含大写、小写、数字、特殊字符 */
    private static String checkPasswordStrong(String password) {
        if (password == null || password.length() < 8 || password.length() > 20) {
            return "密码长度为8-20位";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码须包含大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码须包含小写字母";
        }
        if (!password.matches(".*[0-9].*")) {
            return "密码须包含数字";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "密码须包含特殊字符";
        }
        return null;
    }

    /**
     * 注册。校验用户名、邮箱、验证码，用户名与邮箱唯一，写入 mb_customer。
     *
     * @param body 注册信息
     * @return 错误信息，成功返回 null
     */
    public String register(CustomerRegisterBody body) {
        String username = body.getUsername();
        String email = body.getEmail();
        String code = body.getCode();
        String password = body.getPassword();
        if (StringUtils.isEmpty(username)) {
            return "用户名不能为空";
        }
        if (username.length() < 5 || username.length() > 16) {
            return "用户名长度为5-16位";
        }
        if (!username.matches("^[a-zA-Z]{5,16}$")) {
            return "用户名只能为英文字母，5-16位";
        }
        if (StringUtils.isEmpty(email)) {
            return "邮箱不能为空";
        }
        if (StringUtils.isEmpty(code)) {
            return "验证码不能为空";
        }
        if (StringUtils.isEmpty(password)) {
            return "密码不能为空";
        }
        String pwdErr = checkPasswordStrong(password);
        if (pwdErr != null) {
            return pwdErr;
        }
        if (!emailCodeService.verifyAndRemove(email, code)) {
            return "验证码错误或已过期";
        }
        if (mbCustomerMapper.checkUsernameUnique(username, null) != 0) {
            return "用户名已存在";
        }
        if (mbCustomerMapper.countByEmail(email) > 0) {
            return "该邮箱已注册";
        }
        MbCustomer customer = new MbCustomer();
        customer.setUsername(username);
        customer.setEmail(email);
        customer.setPassword(SecurityUtils.encryptPassword(password));
        customer.setStatus("0");
        customer.setRegisterTime(new Date());
        mbCustomerMapper.insert(customer);
        return null;
    }

    /**
     * 登录。支持邮箱或用户名 + 密码，更新最后登录信息，生成 token。
     *
     * @param body 登录信息（account 为邮箱或用户名）
     * @return 成功带 token，失败带具体错误信息（账号禁用 / 用户名或密码错误）
     */
    public CustomerLoginResult login(CustomerLoginBody body) {
        String account = body.getAccount();
        String password = body.getPassword();
        if (StringUtils.isEmpty(account) || StringUtils.isEmpty(password)) {
            return CustomerLoginResult.fail("用户名/邮箱或密码错误");
        }
        MbCustomer customer = account.contains("@")
                ? mbCustomerMapper.selectByEmail(account)
                : mbCustomerMapper.selectByUsername(account);
        if (customer == null) {
            return CustomerLoginResult.fail("用户名/邮箱或密码错误");
        }
        if (!"0".equals(customer.getStatus())) {
            return CustomerLoginResult.fail("账号已禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(password, customer.getPassword())) {
            return CustomerLoginResult.fail("用户名/邮箱或密码错误");
        }
        customer.setLastLoginAt(new Date());
        customer.setLastLoginIp(com.skyway.common.utils.ip.IpUtils.getIpAddr());
        mbCustomerMapper.update(customer);
        CustomerLoginUser loginUser = new CustomerLoginUser(customer);
        String token = tokenService.createToken(loginUser);
        return CustomerLoginResult.success(token);
    }

    /**
     * 找回密码：校验邮箱、验证码，更新密码。
     *
     * @param body 邮箱、验证码、新密码
     * @return 错误信息，成功返回 null
     */
    public String resetPassword(ResetPasswordBody body) {
        String email = body.getEmail();
        String code = body.getCode();
        String password = body.getPassword();
        if (StringUtils.isEmpty(email)) {
            return "邮箱不能为空";
        }
        if (StringUtils.isEmpty(code)) {
            return "验证码不能为空";
        }
        if (StringUtils.isEmpty(password)) {
            return "新密码不能为空";
        }
        String pwdErr = checkPasswordStrong(password);
        if (pwdErr != null) {
            return pwdErr;
        }
        if (!emailCodeService.verifyResetCodeAndRemove(email, code)) {
            return "验证码错误或已过期";
        }
        MbCustomer customer = mbCustomerMapper.selectByEmail(email);
        if (customer == null) {
            return "该邮箱未注册";
        }
        if (!"0".equals(customer.getStatus())) {
            return "账号已禁用，请联系管理员";
        }
        String encoded = SecurityUtils.encryptPassword(password);
        mbCustomerMapper.updatePassword(customer.getId(), encoded, "forgot");
        return null;
    }
}
