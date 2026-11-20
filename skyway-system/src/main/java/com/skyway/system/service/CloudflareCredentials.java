package com.skyway.system.service;

import com.skyway.system.domain.SysConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudflareCredentials {
    public static final String TOKEN = "skyway.cloudflare.apiToken";
    public static final String MASK = "••••••••";
    private static final String PREFIX = "cf-gcm:";
    private final SecureRandom random = new SecureRandom();
    private final Path directory;

    public CloudflareCredentials(
            @Value("${skyway.ws.state-dir:${user.home}/.skyway/private}") String directory) {
        this.directory = Paths.get(directory).toAbsolutePath();
    }

    public static boolean isToken(String key) {
        return TOKEN.equalsIgnoreCase(key == null ? "" : key.trim());
    }

    public static String display(String key, String value) {
        return (isToken(key) || (value != null && value.startsWith(PREFIX)))
                        && value != null
                        && !value.isEmpty()
                ? MASK
                : value;
    }

    public static SysConfig mask(SysConfig config) {
        if (config != null)
            config.setConfigValue(display(config.getConfigKey(), config.getConfigValue()));
        return config;
    }

    public synchronized String encrypt(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_.-]{12,250}"))
            throw new IllegalArgumentException("Cloudflare API Token 格式无效");
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + Base64.getEncoder()
                            .encodeToString(
                                    ByteBuffer.allocate(iv.length + encrypted.length)
                                            .put(iv)
                                            .put(encrypted)
                                            .array());
        } catch (Exception ex) {
            throw new IllegalStateException("无法加密 Cloudflare 凭据，请检查私有数据目录", ex);
        }
    }

    public synchronized String decrypt(String value) {
        if (value == null || !value.startsWith(PREFIX))
            throw new IllegalStateException("请在参数设置中录入 Cloudflare API Token");
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, bytes, 0, 12));
            return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("无法解密 Cloudflare 凭据，请恢复原部署的私有密钥", ex);
        }
    }

    private SecretKeySpec key() throws IOException {
        Files.createDirectories(directory);
        Path keyFile = directory.resolve("cloudflare.key");
        try (FileChannel lockChannel =
                        FileChannel.open(
                                directory.resolve("key.lock"),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                FileLock ignored = lockChannel.lock()) {
            if (!Files.exists(keyFile)) {
                Path temp = Files.createTempFile(directory, ".key-", ".tmp");
                try {
                    if (Files.getFileStore(directory).supportsFileAttributeView("posix"))
                        Files.setPosixFilePermissions(
                                temp, PosixFilePermissions.fromString("rw-------"));
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    Files.write(temp, bytes);
                    Files.move(temp, keyFile, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
            byte[] bytes = Files.readAllBytes(keyFile);
            if (bytes.length != 32) throw new IOException("密钥长度无效");
            return new SecretKeySpec(bytes, "AES");
        }
    }
}
