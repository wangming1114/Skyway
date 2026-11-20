package com.skyway.web.service.ws;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Finds the control-plane signer, or installs the pinned official release into private state. */
final class WsLego {
    private static final String VERSION = "4.26.0";

    private WsLego() {}

    static synchronized String resolve(String configured, Path tools) throws IOException {
        boolean explicit = configured != null && !configured.trim().isEmpty();
        String name = explicit ? configured.trim() : "lego";
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        boolean windows = os.startsWith("windows");
        Path found = find(name, System.getenv("PATH"), windows);
        if (found != null) return found.toString();
        if (explicit)
            throw new IllegalArgumentException("管理端找不到指定的证书工具，请检查 skyway.ws.lego 的路径和执行权限");

        String platform = os;
        if (os.startsWith("mac")) platform = "darwin";
        if (windows) platform = "windows";
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        if ("x86_64".equals(arch) || "x64".equals(arch)) arch = "amd64";
        if ("aarch64".equals(arch)) arch = "arm64";
        if ("x86".equals(arch) || "i386".equals(arch)) arch = "386";
        String stem = "lego_v" + VERSION + "_" + platform + "_" + arch;
        String asset = stem + (windows ? ".zip" : ".tar.gz");
        Properties hashes = new Properties();
        try (InputStream stream = WsLego.class.getResourceAsStream("/ws/lego-" + VERSION + ".properties")) {
            if (stream == null) throw new IOException("管理端安装包缺少 lego 校验清单");
            hashes.load(stream);
        }
        String checksum = hashes.getProperty(asset);
        if (checksum == null)
            throw new IllegalArgumentException("请在此管理端安装 lego，并用 skyway.ws.lego 指定可执行文件");
        Path binary = tools.resolve(stem + (windows ? ".exe" : "")).toAbsolutePath();
        if (Files.isRegularFile(binary) && Files.isExecutable(binary)) return binary.toString();

        Files.createDirectories(binary.getParent());
        URL release = new URL("https://github.com/go-acme/lego/releases/download/v" + VERSION + "/" + asset);
        try {
            install(release, checksum, binary, windows);
        } catch (IOException ex) {
            throw new IOException(
                    "管理端未能安装 lego " + VERSION
                            + "，请检查 GitHub 连通性、tar 和私有目录权限，或通过 skyway.ws.lego 指定已安装的工具",
                    ex);
        }
        return binary.toString();
    }

    static Path find(String name, String searchPath, boolean windows) {
        Path file = Paths.get(name);
        if (file.isAbsolute() || file.getParent() != null)
            return Files.isRegularFile(file) && Files.isExecutable(file)
                    ? file.toAbsolutePath() : null;
        if (windows && !name.toLowerCase(Locale.ROOT).endsWith(".exe")) name += ".exe";
        if (searchPath != null) {
            for (String dir : searchPath.split(Pattern.quote(File.pathSeparator))) {
                if (dir.isEmpty()) continue;
                Path candidate = Paths.get(dir).resolve(name);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
                    return candidate.toAbsolutePath();
            }
        }
        return null;
    }

    static void install(URL release, String checksum, Path binary, boolean windows) throws IOException {
        Path work = Files.createTempDirectory(binary.getParent(), ".lego-");
        Path archive = work.resolve("download"), executable = work.resolve("binary");
        try {
            URLConnection connection = release.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(connection.getInputStream(), sha)) {
                Files.copy(input, archive);
            }
            if (!checksum.equals(String.format(Locale.ROOT, "%064x", new BigInteger(1, sha.digest()))))
                throw new IOException("lego 安装包的 SHA-256 校验失败");
            Process tar = new ProcessBuilder(
                    "tar", windows ? "-xOf" : "-xOzf", archive.toString(), windows ? "lego.exe" : "lego")
                    .redirectOutput(executable.toFile())
                    .start();
            try {
                if (!tar.waitFor(45, TimeUnit.SECONDS) || tar.exitValue() != 0)
                    throw new IOException("无法从已校验的发行包解出 lego");
            } finally {
                if (tar.isAlive()) tar.destroyForcibly();
            }
            if (Files.size(executable) == 0 || (!windows && !executable.toFile().setExecutable(true, true)))
                throw new IOException("无法设置 lego 的执行权限");
            // Multiple managers sharing state may publish the same verified binary concurrently.
            try {
                Files.move(executable, binary, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(executable, binary, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("管理端安装 lego 被中断", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        } finally {
            Files.deleteIfExists(executable);
            Files.deleteIfExists(archive);
            Files.deleteIfExists(work);
        }
    }
}
