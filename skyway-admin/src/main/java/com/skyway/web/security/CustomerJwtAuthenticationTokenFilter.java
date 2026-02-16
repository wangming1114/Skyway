package com.skyway.web.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.skyway.common.utils.StringUtils;
import com.skyway.web.domain.customer.CustomerLoginUser;
import com.skyway.web.service.CustomerTokenService;

/**
 * C 端 token 过滤器：仅对 /c-api 请求解析客户 token，设置 SecurityContext。
 *
 * @author ruoyi
 */
@Component
public class CustomerJwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private CustomerTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        CustomerLoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser) && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    loginUser, null, loginUser.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
