package com.skyway.web.service.ws;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** The ACME account and DNS credentials stay on the control plane. Only a CSR leaves the VPS. */
@Service
public class WsCertificates {
    private final WsRemote remote;
    private final CloudflareDns dns;
    private final Path state;
    private final String lego;
    private final String directory;

    @Value("${skyway.ws.acme-propagation-wait-seconds:30}")
    private int propagationWaitSeconds;

    public WsCertificates(
            WsRemote remote,
            CloudflareDns dns,
            @Value("${skyway.ws.state-dir:${user.home}/.skyway/private}") String state,
            @Value("${skyway.ws.lego:}") String lego,
            @Value("${skyway.ws.acme-directory:https://acme-v02.api.letsencrypt.org/directory}")
                    String directory) {
        this.remote = remote;
        this.dns = dns;
        this.state = Paths.get(state, "acme").toAbsolutePath();
        this.lego = lego;
        this.directory = directory;
    }

    String signer() throws IOException {
        return WsLego.resolve(lego, state.getParent().resolve("tools"));
    }

    static Instant renewal(String pem, String host) throws Exception {
        X509Certificate cert = leaf(pem);
        boolean matches =
                cert.getSubjectAlternativeNames().stream()
                        .anyMatch(
                                san ->
                                        Integer.valueOf(2).equals(san.get(0))
                                                && host.equalsIgnoreCase(
                                                        String.valueOf(san.get(1))));
        if (!matches) throw new IOException("签发证书与节点域名不匹配");
        Instant from = cert.getNotBefore().toInstant(), until = cert.getNotAfter().toInstant();
        return until.minusSeconds(
                Math.min(30 * 86400L, Duration.between(from, until).getSeconds() / 3));
    }

    private static X509Certificate leaf(String pem) throws Exception {
        return (X509Certificate)
                CertificateFactory.getInstance("X.509")
                        .generateCertificate(
                                new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    Instant ensure(SSHClient ssh, WsStore.Domain domain) throws Exception {
        return ensure(ssh, domain, Instant.now());
    }

    synchronized Instant ensure(SSHClient ssh, WsStore.Domain domain, Instant now)
            throws Exception {
        String pem = remote.run(ssh, "certificate", domain.id).trim();
        if (!pem.isEmpty()) {
            Instant renew = renewal(pem, domain.host);
            if (renew.isAfter(now)) return renew;
        }
        Files.createDirectories(state);
        Path issued = state.resolve("certificates").resolve(domain.host + ".crt");
        Path csr = state.resolve(domain.id + ".csr");
        Path log = Files.createTempFile(state, "order-", ".log");
        try (FileChannel accountLock =
                        FileChannel.open(
                                state.resolve(".order.lock"),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                FileLock ignored = accountLock.lock()) {
            Files.write(
                    csr,
                    remote.run(ssh, "csr", domain.id, domain.host)
                            .getBytes(StandardCharsets.US_ASCII));
            if (Files.isRegularFile(issued)) {
                String chain = new String(Files.readAllBytes(issued), StandardCharsets.US_ASCII);
                Instant renew = renewal(chain, domain.host);
                if (renew.isAfter(now)) {
                    String publicKey = remote.run(ssh, "public-key", domain.id);
                    if (Arrays.equals(
                            leaf(chain).getPublicKey().getEncoded(),
                            Base64.getMimeDecoder().decode(publicKey))) {
                        remote.run(ssh, "put-cert", domain.id, WsRemote.encode(chain));
                        return renew;
                    }
                }
            }
            List<String> command =
                    new ArrayList<>(
                            Arrays.asList(
                                    signer(),
                                    "--path",
                                    state.toString(),
                                    "--server",
                                    directory,
                                    "--accept-tos",
                                    "--email",
                                    "hostmaster@" + dns.root(),
                                    "--dns",
                                    "cloudflare"));
            if (propagationWaitSeconds < 0 || propagationWaitSeconds > 300)
                throw new IllegalArgumentException("ACME DNS 传播等待时间应为 0 至 300 秒");
            if (propagationWaitSeconds > 0) {
                // The CA still checks the public TXT. This mode supports control planes that
                // cannot send DNS queries directly to the zone's authoritative nameservers.
                command.add("--dns.propagation-wait");
                command.add(propagationWaitSeconds + "s");
            }
            Collections.addAll(command, "--csr", csr.toString(), "run");
            ProcessBuilder builder = new ProcessBuilder(command);
            Map<String, String> env = builder.environment();
            for (String key :
                    Arrays.asList(
                            "CLOUDFLARE_EMAIL",
                            "CF_API_EMAIL",
                            "CLOUDFLARE_API_KEY",
                            "CF_API_KEY")) {
                env.remove(key);
                env.remove(key + "_FILE");
            }
            String token = dns.token();
            env.put("CLOUDFLARE_DNS_API_TOKEN", token);
            env.put("CLOUDFLARE_ZONE_API_TOKEN", token);
            builder.redirectErrorStream(true).redirectOutput(log.toFile());
            Process process = builder.start();
            try {
                if (!process.waitFor(8, TimeUnit.MINUTES))
                    throw new IOException("ACME DNS-01 签发超时");
                if (process.exitValue() != 0) throw orderFailure(log);
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
            String chain = new String(Files.readAllBytes(issued), StandardCharsets.US_ASCII);
            Instant renew = renewal(chain, domain.host);
            remote.run(ssh, "put-cert", domain.id, WsRemote.encode(chain));
            return renew;
        } finally {
            Files.deleteIfExists(log);
            Files.deleteIfExists(csr);
        }
    }

    private static IOException orderFailure(Path log) throws IOException {
        // Only publish a classified failure. Provider output can contain account identifiers.
        String detail =
                new String(Files.readAllBytes(log), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
        if (detail.contains("ratelimited") || detail.contains("rate limit"))
            return new IOException("证书颁发机构已达到签发限额");
        if (detail.contains("propagation")
                && (detail.contains("time limit exceeded")
                        || detail.contains("i/o timeout")
                        || detail.contains("not propagated")))
            return new IOException("管理端等待 DNS-01 TXT 传播超时，请检查权威 DNS 连通性或配置传播等待时间");
        if (detail.contains("invalid access token")
                || detail.contains("authentication error")
                || detail.contains("requires permission"))
            return new IOException("Cloudflare 拒绝 DNS-01 操作，请检查区域令牌权限");
        if (detail.contains("unauthorized") || detail.contains("incorrect txt"))
            return new IOException("证书颁发机构未能验证域名的 DNS-01 TXT 记录");
        return new IOException("ACME DNS-01 签发失败，请检查 DNS 服务和证书颁发机构状态");
    }
}
