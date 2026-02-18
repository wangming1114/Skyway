package com.skyway.framework.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import static com.google.code.kaptcha.Constants.*;

/**
 * 验证码配置
 * 
 * @author ruoyi
 */
@Configuration
public class CaptchaConfig
{
    @Bean(name = "captchaProducer")
    public DefaultKaptcha getKaptchaBean()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 无边框
        properties.setProperty(KAPTCHA_BORDER, "no");
        // 背景：浅灰渐变，更柔和
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_FROM, "248,250,252");
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_TO, "241,245,249");
        // 文字颜色：深灰，不刺眼
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "51,65,85");
        // 图片尺寸
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "38");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCode");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Verdana");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "2");
        // 水纹扭曲，比阴影更柔和
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.WaterRipple");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }

    @Bean(name = "captchaProducerMath")
    public DefaultKaptcha getKaptchaBeanMath()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 无边框
        properties.setProperty(KAPTCHA_BORDER, "no");
        // 背景：浅灰渐变
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_FROM, "248,250,252");
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_TO, "241,245,249");
        // 文字颜色：深蓝灰，偏商务
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "37,99,235");
        // 图片尺寸
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "36");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCodeMath");
        // 数学验证码文本生成器
        properties.setProperty(KAPTCHA_TEXTPRODUCER_IMPL, "com.skyway.framework.config.KaptchaTextCreator");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "4");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "6");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Verdana");
        // 轻微噪点，浅色不抢眼
        properties.setProperty(KAPTCHA_NOISE_COLOR, "203,213,224");
        properties.setProperty(KAPTCHA_NOISE_IMPL, "com.google.code.kaptcha.impl.DefaultNoise");
        // 水纹扭曲，更柔和
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.WaterRipple");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
