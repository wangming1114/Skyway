package com.skyway.web.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IVpsInstanceService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

/**
 * VPS 实例 SSH 单次命令执行（如节点配置重命名），用于非 WebSocket 场景。
 */
@Service
public class VpsSshCommandService {

    private static final Logger log = LoggerFactory.getLogger(VpsSshCommandService.class);
    private static final String CONF_DIR = "/etc/sing-box/conf";
    private static final String DISABLED_SUFFIX = ".disabled";

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    /**
     * 在服务器上重命名节点配置文件：停用加 .disabled 后缀，启用则去掉。
     *
     * @param instanceId 实例 ID
     * @param nodeName   节点名称（不含 .json）
     * @param disable    true=停用（.json -> .json.disabled），false=启用（.json.disabled -> .json）
     * @throws IOException 连接或 mv 失败时抛出
     */
    public void renameProxyNodeConfig(Long instanceId, String nodeName, boolean disable) throws IOException {
        if (instanceId == null || StringUtils.isEmpty(nodeName)) {
            throw new IllegalArgumentException("instanceId 与 nodeName 不能为空");
        }
        String baseName = nodeName.trim();
        if (baseName.endsWith(".json")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }
        String fromName = baseName + ".json" + (disable ? "" : DISABLED_SUFFIX);
        String toName = baseName + ".json" + (disable ? DISABLED_SUFFIX : "");
        String fromPath = CONF_DIR + "/" + fromName;
        String toPath = CONF_DIR + "/" + toName;

        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            try (Session session = ssh.startSession()) {
                String cmd = "mv " + fromPath + " " + toPath + " 2>&1";
                Command command = session.exec(cmd);
                consumeStream(command.getInputStream());
                consumeStream(command.getErrorStream());
                command.join(15, java.util.concurrent.TimeUnit.SECONDS);
                Integer exit = command.getExitStatus();
                if (exit != null && exit != 0) {
                    throw new IOException("mv 执行失败，退出码: " + exit);
                }
            }
            // 重命名后重启 sing-box 使配置生效（sb 菜单 5 运行管理 -> 3 重启）
            restartSingBox(ssh);
        } finally {
            if (ssh != null) {
                try {
                    ssh.close();
                } catch (IOException e) {
                    log.debug("SSH close: {}", e.getMessage());
                }
            }
        }
    }

    private SSHClient createSshClient(Long instanceId) throws IOException {
        VpsInstance inst = vpsInstanceService.getById(instanceId);
        if (inst == null || StringUtils.isEmpty(inst.getIp()) || inst.getSshPort() == null
                || StringUtils.isEmpty(inst.getSshUsername())) {
            throw new IllegalStateException("实例不存在或 SSH 信息不完整");
        }
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect(inst.getIp(), inst.getSshPort() != null ? inst.getSshPort() : 22);
        ssh.authPassword(inst.getSshUsername(), inst.getSshPassword() != null ? inst.getSshPassword() : "");
        return ssh;
    }

    /** 在已建立的 SSH 连接上执行 sb 运行管理 -> 重启，使配置生效。失败仅打日志不抛异常。 */
    private void restartSingBox(SSHClient ssh) throws IOException {
        try (Session session = ssh.startSession()) {
            String runCmd = "printf '5\\n3\\n' | sb 2>&1";
            Command command = session.exec(runCmd);
            consumeStream(command.getInputStream());
            consumeStream(command.getErrorStream());
            command.join(30, java.util.concurrent.TimeUnit.SECONDS);
            Integer exit = command.getExitStatus();
            if (exit != null && exit != 0) {
                log.warn("sing-box 重启命令退出码: {}", exit);
            }
        }
    }

    private static void consumeStream(InputStream in) throws IOException {
        if (in == null) return;
        byte[] buf = new byte[4096];
        while (in.read(buf) > 0) {
            // consume
        }
    }

    private static String execAndRead(Session session, String command) throws IOException {
        try (Command cmd = session.exec(command)) {
            InputStream in = cmd.getInputStream();
            byte[] buf = new byte[4096];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            cmd.join(30, TimeUnit.SECONDS);
            return sb.toString();
        }
    }

    private static final Pattern P_PROTOCOL = Pattern.compile("协议\\s*\\(protocol\\)\\s*=\\s*(.+)");
    private static final Pattern P_ADDRESS = Pattern.compile("地址\\s*\\(address\\)\\s*=\\s*(.+)");
    private static final Pattern P_PORT = Pattern.compile("端口\\s*\\(port\\)\\s*=\\s*(.+)");
    private static final Pattern P_ID = Pattern.compile("用户ID\\s*\\(id\\)\\s*=\\s*(.+)");
    private static final Pattern P_FLOW = Pattern.compile("流控\\s*\\(flow\\)\\s*=\\s*(.+)");
    private static final Pattern P_NETWORK = Pattern.compile("传输协议\\s*\\(network\\)\\s*=\\s*(.+)");
    private static final Pattern P_SECURITY = Pattern.compile("传输层安全\\s*\\(TLS\\)\\s*=\\s*(.+)");
    private static final Pattern P_SNI = Pattern.compile("SNI\\s*\\(serverName\\)\\s*=\\s*(.+)");
    private static final Pattern P_FINGERPRINT = Pattern.compile("指纹\\s*\\(Fingerprint\\)\\s*=\\s*(.+)");
    private static final Pattern P_PUBLIC_KEY = Pattern.compile("公钥\\s*\\(Public key\\)\\s*=\\s*(.+)");
    private static final Pattern P_URL = Pattern.compile("(vless://[^\\s]+)");
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[0-9;]*m|\\[[0-9;]+m");

    private static String stripAnsi(String s) {
        if (s == null || s.isEmpty()) return s;
        return ANSI_ESCAPE.matcher(s).replaceAll("");
    }

    private static String match1(Pattern p, String text) {
        if (text == null) return null;
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private static Date parseExpireTime(String expireTimeStr) {
        if (expireTimeStr == null || expireTimeStr.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expireTimeStr.trim());
        } catch (Exception e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(expireTimeStr.trim());
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private ProxyNode parseSbVlessRealityOutput(String output, int defaultPort) {
        if (output == null) return null;
        String protocol = stripAnsi(match1(P_PROTOCOL, output));
        String address = stripAnsi(match1(P_ADDRESS, output));
        String portStr = stripAnsi(match1(P_PORT, output));
        String id = stripAnsi(match1(P_ID, output));
        String flow = stripAnsi(match1(P_FLOW, output));
        String network = stripAnsi(match1(P_NETWORK, output));
        String security = stripAnsi(match1(P_SECURITY, output));
        String sni = stripAnsi(match1(P_SNI, output));
        String fingerprint = stripAnsi(match1(P_FINGERPRINT, output));
        String publicKey = stripAnsi(match1(P_PUBLIC_KEY, output));
        String url = match1(P_URL, output);
        if (url != null) url = stripAnsi(url);
        if (address == null || address.isEmpty() || id == null || id.isEmpty() || url == null || url.isEmpty()) {
            return null;
        }
        int port = defaultPort;
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        ProxyNode node = new ProxyNode();
        node.setNodeType("VLESS-REALITY");
        node.setAddress(address != null ? address.trim() : "");
        node.setPort(port);
        node.setUrl(url != null ? url.trim() : "");
        node.setNodeName("VLESS-REALITY-" + port);
        JSONObject config = new JSONObject();
        config.put("protocol", protocol != null ? protocol.trim() : "vless");
        config.put("id", id != null ? id.trim() : "");
        config.put("flow", flow != null ? flow.trim() : "");
        config.put("network", network != null ? network.trim() : "tcp");
        config.put("security", security != null ? security.trim() : "reality");
        config.put("sni", sni != null ? sni.trim() : "");
        config.put("fingerprint", fingerprint != null ? fingerprint.trim() : "");
        config.put("publicKey", publicKey != null ? publicKey.trim() : "");
        node.setConfigJson(config.toJSONString());
        return node;
    }

    /**
     * 在指定实例上添加 VLESS-REALITY 节点（执行 sb、重命名、解析），返回可入库的节点对象（未设置 createBy，由调用方 insert）。
     */
    public ProxyNode addProxyNodeOnInstance(Long instanceId, Long customerId, int port, String expireTimeStr) throws IOException {
        if (customerId == null) throw new IllegalArgumentException("请选择归属客户");
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            try (Session checkSession = ssh.startSession()) {
                String sbCheck = execAndRead(checkSession, "command -v sb 2>/dev/null || which sb 2>/dev/null || echo ''");
                if (sbCheck == null || sbCheck.trim().isEmpty()) {
                    throw new IllegalStateException("未检测到 sing-box 脚本 (sb)，请先在服务器上安装 sing-box");
                }
            }
            String runCmd = "printf '1\\n18\\n" + port + "\\n' | sb";
            String output;
            try (Session runSession = ssh.startSession()) {
                String toRun = "bash -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                output = execAndRead(runSession, toRun);
            }
            if (output == null) output = "";
            output = stripAnsi(output);
            ProxyNode parsed = parseSbVlessRealityOutput(output, port);
            if (parsed == null || parsed.getUrl() == null || parsed.getUrl().isEmpty()) {
                throw new IllegalStateException("解析 sb 输出失败，请检查服务器上 sing-box 是否正常");
            }
            Date expireDate = parseExpireTime(expireTimeStr);
            String expiryTag = (expireDate == null) ? "permanent" : new SimpleDateFormat("yyyyMMdd").format(expireDate);
            String customPart = String.valueOf(customerId);
            String targetBaseName = "VLESS-REALITY-" + port + "-" + customPart + "-" + expiryTag;
            String oldJsonName = "VLESS-REALITY-" + port + ".json";
            String newJsonName = targetBaseName + ".json";
            String mvCmd = "mv " + CONF_DIR + "/" + oldJsonName + " " + CONF_DIR + "/" + newJsonName + " 2>&1";
            try (Session mvSession = ssh.startSession()) {
                String mvOut = execAndRead(mvSession, mvCmd);
                if (mvOut != null && mvOut.toLowerCase().contains("no such file")) {
                    throw new IOException("重命名失败: 未找到 " + oldJsonName);
                }
            }
            parsed.setInstanceId(instanceId);
            parsed.setCustomerId(customerId);
            parsed.setNodeName(targetBaseName);
            parsed.setCustomId(customPart);
            parsed.setExpireTime(expireDate);
            parsed.setStatus("0");
            return parsed;
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    /**
     * 在服务器上删除节点配置文件（.json 或 .json.disabled），不删库。
     */
    public void removeProxyNodeFromServer(ProxyNode node) throws IOException {
        if (node == null || node.getInstanceId() == null) throw new IllegalArgumentException("节点或实例ID为空");
        String nodeName = (node.getNodeName() != null ? node.getNodeName() : "").trim();
        if (nodeName.isEmpty()) throw new IllegalArgumentException("节点名称为空");
        String targetFile = nodeName.endsWith(".json") ? nodeName : nodeName + ".json";
        String targetFileDisabled = targetFile + DISABLED_SUFFIX;
        SSHClient ssh = null;
        try {
            ssh = createSshClient(node.getInstanceId());
            try (Session whichSession = ssh.startSession()) {
                String whichOut = execAndRead(whichSession,
                    "test -f " + CONF_DIR + "/" + targetFile + " && echo json; test -f " + CONF_DIR + "/" + targetFileDisabled + " && echo disabled");
                String which = (whichOut != null ? whichOut : "").trim();
                boolean hasJson = which.contains("json");
                boolean hasDisabled = which.contains("disabled");
                if (!hasJson && !hasDisabled) {
                    throw new IOException("服务器上未找到: " + targetFile + " 或 " + targetFileDisabled);
                }
                if (hasDisabled) {
                    try (Session rmSession = ssh.startSession()) {
                        execAndRead(rmSession, "rm -f " + CONF_DIR + "/" + targetFileDisabled + " 2>&1");
                    }
                    try {
                        removeTrafficRulesForPort(node.getInstanceId(), node.getPort() != null ? node.getPort() : 0);
                    } catch (Exception e) {
                        log.warn("移除流量规则失败: instanceId={}, port={}", node.getInstanceId(), node.getPort(), e);
                    }
                    return;
                }
            }
            String listOut;
            try (Session listSession = ssh.startSession()) {
                listOut = execAndRead(listSession, "ls -1 " + CONF_DIR + "/*.json 2>/dev/null | sed 's|.*/||'");
            }
            String[] lines = (listOut != null ? listOut : "").split("\\r?\\n");
            int index = -1;
            for (int i = 0; i < lines.length; i++) {
                if (targetFile.equals(lines[i].trim())) { index = i + 1; break; }
            }
            if (index < 1) throw new IOException("未在服务器上找到配置: " + targetFile);
            String runCmd = "printf '4\\n" + index + "\\n\\n' | sb";
            try (Session sbSession = ssh.startSession()) {
                String toRun = "bash -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                execAndRead(sbSession, toRun);
            }
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
        try {
            removeTrafficRulesForPort(node.getInstanceId(), node.getPort() != null ? node.getPort() : 0);
        } catch (Exception e) {
            log.warn("移除流量规则失败: instanceId={}, port={}", node.getInstanceId(), node.getPort(), e);
        }
    }

    // ---------- 流量统计：iptables/nft 按端口计数 ----------
    private static final String CHAIN_IN = "NODE_TRAFFIC_IN";
    private static final String CHAIN_OUT = "NODE_TRAFFIC_OUT";
    private static final Pattern IPTABLES_LINE = Pattern.compile("\\s*(\\d+)\\s+(\\d+)\\s+.*\\s+tcp\\s+.*dpt:(\\d+)");
    private static final Pattern IPTABLES_LINE_OUT = Pattern.compile("\\s*(\\d+)\\s+(\\d+)\\s+.*\\s+tcp\\s+.*spt:(\\d+)");

    public static final class TrafficPair {
        public long rx;
        public long tx;
        public TrafficPair(long rx, long tx) { this.rx = rx; this.tx = tx; }
    }

    /**
     * 为节点端口确保 iptables 计数规则存在（幂等）。
     */
    public void ensureTrafficRulesForPort(Long instanceId, int port) throws IOException {
        if (instanceId == null || port <= 0) return;
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            boolean useNft;
            try (Session detectSession = ssh.startSession()) {
                String detect = execAndRead(detectSession, "command -v iptables >/dev/null 2>&1 && echo iptables; command -v nft >/dev/null 2>&1 && echo nft");
                useNft = detect != null && detect.trim().contains("nft");
            }
            try (Session dataSession = ssh.startSession()) {
                if (useNft) {
                    ensureTrafficRulesNft(dataSession, port);
                } else {
                    ensureTrafficRulesIptables(dataSession, port);
                }
            }
        } finally {
            if (ssh != null) try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
        }
    }

    private void ensureTrafficRulesIptables(Session session, int port) throws IOException {
        String addIn = "iptables -C " + CHAIN_IN + " -p tcp --dport " + port + " -j ACCEPT 2>/dev/null || iptables -A " + CHAIN_IN + " -p tcp --dport " + port + " -j ACCEPT";
        String addOut = "iptables -C " + CHAIN_OUT + " -p tcp --sport " + port + " -j ACCEPT 2>/dev/null || iptables -A " + CHAIN_OUT + " -p tcp --sport " + port + " -j ACCEPT";
        execAndRead(session, "iptables -N " + CHAIN_IN + " 2>/dev/null; iptables -N " + CHAIN_OUT + " 2>/dev/null; " +
            "iptables -C INPUT -p tcp -j " + CHAIN_IN + " 2>/dev/null || iptables -A INPUT -p tcp -j " + CHAIN_IN + "; " +
            "iptables -C OUTPUT -p tcp -j " + CHAIN_OUT + " 2>/dev/null || iptables -A OUTPUT -p tcp -j " + CHAIN_OUT + "; " +
            addIn + "; " + addOut);
    }

    private void ensureTrafficRulesNft(Session session, int port) throws IOException {
        String create = "nft add table inet node_traffic 2>/dev/null; " +
            "nft add chain inet node_traffic in { type filter hook input priority 0\\; policy accept\\; } 2>/dev/null; " +
            "nft add chain inet node_traffic out { type filter hook output priority 0\\; policy accept\\; } 2>/dev/null; ";
        // 先清理同端口历史规则，避免重复规则导致读取到 0 计数的尾部规则
        String cleanIn = "while nft delete rule inet node_traffic in tcp dport " + port + " counter accept 2>/dev/null; do :; done";
        String cleanOut = "while nft delete rule inet node_traffic out tcp sport " + port + " counter accept 2>/dev/null; do :; done";
        String addIn = "nft add rule inet node_traffic in tcp dport " + port + " counter accept";
        String addOut = "nft add rule inet node_traffic out tcp sport " + port + " counter accept";
        execAndRead(session, "bash -c '" + create + cleanIn + "; " + cleanOut + "; " + addIn + "; " + addOut + "'");
    }

    /**
     * 删除该端口在流量链中的规则。
     */
    public void removeTrafficRulesForPort(Long instanceId, int port) throws IOException {
        if (instanceId == null || port <= 0) return;
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            boolean useNft;
            try (Session detectSession = ssh.startSession()) {
                String detect = execAndRead(detectSession, "command -v nft >/dev/null 2>&1 && echo nft; command -v iptables >/dev/null 2>&1 && echo iptables");
                useNft = detect != null && detect.trim().startsWith("nft");
            }
            try (Session dataSession = ssh.startSession()) {
                if (useNft) {
                    String del = "bash -c 'while nft delete rule inet node_traffic in tcp dport " + port + " counter accept 2>/dev/null; do :; done; " +
                        "while nft delete rule inet node_traffic out tcp sport " + port + " counter accept 2>/dev/null; do :; done'";
                    execAndRead(dataSession, del);
                } else {
                    execAndRead(dataSession, "iptables -D " + CHAIN_IN + " -p tcp --dport " + port + " -j ACCEPT 2>/dev/null; " +
                        "iptables -D " + CHAIN_OUT + " -p tcp --sport " + port + " -j ACCEPT 2>/dev/null");
                }
            }
        } finally {
            if (ssh != null) try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
        }
    }

    /**
     * 读取实例上各端口的流量计数器（字节），key=port, value=rx/tx。
     */
    public Map<Integer, TrafficPair> readTrafficCounters(Long instanceId) {
        Map<Integer, TrafficPair> result = new HashMap<>();
        if (instanceId == null) return result;
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            boolean useNft;
            try (Session detectSession = ssh.startSession()) {
                String detect = execAndRead(detectSession, "command -v nft >/dev/null 2>&1 && echo nft; command -v iptables >/dev/null 2>&1 && echo iptables");
                useNft = detect != null && detect.trim().startsWith("nft");
            }
            try (Session dataSession = ssh.startSession()) {
                if (useNft) {
                    readTrafficCountersNft(dataSession, result);
                } else {
                    readTrafficCountersIptables(dataSession, result);
                }
            }
        } catch (Exception e) {
            log.warn("readTrafficCounters instanceId={} failed: {}", instanceId, e.getMessage());
        } finally {
            if (ssh != null) try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
        }
        return result;
    }

    private void readTrafficCountersIptables(Session session, Map<Integer, TrafficPair> result) throws IOException {
        String inOut = execAndRead(session, "iptables -L " + CHAIN_IN + " -v -n -x 2>/dev/null");
        String outOut = execAndRead(session, "iptables -L " + CHAIN_OUT + " -v -n -x 2>/dev/null");
        if (inOut != null) {
            for (String line : inOut.split("\\r?\\n")) {
                Matcher m = IPTABLES_LINE.matcher(line);
                if (m.find()) {
                    int port = Integer.parseInt(m.group(3));
                    long bytes = Long.parseLong(m.group(2));
                    result.computeIfAbsent(port, p -> new TrafficPair(0, 0)).rx = bytes;
                }
            }
        }
        if (outOut != null) {
            for (String line : outOut.split("\\r?\\n")) {
                Matcher m = IPTABLES_LINE_OUT.matcher(line);
                if (m.find()) {
                    int port = Integer.parseInt(m.group(3));
                    long bytes = Long.parseLong(m.group(2));
                    result.computeIfAbsent(port, p -> new TrafficPair(0, 0)).tx = bytes;
                }
            }
        }
    }

    private void readTrafficCountersNft(Session session, Map<Integer, TrafficPair> result) throws IOException {
        String full = execAndRead(session, "nft list table inet node_traffic 2>/dev/null");
        if (full == null) return;
        // 兼容 "counter packets N bytes N" 与 "counter bytes N packets N" 两种 nft 输出顺序
        Pattern nftRule = Pattern.compile("tcp (dport|sport) (\\d+) .*? bytes (\\d+)");
        for (String line : full.split("\\r?\\n")) {
            Matcher m = nftRule.matcher(line);
            if (m.find()) {
                int port = Integer.parseInt(m.group(2));
                long bytes = Long.parseLong(m.group(3));
                TrafficPair pair = result.computeIfAbsent(port, p -> new TrafficPair(0, 0));
                if ("dport".equals(m.group(1))) {
                    pair.rx = Math.max(pair.rx, bytes);
                } else {
                    pair.tx = Math.max(pair.tx, bytes);
                }
            }
        }
    }

}
