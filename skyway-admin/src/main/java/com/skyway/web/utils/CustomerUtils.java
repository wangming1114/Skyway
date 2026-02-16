package com.skyway.web.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.skyway.member.domain.MbCustomer;
import com.skyway.web.domain.customer.CustomerLoginUser;

/**
 * C 端当前登录客户工具（从 SecurityContext 取 CustomerLoginUser）
 *
 * @author ruoyi
 */
public final class CustomerUtils {

    private CustomerUtils() {
    }

    /**
     * 获取当前登录的客户信息（仅 /c-api 请求且已认证时非 null）
     */
    public static MbCustomer getLoginCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomerLoginUser) {
            return ((CustomerLoginUser) principal).getCustomer();
        }
        return null;
    }
}
