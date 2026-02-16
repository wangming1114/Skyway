package com.skyway.web.domain.customer;

/**
 * C 端登录结果（成功返回 token，失败返回错误信息）
 *
 * @author ruoyi
 */
public class CustomerLoginResult {

    private final String token;
    private final String errorMsg;

    private CustomerLoginResult(String token, String errorMsg) {
        this.token = token;
        this.errorMsg = errorMsg;
    }

    public static CustomerLoginResult success(String token) {
        return new CustomerLoginResult(token, null);
    }

    public static CustomerLoginResult fail(String errorMsg) {
        return new CustomerLoginResult(null, errorMsg);
    }

    public boolean isSuccess() {
        return token != null;
    }

    public String getToken() {
        return token;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
