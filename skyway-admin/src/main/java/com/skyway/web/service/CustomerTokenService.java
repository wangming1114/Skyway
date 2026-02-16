package com.skyway.web.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.skyway.common.constant.CacheConstants;
import com.skyway.common.constant.Constants;
import com.skyway.common.core.redis.RedisCache;
import com.skyway.common.utils.ServletUtils;
import com.skyway.common.utils.StringUtils;
import com.skyway.common.utils.http.UserAgentUtils;
import com.skyway.common.utils.ip.AddressUtils;
import com.skyway.common.utils.ip.IpUtils;
import com.skyway.common.utils.uuid.IdUtils;
import com.skyway.web.domain.customer.CustomerLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * C 端客户 token 验证处理（与管理端 TokenService 隔离，使用 login_customer_tokens）
 *
 * @author ruoyi
 */
@Component
public class CustomerTokenService {

    private static final Logger log = LoggerFactory.getLogger(CustomerTokenService.class);

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTime;

    private static final long MILLIS_MINUTE = 60 * 1000L;
    private static final long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    public CustomerLoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            return redisCache.getCacheObject(userKey);
        } catch (Exception e) {
            log.error("获取C端用户信息异常'{}'", e.getMessage());
        }
        return null;
    }

    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    public String createToken(CustomerLoginUser loginUser) {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return buildJwt(claims);
    }

    public void verifyToken(CustomerLoginUser loginUser) {
        long exp = loginUser.getExpireTime();
        long now = System.currentTimeMillis();
        if (exp - now <= MILLIS_MINUTE_TWENTY) {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(CustomerLoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    private void setUserAgent(CustomerLoginUser loginUser) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        String userAgent = request.getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getOperatingSystem(userAgent));
        loginUser.setOs(UserAgentUtils.getBrowser(userAgent));
    }

    private String buildJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid) {
        return CacheConstants.LOGIN_CUSTOMER_TOKEN_KEY + uuid;
    }
}
