package com.skyway.web.domain.customer;

import java.util.Collection;
import java.util.Collections;
import com.alibaba.fastjson2.annotation.JSONField;
import com.skyway.member.domain.MbCustomer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * C 端客户登录身份（mb_customer）
 *
 * @author ruoyi
 */
public class CustomerLoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /** 客户信息 */
    private MbCustomer customer;

    /** 用户唯一标识（token uuid） */
    private String token;

    /** 登录时间 */
    private Long loginTime;

    /** 过期时间 */
    private Long expireTime;

    /** 登录IP */
    private String ipaddr;

    /** 登录地点 */
    private String loginLocation;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    public CustomerLoginUser() {
    }

    public CustomerLoginUser(MbCustomer customer) {
        this.customer = customer;
    }

    @JSONField(serialize = false)
    @Override
    public String getPassword() {
        return customer != null ? customer.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return customer != null ? customer.getEmail() : null;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isEnabled() {
        return customer != null && "0".equals(customer.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public MbCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(MbCustomer customer) {
        this.customer = customer;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getLoginLocation() {
        return loginLocation;
    }

    public void setLoginLocation(String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
