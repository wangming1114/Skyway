package com.skyway.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.skyway.web.security.CustomerJwtAuthenticationTokenFilter;

/**
 * C 端安全配置：/c-api/** 使用独立 FilterChain，匿名放行认证接口，其余需客户 token。
 *
 * @author ruoyi
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@org.springframework.context.annotation.Configuration
public class CustomerSecurityConfig {

    @Autowired
    private CustomerJwtAuthenticationTokenFilter customerJwtAuthenticationTokenFilter;

    @org.springframework.context.annotation.Bean
    public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .requestMatcher(request -> {
                    String path = request.getRequestURI();
                    return path != null && path.startsWith("/c-api/");
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .antMatchers("/c-api/auth/sendEmailCode", "/c-api/auth/register", "/c-api/auth/login",
                        "/c-api/auth/sendResetCode", "/c-api/auth/resetPassword").permitAll()
                        .antMatchers("/c-api/**").authenticated())
                .addFilterBefore(customerJwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
