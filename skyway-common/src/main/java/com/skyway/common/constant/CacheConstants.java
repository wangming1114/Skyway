package com.skyway.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 邮箱验证码 redis key（C 端注册）
     */
    public static final String EMAIL_CODE_KEY = "email_code:";

    /**
     * 邮箱验证码发送限流 redis key（同一邮箱 60 秒内仅允许发送一次）
     */
    public static final String EMAIL_CODE_LIMIT_KEY = "email_code_limit:";

    /**
     * 找回密码验证码 redis key
     */
    public static final String EMAIL_RESET_CODE_KEY = "email_reset_code:";

    /**
     * 找回密码验证码发送限流 redis key
     */
    public static final String EMAIL_RESET_CODE_LIMIT_KEY = "email_reset_code_limit:";

    /**
     * C 端客户登录 token redis key
     */
    public static final String LOGIN_CUSTOMER_TOKEN_KEY = "login_customer_tokens:";
}
