package com.skyway.web.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import com.skyway.common.constant.CacheConstants;
import com.skyway.common.core.redis.RedisCache;
import com.skyway.common.utils.StringUtils;

/**
 * C 端邮箱验证码（发送、存储、限流）
 *
 * @author ruoyi
 */
@Service
public class CustomerEmailCodeService {

    @Autowired
    private RedisCache redisCache;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${skyway.mail.from:}")
    private String from;

    @Value("${skyway.mail.emailCodeExpireMinutes:5}")
    private int emailCodeExpireMinutes;

    @Value("${skyway.mail.emailCodeLimitSeconds:60}")
    private int emailCodeLimitSeconds;

    private static final String EMAIL_CODE_SUBJECT = "验证码";
    private static final String EMAIL_CODE_BODY_PREFIX = "您的验证码是：";
    private static final String RESET_CODE_SUBJECT = "找回密码验证码";
    private static final String RESET_CODE_BODY_PREFIX = "您正在找回密码，验证码是：";

    /**
     * 发送验证码到邮箱。同一邮箱在限定秒数内只能发送一次。
     *
     * @param email 邮箱
     * @return 错误信息，成功返回 null
     */
    public String sendCode(String email) {
        if (mailSender == null) {
            return "邮件服务未配置，无法发送验证码";
        }
        if (StringUtils.isEmpty(from)) {
            return "发件人未配置";
        }
        String limitKey = CacheConstants.EMAIL_CODE_LIMIT_KEY + email;
        if (Boolean.TRUE.equals(redisCache.hasKey(limitKey))) {
            return "发送过于频繁，请稍后再试";
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String codeKey = CacheConstants.EMAIL_CODE_KEY + email;
        redisCache.setCacheObject(codeKey, code, emailCodeExpireMinutes, TimeUnit.MINUTES);
        redisCache.setCacheObject(limitKey, "1", emailCodeLimitSeconds, TimeUnit.SECONDS);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(EMAIL_CODE_SUBJECT);
        message.setText(EMAIL_CODE_BODY_PREFIX + code + "，有效期" + emailCodeExpireMinutes + "分钟，请勿泄露。");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            redisCache.deleteObject(codeKey);
            redisCache.deleteObject(limitKey);
            return "发送失败：" + e.getMessage();
        }
        return null;
    }

    /**
     * 校验验证码（不区分大小写），校验通过后删除 Redis 中的验证码。
     *
     * @param email 邮箱
     * @param code  用户输入的验证码
     * @return true 校验通过，false 失败或过期
     */
    public boolean verifyAndRemove(String email, String code) {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code)) {
            return false;
        }
        String codeKey = CacheConstants.EMAIL_CODE_KEY + email;
        String cached = redisCache.getCacheObject(codeKey);
        if (cached == null) {
            return false;
        }
        if (!cached.equalsIgnoreCase(code)) {
            return false;
        }
        redisCache.deleteObject(codeKey);
        return true;
    }

    /**
     * 发送找回密码验证码（与注册验证码独立限流、独立存储）
     *
     * @param email 邮箱
     * @return 错误信息，成功返回 null
     */
    public String sendResetCode(String email) {
        if (mailSender == null) {
            return "邮件服务未配置，无法发送验证码";
        }
        if (StringUtils.isEmpty(from)) {
            return "发件人未配置";
        }
        String limitKey = CacheConstants.EMAIL_RESET_CODE_LIMIT_KEY + email;
        if (Boolean.TRUE.equals(redisCache.hasKey(limitKey))) {
            return "发送过于频繁，请稍后再试";
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String codeKey = CacheConstants.EMAIL_RESET_CODE_KEY + email;
        redisCache.setCacheObject(codeKey, code, emailCodeExpireMinutes, TimeUnit.MINUTES);
        redisCache.setCacheObject(limitKey, "1", emailCodeLimitSeconds, TimeUnit.SECONDS);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(RESET_CODE_SUBJECT);
        message.setText(RESET_CODE_BODY_PREFIX + code + "，有效期" + emailCodeExpireMinutes + "分钟，请勿泄露。");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            redisCache.deleteObject(codeKey);
            redisCache.deleteObject(limitKey);
            return "发送失败：" + e.getMessage();
        }
        return null;
    }

    /**
     * 校验找回密码验证码并删除
     *
     * @param email 邮箱
     * @param code  验证码
     * @return true 校验通过
     */
    public boolean verifyResetCodeAndRemove(String email, String code) {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(code)) {
            return false;
        }
        String codeKey = CacheConstants.EMAIL_RESET_CODE_KEY + email;
        String cached = redisCache.getCacheObject(codeKey);
        if (cached == null) {
            return false;
        }
        if (!cached.equalsIgnoreCase(code)) {
            return false;
        }
        redisCache.deleteObject(codeKey);
        return true;
    }
}
