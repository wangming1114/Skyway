package com.skyway.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 本地调试场景下启动内嵌 Redis，避免依赖外部 Redis 服务。
 */
@Configuration
@ConditionalOnProperty(prefix = "skyway.redis.embedded", name = "enabled", havingValue = "true")
public class EmbeddedRedisConfig
{
    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedisConfig.class);

    @Value("${skyway.redis.embedded.host:127.0.0.1}")
    private String host;

    @Value("${skyway.redis.embedded.port:16379}")
    private int port;

    private Object redisServer;

    /**
     * 应用启动后初始化内嵌 Redis。
     */
    @PostConstruct
    public void start()
    {
        log.info("准备启动内嵌Redis，地址: {}:{}", host, port);
        assertPortAvailable(host, port);
        try
        {
            Class<?> redisServerClass = Class.forName("redis.embedded.RedisServer");
            this.redisServer = redisServerClass.getConstructor(int.class).newInstance(port);
            redisServerClass.getMethod("start").invoke(this.redisServer);
            log.info("内嵌Redis启动成功，地址: {}:{}", host, port);
        }
        catch (ClassNotFoundException ex)
        {
            throw new IllegalStateException(
                    "Embedded redis dependency not found. Please check com.github.codemonstur:embedded-redis.", ex);
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex)
        {
            throw new IllegalStateException(String.format(
                    "Failed to start embedded redis on %s:%d. Check local port usage or set skyway.redis.embedded.enabled=false.",
                    host, port), ex);
        }
    }

    /**
     * 应用关闭前停止内嵌 Redis。
     */
    @PreDestroy
    public void stop()
    {
        if (this.redisServer == null)
        {
            return;
        }
        log.info("准备停止内嵌Redis，地址: {}:{}", host, port);
        try
        {
            this.redisServer.getClass().getMethod("stop").invoke(this.redisServer);
            log.info("内嵌Redis已停止，地址: {}:{}", host, port);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
        {
            log.warn("Failed to stop embedded redis cleanly.", ex);
        }
    }

    /**
     * 启动前检测端口占用，防止连到错误的 Redis 实例。
     */
    private void assertPortAvailable(String host, int port)
    {
        try (ServerSocket socket = new ServerSocket())
        {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(host, port));
        }
        catch (IOException ex)
        {
            throw new IllegalStateException(String.format(
                    "Embedded redis port %s:%d is already in use. Stop the existing process or change skyway.redis.embedded.port.",
                    host, port), ex);
        }
    }
}
