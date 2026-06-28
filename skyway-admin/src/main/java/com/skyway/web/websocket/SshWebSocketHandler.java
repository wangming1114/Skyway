package com.skyway.web.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.service.IProxyNodeService;
import com.skyway.resource.service.IProxyNodeTrafficService;
import com.skyway.resource.service.IVpsInstanceService;
import com.skyway.web.service.VpsSshCommandService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;

/**
 * SSH WebSocket 处理器：使用实例 SSH 信息连接远程，建立 Shell+PTY 并与会话双向桥接
 *
 * @author ruoyi
 */
@Component
public class SshWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SshWebSocketHandler.class);
    private static final String ATTR_INSTANCE_ID = "instanceId";

    /** SFTP 二进制分片魔术头，用于区分终端按键与上传分片，避免误把按键写入文件 */
    private static final byte[] SFTP_CHUNK_MAGIC = "SFT0CHNK".getBytes(StandardCharsets.UTF_8);
    private static final int PTY_COLS = 80;
    private static final int PTY_ROWS = 24;
    /** 仅允许的一键安装命令：233boy sing-box 官方安装脚本，禁止任意命令执行 */
    private static final String ALLOWED_EXEC_INSTALL_SINGBOX =
            "bash <(wget -qO- -o- https://github.com/233boy/sing-box/raw/main/install.sh)";

    /** 一键三网回程检查：兼容无 curl 时用 wget（Alpine 等） */
    private static final String PREDEFINED_EXEC_BACKTRACE =
            "(curl -sSf https://raw.githubusercontent.com/zhanghanyun/backtrace/main/install.sh 2>/dev/null || wget -qO- https://raw.githubusercontent.com/zhanghanyun/backtrace/main/install.sh 2>/dev/null) | sh";

    /** 一键融合怪脚本：安装后通过 echo option | goecs 传入选项（0-10） */
    private static final String PREDEFINED_EXEC_GOECS_BASE =
            "export noninteractive=true && curl -LsSf https://raw.githubusercontent.com/oneclickvirt/ecs/master/goecs.sh -o goecs.sh && chmod +x goecs.sh && ./goecs.sh install";

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Autowired
    private VpsSshCommandService vpsSshCommandService;

    @Autowired
    private IProxyNodeTrafficService proxyNodeTrafficService;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ssh-ws-bridge-" + r.hashCode());
        t.setDaemon(true);
        return t;
    });

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        Object instanceIdObj = wsSession.getAttributes().get(ATTR_INSTANCE_ID);
        if (instanceIdObj == null) {
            closeAndLog(wsSession, "missing instanceId");
            return;
        }
        Long instanceId = (Long) instanceIdObj;
        VpsInstance instance = vpsInstanceService.getById(instanceId);
        if (instance == null || StringUtils.isEmpty(instance.getIp()) || instance.getSshPort() == null
                || StringUtils.isEmpty(instance.getSshUsername())) {
            sendErrorAndClose(wsSession, "实例不存在或 SSH 信息不完整");
            return;
        }
        String password = instance.getSshPassword();
        if (password == null) {
            password = "";
        }
        SSHClient ssh = null;
        Session session = null;
        try {
            ssh = new SSHClient();
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(instance.getIp(), instance.getSshPort() != null ? instance.getSshPort() : 22);
            ssh.authPassword(instance.getSshUsername(), password);

            session = ssh.startSession();
            session.allocateDefaultPTY();
            Session.Shell shell = session.startShell();

            InputStream fromShell = shell.getInputStream();
            OutputStream toShell = shell.getOutputStream();

            wsSession.getAttributes().put("sshClient", ssh);
            wsSession.getAttributes().put("session", session);
            wsSession.getAttributes().put("shell", shell);

            AtomicBoolean closed = new AtomicBoolean(false);
            wsSession.getAttributes().put("closed", closed);

            executor.execute(() -> {
                byte[] buf = new byte[4096];
                try {
                    while (!closed.get() && wsSession.isOpen()) {
                        int n = fromShell.read(buf);
                        if (n <= 0) break;
                        if (wsSession.isOpen()) {
                            synchronized (wsSession) {
                                wsSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(buf, 0, n)));
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!closed.get()) log.debug("SSH read closed: {}", e.getMessage());
                } catch (Exception e) {
                    log.warn("SSH->WS copy error: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("SSH 连接失败 instanceId={}, host={}, user={}: {}", instanceId, instance.getIp(),
                    instance.getSshUsername(), e.getMessage());
            if (ssh != null) try { ssh.close(); } catch (IOException ignored) {}
            if (session != null) try { session.close(); } catch (IOException ignored) {}
            sendErrorAndClose(wsSession, "SSH 连接失败: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (StringUtils.isEmpty(payload)) return;
        JSONObject obj = null;
        try {
            obj = JSON.parseObject(payload);
        } catch (Exception ignored) {
        }
        if (obj != null && obj.containsKey("type")) {
            String type = obj.getString("type");
            if ("resize".equals(type)) {
                // PTY resize: SSHj version may not expose getWindow(); no-op here, terminal still works
                return;
            }
            if ("sysinfo".equals(type)) {
                handleSysinfo(wsSession, obj);
                return;
            }
            if ("exec".equals(type)) {
                handleExec(wsSession, obj);
                return;
            }
            if ("get_goecs_menu".equals(type)) {
                handleGetGoecsMenu(wsSession);
                return;
            }
            if ("add_proxy_node".equals(type)) {
                handleAddProxyNode(wsSession, obj);
                return;
            }
            if ("remove_proxy_node".equals(type)) {
                handleRemoveProxyNode(wsSession, obj);
                return;
            }
            if (type != null && type.startsWith("sftp_")) {
                if ("sftp_download_cancel".equals(type)) {
                    Object reqId = obj.get("_id");
                    if (reqId != null) {
                        @SuppressWarnings("unchecked")
                        Set<Object> set = (Set<Object>) wsSession.getAttributes().computeIfAbsent("downloadCancelledReqIds", k -> ConcurrentHashMap.newKeySet());
                        set.add(reqId);
                    }
                    return;
                }
                if ("sftp_upload_chunk_bin".equals(type)) {
                    String pathVal = obj.getString("path");
                    String path = pathVal != null ? pathVal.trim() : "/";
                    long offset = obj.getLongValue("offset");
                    Object reqId = obj.get("_id");
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("path", path);
                    meta.put("offset", offset);
                    meta.put("reqId", reqId);
                    wsSession.getAttributes().put("uploadPendingChunk", meta);
                    return;
                }
                handleSftp(wsSession, obj);
                return;
            }
        }
        Object toShell = wsSession.getAttributes().get("shell");
        if (toShell instanceof net.schmizz.sshj.connection.channel.direct.Session.Shell) {
            OutputStream out = ((net.schmizz.sshj.connection.channel.direct.Session.Shell) toShell).getOutputStream();
            if (out != null) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    /**
     * 处理 exec 类型消息：仅允许预置命令。支持 command（兼容 sing-box）或 commandId（backtrace / goecs）。
     */
    private void handleExec(WebSocketSession wsSession, JSONObject obj) {
        Object reqId = obj.get("reqId");
        String commandId = obj.getString("commandId");
        String command = obj.getString("command");
        String toRun = null;
        String execKind = null; // "singbox" | "backtrace" | "goecs"

        if (StringUtils.isNotEmpty(commandId)) {
            if ("backtrace".equals(commandId)) {
                toRun = PREDEFINED_EXEC_BACKTRACE;
                execKind = "backtrace";
            } else if ("goecs".equals(commandId)) {
                int option = 1;
                Object optObj = obj.get("option");
                if (optObj != null) {
                    if (optObj instanceof Number) {
                        option = ((Number) optObj).intValue();
                    } else {
                        try {
                            option = Integer.parseInt(optObj.toString().trim());
                        } catch (NumberFormatException e) {
                            option = 1;
                        }
                    }
                    if (option < 0 || option > 10) option = 1;
                }
                toRun = PREDEFINED_EXEC_GOECS_BASE + " && echo " + option + " | goecs";
                execKind = "goecs";
            } else {
                sendExecError(wsSession, reqId, "不支持的 commandId: " + commandId);
                return;
            }
        } else if (StringUtils.isNotEmpty(command)) {
            String trimmed = command.trim();
            if (trimmed.contains(";") || trimmed.contains("&&") || trimmed.contains("||")
                    || trimmed.contains("\n") || trimmed.contains("\r")) {
                sendExecError(wsSession, reqId, "不允许的命令格式");
                return;
            }
            if (ALLOWED_EXEC_INSTALL_SINGBOX.equals(trimmed)) {
                toRun = trimmed;
                execKind = "singbox";
            } else {
                sendExecError(wsSession, reqId, "仅支持预置的 sing-box 安装命令");
                return;
            }
        } else {
            sendExecError(wsSession, reqId, "缺少 command 或 commandId");
            return;
        }

        Object sshObj = wsSession.getAttributes().get("sshClient");
        if (!(sshObj instanceof SSHClient)) {
            sendExecError(wsSession, reqId, "SSH 未连接");
            return;
        }
        SSHClient ssh = (SSHClient) sshObj;
        final String finalToRun = toRun;
        final String finalKind = execKind;
        executor.execute(() -> {
            Session newSession = null;
            Command cmd = null;
            try {
                if (!ensurePackageForInstall(wsSession, reqId, ssh, "bash")) {
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }
                if ("singbox".equals(finalKind)) {
                    if (!ensurePackageForInstall(wsSession, reqId, ssh, "wget")) {
                        sendExecEnd(wsSession, reqId, -1);
                        return;
                    }
                } else if ("backtrace".equals(finalKind)) {
                    boolean hasCurl = checkCommandExists(ssh, "curl");
                    boolean hasWget = checkCommandExists(ssh, "wget");
                    if (!hasCurl && !hasWget) {
                        if (!ensurePackageForInstall(wsSession, reqId, ssh, "curl")) {
                            if (!ensurePackageForInstall(wsSession, reqId, ssh, "wget")) {
                                sendExecError(wsSession, reqId, "需要 curl 或 wget，自动安装失败");
                                sendExecEnd(wsSession, reqId, -1);
                                return;
                            }
                        }
                    }
                } else if ("goecs".equals(finalKind)) {
                    if (!ensurePackageForInstall(wsSession, reqId, ssh, "curl")) {
                        sendExecEnd(wsSession, reqId, -1);
                        return;
                    }
                }

                newSession = ssh.startSession();
                String escaped = finalToRun.replace("\\", "\\\\").replace("\"", "\\\"");
                String runCmd = "bash -c \"" + escaped + "\"";
                cmd = newSession.exec(runCmd);
                InputStream stdout = cmd.getInputStream();
                InputStream stderr = cmd.getErrorStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = stdout.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    sendExecOutput(wsSession, reqId, data, false);
                }
                while ((n = stderr.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    sendExecOutput(wsSession, reqId, data, true);
                }
                int timeoutSeconds = "goecs".equals(finalKind) ? 300 : 120;
                cmd.join(timeoutSeconds, TimeUnit.SECONDS);
                Integer exitStatus = cmd.getExitStatus();
                if (exitStatus == null) {
                    sendExecError(wsSession, reqId, "进程未返回退出码（可能超时或连接中断），请根据上方日志排查");
                }
                if (exitStatus == null) exitStatus = -1;
                int finalCode = exitStatus;
                if (finalCode != 0 && "singbox".equals(finalKind) && isSingBoxInstalled(ssh)) {
                    finalCode = 0;
                }
                sendExecEnd(wsSession, reqId, finalCode);
            } catch (Exception e) {
                log.debug("exec error: {}", e.getMessage());
                sendExecEnd(wsSession, reqId, -1);
                try {
                    JSONObject err = new JSONObject();
                    err.put("type", "exec_error");
                    err.put("reqId", reqId);
                    err.put("message", e.getMessage());
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(err.toJSONString()));
                    }
                } catch (Exception ignored) {}
            } finally {
                if (cmd != null) {
                    try { cmd.close(); } catch (IOException ignored) {}
                }
                if (newSession != null) {
                    try { newSession.close(); } catch (IOException ignored) {}
                }
            }
        });
    }

    private boolean checkCommandExists(SSHClient ssh, String name) {
        try (Session s = ssh.startSession()) {
            String out = execAndRead(s, "command -v " + name + " >/dev/null 2>&1 && echo yes || echo no");
            return out != null && out.trim().toLowerCase().startsWith("yes");
        } catch (IOException e) {
            return false;
        }
    }

    private static final Pattern GOECS_MENU_LINE = Pattern.compile("^\\s*(\\d+)\\.\\s+(.+)$");

    /**
     * 在服务器上执行 echo 0 | goecs，解析菜单输出并回传选项列表（type: goecs_menu）。
     * 若 goecs 未安装或执行失败，回传空数组，前端保留默认选项。
     */
    private void handleGetGoecsMenu(WebSocketSession wsSession) {
        Object sshObj = wsSession.getAttributes().get("sshClient");
        if (!(sshObj instanceof SSHClient)) {
            sendGoecsMenu(wsSession, new java.util.ArrayList<>());
            return;
        }
        SSHClient ssh = (SSHClient) sshObj;
        executor.execute(() -> {
            java.util.List<JSONObject> options = new java.util.ArrayList<>();
            try (Session session = ssh.startSession()) {
                String raw = execAndRead(session, "sh -c " + quoteSh("echo 0 | goecs 2>&1"));
                if (raw != null && !raw.isEmpty()) {
                    String cleaned = stripAnsi(raw);
                    for (String line : cleaned.split("\\r?\\n")) {
                        Matcher m = GOECS_MENU_LINE.matcher(line.trim());
                        if (m.matches()) {
                            int value = Integer.parseInt(m.group(1));
                            String label = m.group(2).trim();
                            if (value >= 0 && value <= 10) {
                                JSONObject item = new JSONObject();
                                item.put("value", value);
                                item.put("label", label);
                                options.add(item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("get_goecs_menu: {}", e.getMessage());
            }
            sendGoecsMenu(wsSession, options);
        });
    }

    private void sendGoecsMenu(WebSocketSession wsSession, java.util.List<JSONObject> options) {
        try {
            JSONObject out = new JSONObject();
            out.put("type", "goecs_menu");
            out.put("options", options);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(out.toJSONString()));
            }
        } catch (Exception ignored) {}
    }

    /**
     * 一键安装前确保依赖已安装：若缺失则按 apk/dnf/yum/apt-get 自动安装，并将输出写入前端日志。
     * @return true 表示已可用（原本就有或安装成功），false 表示仍不可用且已发送错误
     */
    private boolean ensurePackageForInstall(WebSocketSession wsSession, Object reqId, SSHClient ssh, String pkg) {
        try (Session check = ssh.startSession()) {
            String out = execAndRead(check, "command -v " + pkg + " >/dev/null 2>&1 && echo yes || echo no");
            if (out != null && out.trim().toLowerCase().startsWith("yes")) {
                return true;
            }
        } catch (IOException e) {
            log.debug("ensurePackage check {}: {}", pkg, e.getMessage());
            sendExecError(wsSession, reqId, "检查 " + pkg + " 时出错: " + e.getMessage());
            return false;
        }
        String installCmd = "command -v " + pkg + " >/dev/null 2>&1 && exit 0;"
            + " if command -v apk >/dev/null 2>&1; then echo '[自动安装 " + pkg + "] apk...'; apk add --no-cache " + pkg + " 2>&1; fi;"
            + " if command -v dnf >/dev/null 2>&1; then echo '[自动安装 " + pkg + "] dnf...'; dnf install -y " + pkg + " 2>&1; fi;"
            + " if command -v yum >/dev/null 2>&1; then echo '[自动安装 " + pkg + "] yum...'; yum install -y " + pkg + " 2>&1; fi;"
            + " if command -v apt-get >/dev/null 2>&1; then echo '[自动安装 " + pkg + "] apt-get...'; apt-get update -qq 2>&1; apt-get install -y " + pkg + " 2>&1; fi;"
            + " exit 0";
        try (Session installSession = ssh.startSession()) {
            String installOut = execAndRead(installSession, "sh -c " + quoteSh(installCmd));
            if (installOut != null && !installOut.isEmpty()) {
                sendExecOutput(wsSession, reqId, installOut.trim() + "\n", false);
            }
        } catch (IOException e) {
            log.debug("ensurePackage install {}: {}", pkg, e.getMessage());
            sendExecError(wsSession, reqId, "自动安装 " + pkg + " 时出错: " + e.getMessage());
            return false;
        }
        try (Session recheck = ssh.startSession()) {
            String again = execAndRead(recheck, "command -v " + pkg + " >/dev/null 2>&1 && echo yes || echo no");
            if (again != null && again.trim().toLowerCase().startsWith("yes")) {
                return true;
            }
        } catch (IOException e) {
            log.debug("ensurePackage recheck {}: {}", pkg, e.getMessage());
        }
        sendExecError(wsSession, reqId, "无法自动安装 " + pkg + "（需要 root 或具备安装权限的账号），请手动安装后重试");
        return false;
    }

    private static String quoteSh(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }

    private void sendExecOutput(WebSocketSession wsSession, Object reqId, String data, boolean stderr) {
        try {
            JSONObject out = new JSONObject();
            out.put("type", "exec_output");
            out.put("data", data);
            if (reqId != null) out.put("reqId", reqId);
            if (stderr) out.put("stderr", true);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(out.toJSONString()));
            }
        } catch (Exception ignored) {}
    }

    private void sendExecEnd(WebSocketSession wsSession, Object reqId, int code) {
        try {
            JSONObject out = new JSONObject();
            out.put("type", "exec_end");
            if (reqId != null) out.put("reqId", reqId);
            out.put("code", code);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(out.toJSONString()));
            }
        } catch (Exception ignored) {}
    }

    private void sendExecError(WebSocketSession wsSession, Object reqId, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("type", "exec_error");
            if (reqId != null) err.put("reqId", reqId);
            err.put("message", message);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(err.toJSONString()));
            }
        } catch (Exception ignored) {}
    }

    /**
     * 添加节点：根据 nodeType 分支执行 sb 对应菜单，重命名配置，解析输出并入库。支持 VLESS-REALITY、VMess-TCP。
     */
    private void handleAddProxyNode(WebSocketSession wsSession, JSONObject obj) {
        Object reqId = obj.get("reqId");
        Integer port = obj.getInteger("port");
        String expireTimeStr = obj.getString("expireTime");
        Long customerId = obj.getLong("customerId");
        String remark = obj.getString("remark");
        String nodeType = obj.getString("nodeType");
        if (nodeType != null) nodeType = nodeType.trim();
        if (nodeType == null || nodeType.isEmpty()) {
            nodeType = "VLESS-REALITY";
        }
        if (!"VLESS-REALITY".equals(nodeType) && !"VMess-TCP".equals(nodeType)) {
            sendExecError(wsSession, reqId, "不支持的协议类型: " + nodeType);
            sendExecEnd(wsSession, reqId, -1);
            return;
        }

        if (customerId == null) {
            sendExecError(wsSession, reqId, "请选择归属客户");
            return;
        }
        String customerIdStr = String.valueOf(customerId);
        if (port == null || port < 1 || port > 65535) {
            sendExecError(wsSession, reqId, "端口无效（1-65535）");
            return;
        }

        Object instanceIdObj = wsSession.getAttributes().get(ATTR_INSTANCE_ID);
        if (!(instanceIdObj instanceof Long)) {
            sendExecError(wsSession, reqId, "缺少实例信息");
            return;
        }
        Long instanceId = (Long) instanceIdObj;
        final String nodeTypeFinal = nodeType;

        executor.execute(() -> {
            SSHClient ssh = null;
            Session newSession = null;
            Command cmd = null;
            StringBuilder fullOutput = new StringBuilder();
            try {
                ssh = createSshClient(instanceId);
                try (Session checkSession = ssh.startSession()) {
                    String sbCheck = execAndRead(checkSession, "command -v sb 2>/dev/null || which sb 2>/dev/null || echo ''");
                    if (sbCheck == null || sbCheck.trim().isEmpty()) {
                        sendExecOutput(wsSession, reqId, "未检测到 sing-box 脚本 (sb)，请先在服务器上安装 sing-box。\n", false);
                        sendExecError(wsSession, reqId, "未检测到 sb 命令，请先安装 sing-box");
                        sendExecEnd(wsSession, reqId, -1);
                        return;
                    }
                }

                String runCmd;
                String oldJsonName;
                String typeLabel;
                if ("VMess-TCP".equals(nodeTypeFinal)) {
                    typeLabel = "VMess-TCP";
                    oldJsonName = "VMess-TCP-" + port + ".json";
                    runCmd = "printf '1\\n5\\n" + port + "\\n' | sb";
                } else {
                    typeLabel = "VLESS-REALITY";
                    oldJsonName = "VLESS-REALITY-" + port + ".json";
                    runCmd = "printf '1\\n18\\n" + port + "\\n' | sb";
                }

                String hint = "正在执行: 添加配置 -> " + typeLabel + " -> 端口 " + port + "\n";
                sendExecOutput(wsSession, reqId, hint, false);
                fullOutput.append(hint);

                newSession = ssh.startSession();
                String toRun = "sh -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                cmd = newSession.exec(toRun);

                InputStream stdout = cmd.getInputStream();
                InputStream stderr = cmd.getErrorStream();
                byte[] buf = new byte[4096];
                int n;
                boolean portConflict = false;
                while ((n = stdout.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    fullOutput.append(data);
                    sendExecOutput(wsSession, reqId, data, false);
                    String soFar = stripAnsi(fullOutput.toString());
                    if (soFar.contains("无法使用") && soFar.contains("端口")) {
                        portConflict = true;
                        break;
                    }
                }
                if (portConflict) {
                    try { if (cmd != null) cmd.close(); } catch (IOException ignored) {}
                    sendExecError(wsSession, reqId, "端口 (" + port + ") 已被占用，请选择其他端口重试（可使用推荐端口）");
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }
                while ((n = stderr.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    fullOutput.append(data);
                    sendExecOutput(wsSession, reqId, data, true);
                }
                cmd.join(30, TimeUnit.SECONDS);
                Integer exitStatus = cmd.getExitStatus();
                if (exitStatus == null) exitStatus = -1;
                int exitCode = exitStatus;

                String output = fullOutput.toString();
                output = stripAnsi(output);

                if (output.contains("无法使用") && output.contains("端口")) {
                    sendExecError(wsSession, reqId, "端口 (" + port + ") 已被占用，请选择其他端口重试（可使用推荐端口）");
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }

                ProxyNode parsed;
                if ("VMess-TCP".equals(nodeTypeFinal)) {
                    parsed = parseSbVmessTcpOutput(output, port);
                } else {
                    parsed = parseSbVlessRealityOutput(output, port);
                }
                if (parsed == null || parsed.getUrl() == null || parsed.getUrl().isEmpty()) {
                    sendExecError(wsSession, reqId, "解析输出失败，请查看上方日志");
                    sendExecEnd(wsSession, reqId, exitCode != 0 ? exitCode : -1);
                    return;
                }

                Date expireDate = parseExpireTime(expireTimeStr);
                String customPart = customerIdStr;
                String targetBaseName = buildNodeBaseName(typeLabel, parsed.getAddress(), port, customPart, expireDate);
                String newJsonName = targetBaseName + ".json";
                String confDir = "/etc/sing-box/conf";
                String mvCmd = "mv " + confDir + "/" + oldJsonName + " " + confDir + "/" + newJsonName + " 2>&1";

                sendExecOutput(wsSession, reqId, "重命名配置: " + oldJsonName + " -> " + newJsonName + "\n", false);
                String mvOut;
                try (Session mvSession = ssh.startSession()) {
                    mvOut = execAndRead(mvSession, mvCmd);
                }
                if (mvOut != null) sendExecOutput(wsSession, reqId, mvOut, false);
                if (mvOut != null && mvOut.toLowerCase().contains("no such file")) {
                    sendExecError(wsSession, reqId, "重命名失败: 未找到 " + oldJsonName);
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }

                parsed.setInstanceId(instanceId);
                parsed.setCustomerId(customerId);
                parsed.setNodeName(targetBaseName);
                parsed.setCustomId(customerIdStr);
                parsed.setExpireTime(expireDate);
                parsed.setStatus("0");
                if (remark != null && !remark.trim().isEmpty()) parsed.setRemark(remark.trim());
                try {
                    proxyNodeService.insert(parsed);
                    try {
                        vpsSshCommandService.ensureTrafficRulesForPort(instanceId, parsed.getPort() != null ? parsed.getPort() : port);
                    } catch (Exception ex) {
                        log.warn("ensureTrafficRulesForPort instanceId={}, port={} failed: {}", instanceId, port, ex.getMessage());
                    }
                    sendExecOutput(wsSession, reqId, "节点已保存到数据库。\n", false);
                    sendExecEnd(wsSession, reqId, 0);
                    JSONObject nodeCreated = new JSONObject();
                    nodeCreated.put("type", "node_created");
                    if (reqId != null) nodeCreated.put("reqId", reqId);
                    nodeCreated.put("node", toJsonNode(parsed));
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(nodeCreated.toJSONString()));
                    }
                } catch (Exception e) {
                    log.warn("proxy node insert failed: {}", e.getMessage());
                    sendExecError(wsSession, reqId, "保存节点失败: " + e.getMessage());
                    sendExecEnd(wsSession, reqId, -1);
                }
            } catch (Exception e) {
                log.debug("add_proxy_node error: {}", e.getMessage());
                sendExecError(wsSession, reqId, e.getMessage());
                sendExecEnd(wsSession, reqId, -1);
            } finally {
                if (cmd != null) try { cmd.close(); } catch (IOException ignored) {}
                if (newSession != null) try { newSession.close(); } catch (IOException ignored) {}
                if (ssh != null) try { ssh.close(); } catch (IOException ignored) {}
            }
        });
    }

    private static String buildNodeBaseName(String nodeType, String address, Integer port, String customerId, Date expireDate) {
        String typePart = StringUtils.isNotEmpty(nodeType) ? nodeType.trim() : "UNKNOWN";
        String addressPart = StringUtils.isNotEmpty(address) ? address.trim() : "unknown";
        String portPart = port != null ? String.valueOf(port) : "0";
        String customerPart = StringUtils.isNotEmpty(customerId) ? customerId.trim() : "0";
        String expiryTag = expireDate == null ? "permanent" : new SimpleDateFormat("yyyyMMdd").format(expireDate);
        return typePart + "-" + addressPart + "-" + portPart + "-" + customerPart + "-" + expiryTag;
    }

    /**
     * 删除节点：根据 nodeId 查库得到 nodeName，在服务器上通过 sb 菜单 4 按名称匹配删除配置，成功后再删数据库。
     */
    private void handleRemoveProxyNode(WebSocketSession wsSession, JSONObject obj) {
        Object reqId = obj.get("reqId");
        Long nodeId = obj.getLong("nodeId");
        if (nodeId == null) {
            sendExecError(wsSession, reqId, "缺少 nodeId");
            sendExecEnd(wsSession, reqId, -1);
            return;
        }

        ProxyNode node = proxyNodeService.getById(nodeId);
        if (node == null) {
            sendExecError(wsSession, reqId, "节点不存在");
            sendExecEnd(wsSession, reqId, -1);
            return;
        }
        Long instanceId = node.getInstanceId();
        if (instanceId == null) {
            sendExecError(wsSession, reqId, "节点缺少实例信息");
            sendExecEnd(wsSession, reqId, -1);
            return;
        }

        String nodeName = node.getNodeName();
        String nameTrim = (nodeName != null ? nodeName : "").trim();
        if (nameTrim.isEmpty()) {
            sendExecError(wsSession, reqId, "节点名称为空");
            sendExecEnd(wsSession, reqId, -1);
            return;
        }
        final String targetFile = nameTrim.endsWith(".json") ? nameTrim : nameTrim + ".json";
        final String targetFileDisabled = targetFile + ".disabled";
        final Long nodeIdFinal = nodeId;
        final Long instanceIdFinal = instanceId;
        final int portFinal = node.getPort() != null ? node.getPort() : 0;

        executor.execute(() -> {
            SSHClient ssh = null;
            Session sbSession = null;
            Command cmd = null;
            try {
                ssh = createSshClient(instanceId);
                // 0. 判断实际存在的是 .json 还是 .json.disabled（停用后为后者）
                String whichOut = null;
                try (Session whichSession = ssh.startSession()) {
                    whichOut = execAndRead(whichSession,
                            "test -f /etc/sing-box/conf/" + targetFile + " && echo json; test -f /etc/sing-box/conf/" + targetFileDisabled + " && echo disabled");
                }
                String which = (whichOut != null ? whichOut : "").trim();
                boolean hasJson = which.contains("json");
                boolean hasDisabled = which.contains("disabled");

                if (!hasJson && !hasDisabled) {
                    sendExecOutput(wsSession, reqId, "服务器上未找到: " + targetFile + " 或 " + targetFileDisabled + "\n", false);
                    sendExecError(wsSession, reqId, "找不到配置文件");
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }

                if (hasDisabled) {
                    // 已停用：配置文件为 .json.disabled，sb 菜单 4 不包含该文件，直接 rm 后删库
                    sendExecOutput(wsSession, reqId, "正在删除已停用配置: " + targetFileDisabled + "\n", false);
                    try (Session rmSession = ssh.startSession()) {
                        String rmCmd = "rm -f /etc/sing-box/conf/" + targetFileDisabled + " 2>&1";
                        execAndRead(rmSession, rmCmd);
                    }
                    String verifyOut = null;
                    try (Session verifySession = ssh.startSession()) {
                        verifyOut = execAndRead(verifySession, "test -f /etc/sing-box/conf/" + targetFileDisabled + " && echo exists || echo deleted");
                    }
                    boolean fileGone = verifyOut != null && verifyOut.trim().toLowerCase().contains("deleted");
                    if (fileGone) {
                        try {
                            try { vpsSshCommandService.removeTrafficRulesForPort(instanceIdFinal, portFinal); } catch (Exception e) { log.warn("removeTrafficRulesForPort failed: {}", e.getMessage()); }
                            proxyNodeTrafficService.deleteByNodeId(nodeIdFinal);
                            proxyNodeService.deleteById(nodeIdFinal);
                            sendExecOutput(wsSession, reqId, "数据库记录已删除。\n", false);
                            sendExecEnd(wsSession, reqId, 0);
                        } catch (Exception e) {
                            log.warn("proxy node delete failed: {}", e.getMessage());
                            sendExecError(wsSession, reqId, "删除数据库记录失败: " + e.getMessage());
                            sendExecEnd(wsSession, reqId, -1);
                        }
                    } else {
                        sendExecError(wsSession, reqId, "未在服务器上确认配置已删除");
                        sendExecEnd(wsSession, reqId, -1);
                    }
                    return;
                }

                // 1. 存在 .json：获取 sb 配置列表序号（1-based），通过 sb 菜单 4 删除
                String listOut;
                try (Session listSession = ssh.startSession()) {
                    String listCmd = "ls -1 /etc/sing-box/conf/*.json 2>/dev/null | sed 's|.*/||'";
                    listOut = execAndRead(listSession, listCmd);
                }

                String[] lines = (listOut != null ? listOut : "").split("\\r?\\n");
                int index = -1;
                for (int i = 0; i < lines.length; i++) {
                    if (targetFile.equals(lines[i].trim())) {
                        index = i + 1;
                        break;
                    }
                }
                if (index < 1) {
                    sendExecOutput(wsSession, reqId, "服务器配置列表中未找到: " + targetFile + "\n", false);
                    sendExecError(wsSession, reqId, "未在服务器上找到配置: " + targetFile);
                    sendExecEnd(wsSession, reqId, -1);
                    return;
                }

                sendExecOutput(wsSession, reqId, "正在通过 sb 删除配置: " + targetFile + " (序号 " + index + ")\n", false);

                // 2. 执行 printf '4\n{index}\n\n' | sb，流式输出
                String runCmd = "printf '4\\n" + index + "\\n\\n' | sb";
                sbSession = ssh.startSession();
                String toRun = "sh -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                cmd = sbSession.exec(toRun);

                byte[] buf = new byte[4096];
                int n;
                InputStream stdout = cmd.getInputStream();
                InputStream stderr = cmd.getErrorStream();
                while ((n = stdout.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    sendExecOutput(wsSession, reqId, data, false);
                }
                while ((n = stderr.read(buf)) > 0 && wsSession.isOpen()) {
                    String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                    sendExecOutput(wsSession, reqId, data, true);
                }
                cmd.join(30, TimeUnit.SECONDS);

                String verifyOut = null;
                try (Session verifySession = ssh.startSession()) {
                    verifyOut = execAndRead(verifySession, "test -f /etc/sing-box/conf/" + targetFile + " && echo exists || echo deleted");
                }
                boolean fileGone = verifyOut != null && verifyOut.trim().toLowerCase().contains("deleted");
                if (fileGone) {
                    try {
                        try { vpsSshCommandService.removeTrafficRulesForPort(instanceIdFinal, portFinal); } catch (Exception e) { log.warn("removeTrafficRulesForPort failed: {}", e.getMessage()); }
                        proxyNodeTrafficService.deleteByNodeId(nodeIdFinal);
                        proxyNodeService.deleteById(nodeIdFinal);
                        sendExecOutput(wsSession, reqId, "数据库记录已删除。\n", false);
                        sendExecEnd(wsSession, reqId, 0);
                    } catch (Exception e) {
                        log.warn("proxy node delete failed: {}", e.getMessage());
                        sendExecError(wsSession, reqId, "删除数据库记录失败: " + e.getMessage());
                        sendExecEnd(wsSession, reqId, -1);
                    }
                } else {
                    Integer exitStatus = cmd.getExitStatus();
                    int exitCode = (exitStatus != null) ? exitStatus : -1;
                    if (exitCode != 0) sendExecError(wsSession, reqId, "sb 删除命令异常退出: " + exitCode);
                    else sendExecError(wsSession, reqId, "未在服务器上确认配置已删除，请查看上方日志");
                    sendExecEnd(wsSession, reqId, -1);
                }
            } catch (Exception e) {
                log.debug("remove_proxy_node error: {}", e.getMessage());
                sendExecError(wsSession, reqId, e.getMessage());
                sendExecEnd(wsSession, reqId, -1);
            } finally {
                if (cmd != null) try { cmd.close(); } catch (IOException ignored) {}
                if (sbSession != null) try { sbSession.close(); } catch (IOException ignored) {}
                if (ssh != null) try { ssh.close(); } catch (IOException ignored) {}
            }
        });
    }

    /** 为 add/remove 节点等操作创建独立的 SSH 连接，用毕由调用方关闭，不依赖 session 长连接 */
    private SSHClient createSshClient(Long instanceId) throws Exception {
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
    private static final Pattern P_VMESS_URL = Pattern.compile("(vmess://[^\\s]+)");
    /** 去除终端 ANSI 转义序列（如 [41m [0m），避免解析出的字段带颜色码 */
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[0-9;]*m|\\[[0-9;]+m");

    private static String stripAnsi(String s) {
        if (s == null || s.isEmpty()) return s;
        return ANSI_ESCAPE.matcher(s).replaceAll("");
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
     * 解析 sb 输出的 VMess-TCP 信息：优先匹配 vmess:// 链接并 Base64 解码取 id/aid/add/port，否则用正则匹配用户ID、alterId、地址、端口。
     */
    private ProxyNode parseSbVmessTcpOutput(String output, int defaultPort) {
        if (output == null) return null;
        String url = match1(P_VMESS_URL, output);
        if (url != null) url = stripAnsi(url);
        String id = null;
        int aid = 0;
        String address = stripAnsi(match1(P_ADDRESS, output));
        String portStr = stripAnsi(match1(P_PORT, output));
        if (address == null) address = "";
        int port = defaultPort;
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (url != null && url.startsWith("vmess://")) {
            try {
                String b64 = url.substring(8).trim();
                String json = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
                JSONObject o = JSON.parseObject(json);
                if (o != null) {
                    id = o.getString("id");
                    aid = o.getIntValue("aid");
                    if (address.isEmpty()) address = o.getString("add");
                    if (port == defaultPort && o.get("port") != null) port = o.getIntValue("port");
                }
            } catch (Exception ignored) {}
        }
        if (id == null || id.isEmpty()) {
            id = stripAnsi(match1(P_ID, output));
        }
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (url == null || url.isEmpty()) {
            String aidStr = stripAnsi(match1(Pattern.compile("alterId\\s*[=：:]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE), output));
            if (aidStr != null && !aidStr.isEmpty()) {
                try {
                    aid = Integer.parseInt(aidStr.trim());
                } catch (NumberFormatException ignored) {}
            }
            JSONObject vmess = new JSONObject();
            vmess.put("v", 2);
            vmess.put("ps", "VMess-TCP-" + port);
            vmess.put("add", address.isEmpty() ? "0.0.0.0" : address);
            vmess.put("port", port);
            vmess.put("id", id.trim());
            vmess.put("aid", aid);
            vmess.put("net", "tcp");
            vmess.put("type", "none");
            vmess.put("host", "");
            vmess.put("path", "");
            vmess.put("tls", "");
            url = "vmess://" + Base64.getEncoder().encodeToString(vmess.toJSONString().getBytes(StandardCharsets.UTF_8));
        }

        ProxyNode node = new ProxyNode();
        node.setNodeType("VMess-TCP");
        node.setAddress(address != null ? address.trim() : "");
        node.setPort(port);
        node.setUrl(url);
        node.setNodeName("VMess-TCP-" + port);

        JSONObject config = new JSONObject();
        config.put("protocol", "vmess");
        config.put("id", id != null ? id.trim() : "");
        config.put("aid", aid);
        config.put("network", "tcp");
        node.setConfigJson(config.toJSONString());
        return node;
    }

    private static String match1(Pattern p, String text) {
        if (text == null) return null;
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /** 文件名片段安全化：仅保留字母数字、横线、下划线 */
    private static String sanitizeFilenamePart(String s) {
        if (s == null || s.isEmpty()) return "na";
        return s.replaceAll("[^a-zA-Z0-9_-]", "_");
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

    private static JSONObject toJsonNode(ProxyNode node) {
        JSONObject o = new JSONObject();
        o.put("id", node.getId());
        o.put("instanceId", node.getInstanceId());
        o.put("nodeName", node.getNodeName());
        o.put("nodeType", node.getNodeType());
        o.put("address", node.getAddress());
        o.put("port", node.getPort());
        o.put("url", node.getUrl());
        o.put("expireTime", node.getExpireTime() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(node.getExpireTime()) : null);
        o.put("status", node.getStatus());
        o.put("remark", node.getRemark());
        return o;
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession wsSession, BinaryMessage message) throws Exception {
        Object pendingObj = wsSession.getAttributes().get("uploadPendingChunk");
        if (pendingObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) pendingObj;
            wsSession.getAttributes().remove("uploadPendingChunk");
            ByteBuffer payload = message.getPayload();
            int rem = payload.remaining();
            if (rem >= SFTP_CHUNK_MAGIC.length) {
                byte[] magicBuf = new byte[SFTP_CHUNK_MAGIC.length];
                ByteBuffer dup = payload.duplicate();
                dup.get(magicBuf);
                boolean magicOk = true;
                for (int i = 0; i < SFTP_CHUNK_MAGIC.length; i++) {
                    if (magicBuf[i] != SFTP_CHUNK_MAGIC[i]) {
                        magicOk = false;
                        break;
                    }
                }
                if (magicOk) {
                    int chunkLen = rem - SFTP_CHUNK_MAGIC.length;
                    byte[] chunkBytes = new byte[chunkLen];
                    payload.position(payload.position() + SFTP_CHUNK_MAGIC.length);
                    payload.get(chunkBytes);
                    final byte[] bytes = chunkBytes;
                    executor.execute(() -> processUploadChunk(wsSession, meta, bytes));
                    return;
                }
            }
        }
        Object toShell = wsSession.getAttributes().get("shell");
        if (toShell instanceof net.schmizz.sshj.connection.channel.direct.Session.Shell) {
            OutputStream out = ((net.schmizz.sshj.connection.channel.direct.Session.Shell) toShell).getOutputStream();
            if (out != null) {
                out.write(message.getPayload().array(), message.getPayload().position(), message.getPayload().remaining());
                out.flush();
            }
        }
    }

    private static final int SFTP_WRITE_PIECE = 32 * 1024;

    private void processUploadChunk(WebSocketSession wsSession, Map<String, Object> meta, byte[] bytes) {
        try {
            Object rfObj = wsSession.getAttributes().get("uploadRemoteFile");
            if (!(rfObj instanceof RemoteFile)) {
                sendSftpError(wsSession, "sftp_upload_chunk", "没有正在进行的上传", meta.get("reqId"));
                return;
            }
            RemoteFile rf = (RemoteFile) rfObj;
            long offset = ((Number) meta.get("offset")).longValue();
            for (int off = 0; off < bytes.length; off += SFTP_WRITE_PIECE) {
                int len = Math.min(SFTP_WRITE_PIECE, bytes.length - off);
                rf.write(offset + off, bytes, off, len);
            }
            JSONObject resp = new JSONObject();
            resp.put("type", "sftp_upload_chunk");
            resp.put("ok", true);
            resp.put("offset", offset);
            resp.put("written", bytes.length);
            Object reqId = meta.get("reqId");
            if (reqId != null) resp.put("reqId", reqId);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
            }
        } catch (Exception e) {
            log.warn("SFTP upload chunk failed", e);
            sendSftpError(wsSession, "sftp_upload_chunk", e.getMessage(), meta.get("reqId"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, org.springframework.web.socket.CloseStatus status) throws Exception {
        AtomicBoolean closed = (AtomicBoolean) wsSession.getAttributes().get("closed");
        if (closed != null) closed.set(true);
        Object shell = wsSession.getAttributes().get("shell");
        if (shell instanceof net.schmizz.sshj.connection.channel.direct.Session.Shell) {
            try { ((net.schmizz.sshj.connection.channel.direct.Session.Shell) shell).close(); } catch (Exception ignored) {}
        }
        Object session = wsSession.getAttributes().get("session");
        if (session instanceof Session) {
            try { ((Session) session).close(); } catch (Exception ignored) {}
        }
        Object uploadRf = wsSession.getAttributes().get("uploadRemoteFile");
        if (uploadRf instanceof RemoteFile) {
            try { ((RemoteFile) uploadRf).close(); } catch (Exception ignored) {}
        }
        Object sftpObj = wsSession.getAttributes().get("sftpClient");
        if (sftpObj instanceof SFTPClient) {
            try { ((SFTPClient) sftpObj).close(); } catch (Exception ignored) {}
        }
        Object ssh = wsSession.getAttributes().get("sshClient");
        if (ssh instanceof SSHClient) {
            try { ((SSHClient) ssh).close(); } catch (Exception ignored) {}
        }
    }

    /** 获取或创建会话级 SFTPClient，避免每次 SFTP 操作都新建/关闭通道 */
    private synchronized SFTPClient getOrCreateSftp(WebSocketSession wsSession, SSHClient ssh) throws IOException {
        Object existing = wsSession.getAttributes().get("sftpClient");
        if (existing instanceof SFTPClient) {
            SFTPClient sftp = (SFTPClient) existing;
            try {
                // 用轻量级的 canonicalize 代替 ls("/") 做存活检测
                sftp.canonicalize(".");
                return sftp;
            } catch (Exception e) {
                try { sftp.close(); } catch (Exception ignored) {}
            }
        }
        SFTPClient sftp = ssh.newSFTPClient();
        wsSession.getAttributes().put("sftpClient", sftp);
        return sftp;
    }

    /** 单次 Session 单次 exec 批量拉取系统信息，减少往返延迟 */
    private static final String SYSINFO_SCRIPT =
        "echo ___SECTION_UPTIME___; uptime 2>/dev/null || true; "
        + "echo ___SECTION_PROC_UPTIME___; cat /proc/uptime 2>/dev/null || true; "
        + "echo ___SECTION_FREE___; free -m 2>/dev/null || true; "
        + "echo ___SECTION_DF___; df -h 2>/dev/null | head -20 || true; "
        + "echo ___SECTION_LOAD___; cat /proc/loadavg 2>/dev/null || true; "
        + "echo ___SECTION_STAT___; cat /proc/stat 2>/dev/null | grep '^cpu ' || true; "
        + "echo ___SECTION_PROC___; (ps -eo rss,pcpu,comm --no-headers 2>/dev/null || ps aux 2>/dev/null) | head -30; "
        + "echo ___SECTION_NET___; cat /proc/net/dev 2>/dev/null || true";

    private void handleSysinfo(WebSocketSession wsSession, JSONObject obj) {
        Object sshObj = wsSession.getAttributes().get("sshClient");
        if (!(sshObj instanceof SSHClient)) return;
        SSHClient ssh = (SSHClient) sshObj;
        executor.execute(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                String raw;
                try (Session s = ssh.startSession()) {
                    raw = execAndRead(s, "sh -c " + quoteSh(SYSINFO_SCRIPT));
                }
                if (raw == null) raw = "";
                Map<String, String> sections = new HashMap<>();
                String[] parts = raw.split("___SECTION_");
                for (int i = 1; i < parts.length; i++) {
                    String seg = parts[i];
                    int idx = seg.indexOf("___");
                    if (idx < 0) continue;
                    String name = seg.substring(0, idx).trim();
                    String content = idx + 3 < seg.length() ? seg.substring(idx + 3).trim() : "";
                    sections.put(name, content);
                }
                String uptimeOut = sections.get("UPTIME");
                String procUptime = sections.get("PROC_UPTIME");
                String memOut = sections.get("FREE");
                String dfOut = sections.get("DF");
                String loadOut = sections.get("LOAD");
                String statOut = sections.get("STAT");
                String procOut = sections.get("PROC");
                String netOut = sections.get("NET");
                data.put("uptime", uptimeOut != null ? uptimeOut : "");
                if (procUptime != null && !procUptime.isEmpty()) {
                    try {
                        double secs = Double.parseDouble(procUptime.split("\\s+")[0]);
                        long totalSec = (long) secs;
                        data.put("uptimeSec", totalSec);
                        data.put("uptimeDays", totalSec / 86400);
                        data.put("uptimeHours", (totalSec % 86400) / 3600);
                        data.put("uptimeMinutes", (totalSec % 3600) / 60);
                    } catch (NumberFormatException ignored) {}
                }
                data.put("memory", memOut != null ? memOut : "");
                data.put("disk", dfOut != null ? dfOut : "");
                data.put("loadavg", loadOut != null ? loadOut : "");
                parseSysinfoStructured(data, uptimeOut, memOut, dfOut, loadOut, statOut);
                parseSysinfoProcesses(data, procOut);
                parseSysinfoNetwork(data, netOut);
                JSONObject resp = new JSONObject();
                resp.put("type", "sysinfo");
                resp.put("data", data);
                synchronized (wsSession) {
                    if (wsSession.isOpen()) {
                        try {
                            wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                        } catch (IllegalStateException sendEx) {
                            String msg = sendEx.getMessage() != null ? sendEx.getMessage() : "";
                            if (msg.contains("BINARY_PARTIAL_WRITING") || msg.contains("TEXT_PARTIAL_WRITING") || msg.contains("InvalidState")) {
                                log.debug("sysinfo send skipped (session busy): {}", msg);
                                return;
                            }
                            throw sendEx;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("sysinfo exec error: {}", e.getMessage());
                try {
                    String errMsg = e.getMessage() != null ? e.getMessage() : "";
                    if (errMsg.contains("BINARY_PARTIAL_WRITING") || errMsg.contains("InvalidState")) {
                        return;
                    }
                    JSONObject err = new JSONObject();
                    err.put("type", "sysinfo");
                    err.put("data", new JSONObject().fluentPut("error", errMsg));
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(err.toJSONString()));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    /** 解析原始命令输出为结构化字段，供前端指标卡展示 */
    private void parseSysinfoStructured(Map<String, Object> data, String uptime, String memory, String disk, String loadavg, String stat) {
        if (loadavg != null && !loadavg.isEmpty()) {
            String[] parts = loadavg.trim().split("\\s+");
            if (parts.length >= 3) {
                try {
                    data.put("load1", Double.parseDouble(parts[0]));
                    data.put("load5", Double.parseDouble(parts[1]));
                    data.put("load15", Double.parseDouble(parts[2]));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (memory != null && !memory.isEmpty()) {
            String[] lines = memory.split("\n");
            for (String line : lines) {
                String t = line.trim();
                if (t.startsWith("Mem:")) {
                    String[] tokens = t.split("\\s+");
                    if (tokens.length >= 3) {
                        try {
                            long total = Long.parseLong(tokens[1]);
                            long used = Long.parseLong(tokens[2]);
                            data.put("memoryTotalMb", total);
                            data.put("memoryUsedMb", used);
                            if (total > 0) {
                                data.put("memoryPercent", Math.round(used * 100.0 / total));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } else if (t.startsWith("Swap:")) {
                    String[] tokens = t.split("\\s+");
                    if (tokens.length >= 3) {
                        try {
                            long total = Long.parseLong(tokens[1]);
                            long used = Long.parseLong(tokens[2]);
                            data.put("swapTotalMb", total);
                            data.put("swapUsedMb", used);
                            if (total > 0) {
                                data.put("swapPercent", Math.round(used * 100.0 / total));
                            } else {
                                data.put("swapPercent", 0);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        if (stat != null && !stat.isEmpty() && stat.startsWith("cpu ")) {
            String[] tokens = stat.trim().split("\\s+");
            if (tokens.length >= 5) {
                try {
                    long user = Long.parseLong(tokens[1]);
                    long nice = Long.parseLong(tokens[2]);
                    long system = Long.parseLong(tokens[3]);
                    long idle = Long.parseLong(tokens[4]);
                    long iowait = tokens.length > 5 ? Long.parseLong(tokens[5]) : 0;
                    long total = user + nice + system + idle + iowait;
                    long idleTotal = idle + iowait;
                    if (total > 0) {
                        data.put("cpuPercent", Math.round((total - idleTotal) * 100.0 / total));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (disk != null && !disk.isEmpty()) {
            String[] lines = disk.split("\n");
            List<Map<String, Object>> disks = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                String[] tokens = line.trim().split("\\s+", 6);
                if (tokens.length >= 4) {
                    try {
                        Map<String, Object> row = new HashMap<>();
                        row.put("mount", tokens.length >= 6 ? tokens[5] : "");
                        row.put("size", tokens.length >= 2 ? tokens[1] : "");
                        row.put("used", tokens.length >= 3 ? tokens[2] : "");
                        row.put("avail", tokens.length >= 4 ? tokens[3] : "");
                        int usePercent = 0;
                        if (tokens.length >= 5) {
                            String usePct = tokens[4].replace("%", "");
                            if (!usePct.isEmpty()) {
                                try {
                                    usePercent = Integer.parseInt(usePct);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        row.put("usePercent", usePercent);
                        disks.add(row);
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (!disks.isEmpty()) {
                data.put("disks", disks);
            }
        }
    }

    /** 解析 ps 输出为 processes: [{ rssMb, cpuPct, comm }]，兼容 ps -eo 与 ps aux */
    private void parseSysinfoProcesses(Map<String, Object> data, String procOut) {
        if (procOut == null || procOut.isEmpty()) return;
        String[] lines = procOut.split("\\n");
        List<Map<String, Object>> list = new ArrayList<>();
        boolean isPsAux = false;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("USER") && t.contains("PID")) {
                isPsAux = true;
                continue;
            }
            if (isPsAux) {
                String[] tokens = t.split("\\s+", 11);
                if (tokens.length >= 10) {
                    try {
                        int rssK = Integer.parseInt(tokens[5]);
                        double cpu = Double.parseDouble(tokens[2]);
                        String comm = tokens.length >= 11 ? tokens[10] : "";
                        Map<String, Object> row = new HashMap<>();
                        row.put("rssMb", rssK / 1024);
                        row.put("cpuPct", Math.round(cpu * 10) / 10.0);
                        row.put("comm", comm);
                        list.add(row);
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                String[] tokens = t.split("\\s+", 3);
                if (tokens.length >= 3) {
                    try {
                        int rssK = Integer.parseInt(tokens[0]);
                        double cpu = Double.parseDouble(tokens[1]);
                        Map<String, Object> row = new HashMap<>();
                        row.put("rssMb", rssK / 1024);
                        row.put("cpuPct", Math.round(cpu * 10) / 10.0);
                        row.put("comm", tokens[2]);
                        list.add(row);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (!list.isEmpty()) data.put("processes", list);
    }

    /** 解析 /proc/net/dev 为 interfaces: [{ name, rxBytes, txBytes }] */
    private void parseSysinfoNetwork(Map<String, Object> data, String netOut) {
        if (netOut == null || netOut.isEmpty()) return;
        String[] lines = netOut.split("\\n");
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String name = line.substring(0, colon).trim();
            if ("lo".equals(name)) continue;
            String rest = line.substring(colon + 1).trim();
            String[] tokens = rest.split("\\s+");
            if (tokens.length >= 10) {
                try {
                    long rx = Long.parseLong(tokens[0]);
                    long tx = Long.parseLong(tokens[8]);
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", name);
                    row.put("rxBytes", rx);
                    row.put("txBytes", tx);
                    list.add(row);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (!list.isEmpty()) data.put("interfaces", list);
    }

    /** Put file attributes (size, mtime, mode, uid, gid, directory, name) into response for local list updates */
    private void putFileAttrs(JSONObject resp, SFTPClient sftp, String path) {
        try {
            FileAttributes a = sftp.stat(path);
            resp.put("size", a.getSize());
            resp.put("mtime", a.getMtime());
            resp.put("mode", a.getMode().getMask());
            resp.put("uid", a.getUID());
            resp.put("gid", a.getGID());
            resp.put("directory", a.getMode().getType() == FileMode.Type.DIRECTORY);
            int last = path.lastIndexOf('/');
            resp.put("name", last >= 0 && last < path.length() - 1 ? path.substring(last + 1) : path);
        } catch (Exception ignored) {}
    }

    private String execAndRead(Session session, String command) throws IOException {
        try (Command cmd = session.exec(command)) {
            InputStream in = cmd.getInputStream();
            byte[] buf = new byte[4096];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }

    /** 安装完成兜底判断：系统中存在 sing-box 可执行文件即视为已安装 */
    private boolean isSingBoxInstalled(SSHClient ssh) {
        try (Session verifySession = ssh.startSession()) {
            String out = execAndRead(verifySession,
                    "command -v sing-box 2>/dev/null || which sing-box 2>/dev/null || echo ''");
            return out != null && !out.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** 规范化 SFTP 路径：空为 "/"，合并连续斜杠，去掉末尾斜杠（根保留为 "/”） */
    private static String normalizeSftpPath(String path) {
        if (path == null || path.isEmpty()) return "/";
        String p = path.trim().replaceAll("/+", "/");
        if (p.isEmpty()) return "/";
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private void handleSftp(WebSocketSession wsSession, JSONObject obj) {
        final String type = obj.getString("type");
        String pathVal = obj.getString("path");
        if (pathVal == null) pathVal = "/";
        final String path = pathVal.trim();
        if (path.contains("..")) {
            sendSftpError(wsSession, type, "路径不允许包含 ..");
            return;
        }
        Object sshObj = wsSession.getAttributes().get("sshClient");
        if (!(sshObj instanceof SSHClient)) {
            sendSftpError(wsSession, type, "SSH 未连接");
            return;
        }
        final SSHClient ssh = (SSHClient) sshObj;
        final JSONObject objFinal = obj;
        final Object reqId = obj.get("_id");
        executor.execute(() -> {
            try {
                SFTPClient sftp = getOrCreateSftp(wsSession, ssh);
                if ("sftp_home".equals(type)) {
                    // 返回当前 SFTP 用户的家目录 (canonicalize ".")
                    String home = sftp.canonicalize(".");
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_home");
                    resp.put("home", home != null ? home : "/");
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_list".equals(type)) {
                    String listPath = normalizeSftpPath(path);
                    List<RemoteResourceInfo> list = sftp.ls(listPath);
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (RemoteResourceInfo r : list) {
                        String name = r.getName();
                        if (name == null || ".".equals(name) || "..".equals(name)) continue;
                        FileAttributes attrs = r.getAttributes();
                        if (attrs == null) continue;
                        FileMode mode = attrs.getMode();
                        if (mode == null) continue;
                        boolean isDir = mode.getType() == FileMode.Type.DIRECTORY || r.isDirectory();
                        String childPath = "/".equals(listPath) ? listPath + name : listPath + "/" + name;
                        Map<String, Object> row = new HashMap<>();
                        row.put("name", name);
                        row.put("path", childPath);
                        row.put("directory", isDir);
                        long size = attrs.getSize();
                        long mtime = attrs.getMtime();
                        row.put("size", size);
                        row.put("mtime", mtime);
                        row.put("mode", mode.getMask());
                        row.put("uid", attrs.getUID());
                        row.put("gid", attrs.getGID());
                        rows.add(row);
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_list");
                    resp.put("path", listPath);
                    resp.put("data", rows);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_download".equals(type)) {
                    // 流式下载：按块从 SFTP 读取并推给前端，不在 Skyway 内存中保留完整文件
                    long size;
                    try {
                        FileAttributes attrs = sftp.stat(path);
                        if (attrs.getMode().getType() == FileMode.Type.DIRECTORY) {
                            sendSftpError(wsSession, type, "不能下载目录", reqId);
                            return;
                        }
                        size = attrs.getSize();
                    } catch (IOException e) {
                        sendSftpError(wsSession, type, e.getMessage() != null ? e.getMessage() : "文件不存在或无法读取", reqId);
                        return;
                    }
                    String name = path.substring(path.lastIndexOf('/') + 1);
                    JSONObject startResp = new JSONObject();
                    startResp.put("type", "sftp_download_start");
                    startResp.put("path", path);
                    startResp.put("name", name);
                    startResp.put("size", size);
                    if (reqId != null) startResp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(startResp.toJSONString()));
                    }
                    final int chunkSize = 256 * 1024;
                    byte[] buf = new byte[chunkSize];
                    long offset = 0;
                    boolean cancelled = false;
                    @SuppressWarnings("unchecked")
                    Set<Object> cancelledSet = (Set<Object>) wsSession.getAttributes().get("downloadCancelledReqIds");
                    try (RemoteFile rf = sftp.open(path);
                         InputStream rfis = rf.new ReadAheadRemoteFileInputStream(8)) {
                        int n;
                        while ((n = rfis.read(buf)) != -1 && wsSession.isOpen()) {
                            if (cancelledSet != null && reqId != null && cancelledSet.contains(reqId)) {
                                cancelled = true;
                                break;
                            }
                            byte[] chunk = n == buf.length ? buf : java.util.Arrays.copyOf(buf, n);
                            String base64 = Base64.getEncoder().encodeToString(chunk);
                            JSONObject chunkResp = new JSONObject();
                            chunkResp.put("type", "sftp_download_chunk");
                            chunkResp.put("offset", offset);
                            chunkResp.put("base64", base64);
                            if (reqId != null) chunkResp.put("reqId", reqId);
                            synchronized (wsSession) {
                                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(chunkResp.toJSONString()));
                            }
                            offset += n;
                        }
                    }
                    if (cancelledSet != null && reqId != null) cancelledSet.remove(reqId);
                    JSONObject endResp = new JSONObject();
                    endResp.put("type", "sftp_download_end");
                    endResp.put("cancelled", cancelled);
                    if (reqId != null) endResp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(endResp.toJSONString()));
                    }
                } else if ("sftp_download_dir".equals(type)) {
                    // 目录打包为 tar 流，复用与 sftp_download 相同的 start/chunk/end 协议
                    FileAttributes attrs;
                    try {
                        attrs = sftp.stat(path);
                        if (attrs.getMode().getType() != FileMode.Type.DIRECTORY) {
                            sendSftpError(wsSession, type, "不是目录", reqId);
                            return;
                        }
                    } catch (IOException e) {
                        sendSftpError(wsSession, type, e.getMessage() != null ? e.getMessage() : "目录不存在或无法读取", reqId);
                        return;
                    }
                    int lastSlash = path.lastIndexOf('/');
                    String parent = lastSlash <= 0 ? "/" : path.substring(0, lastSlash);
                    String dirname = lastSlash < 0 ? path : path.substring(lastSlash + 1);
                    String name = dirname + ".tar";
                    String tarCmd = "tar cf - -C " + quoteSh(parent) + " " + quoteSh(dirname);
                    JSONObject startResp = new JSONObject();
                    startResp.put("type", "sftp_download_start");
                    startResp.put("path", path);
                    startResp.put("name", name);
                    startResp.put("size", -1);
                    if (reqId != null) startResp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(startResp.toJSONString()));
                    }
                    final int chunkSize = 256 * 1024;
                    byte[] buf = new byte[chunkSize];
                    long offset = 0;
                    boolean cancelled = false;
                    @SuppressWarnings("unchecked")
                    Set<Object> cancelledSet = (Set<Object>) wsSession.getAttributes().get("downloadCancelledReqIds");
                    net.schmizz.sshj.connection.channel.direct.Session tarSession = null;
                    Command tarCmdObj = null;
                    try {
                        tarSession = ssh.startSession();
                        tarCmdObj = tarSession.exec(tarCmd);
                        InputStream tarIn = tarCmdObj.getInputStream();
                        int n;
                        while ((n = tarIn.read(buf)) != -1 && wsSession.isOpen()) {
                            if (cancelledSet != null && reqId != null && cancelledSet.contains(reqId)) {
                                cancelled = true;
                                break;
                            }
                            byte[] chunk = n == buf.length ? buf : java.util.Arrays.copyOf(buf, n);
                            String base64 = Base64.getEncoder().encodeToString(chunk);
                            JSONObject chunkResp = new JSONObject();
                            chunkResp.put("type", "sftp_download_chunk");
                            chunkResp.put("offset", offset);
                            chunkResp.put("base64", base64);
                            if (reqId != null) chunkResp.put("reqId", reqId);
                            synchronized (wsSession) {
                                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(chunkResp.toJSONString()));
                            }
                            offset += n;
                        }
                    } finally {
                        if (tarCmdObj != null) try { tarCmdObj.close(); } catch (IOException ignored) {}
                        if (tarSession != null) try { tarSession.close(); } catch (IOException ignored) {}
                    }
                    if (cancelledSet != null && reqId != null) cancelledSet.remove(reqId);
                    JSONObject endResp = new JSONObject();
                    endResp.put("type", "sftp_download_end");
                    endResp.put("cancelled", cancelled);
                    if (reqId != null) endResp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(endResp.toJSONString()));
                    }
                } else if ("sftp_upload".equals(type)) {
                    String base64 = objFinal.getString("base64");
                    if (base64 == null || base64.isEmpty()) {
                        sendSftpError(wsSession, type, "缺少 base64 内容", reqId);
                        return;
                    }
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    String name = objFinal.getString("name");
                    String targetPath;
                    if (name != null && !name.isEmpty()) {
                        targetPath = path.endsWith("/") ? path + name : path + "/" + name;
                    } else {
                        targetPath = path;
                    }
                    if (targetPath.contains("..")) {
                        sendSftpError(wsSession, type, "路径不允许包含 ..", reqId);
                        return;
                    }
                    try (RemoteFile rf = sftp.open(targetPath,
                            EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))) {
                        rf.write(0, bytes, 0, bytes.length);
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_upload");
                    resp.put("path", targetPath);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, targetPath);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_upload_start".equals(type)) {
                    String name = objFinal.getString("name");
                    String targetPath;
                    if (name != null && !name.isEmpty()) {
                        targetPath = path.endsWith("/") ? path + name : path + "/" + name;
                    } else {
                        targetPath = path;
                    }
                    if (targetPath.contains("..")) {
                        sendSftpError(wsSession, type, "路径不允许包含 ..", reqId);
                        return;
                    }
                    // 关闭之前可能残留的上传文件句柄
                    Object oldRf = wsSession.getAttributes().remove("uploadRemoteFile");
                    if (oldRf instanceof RemoteFile) {
                        try { ((RemoteFile) oldRf).close(); } catch (Exception ignored) {}
                    }
                    // 打开文件并保持 handle 在会话中，整个分片上传期间复用
                    RemoteFile rf = sftp.open(targetPath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC));
                    wsSession.getAttributes().put("uploadRemoteFile", rf);
                    wsSession.getAttributes().put("uploadTargetPath", targetPath);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_upload_start");
                    resp.put("ok", true);
                    resp.put("path", targetPath);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_upload_chunk".equals(type)) {
                    String base64 = objFinal.getString("base64");
                    if (base64 == null || base64.isEmpty()) {
                        sendSftpError(wsSession, type, "缺少 base64 内容", reqId);
                        return;
                    }
                    Object rfObj = wsSession.getAttributes().get("uploadRemoteFile");
                    if (!(rfObj instanceof RemoteFile)) {
                        sendSftpError(wsSession, type, "没有正在进行的上传，请先 sftp_upload_start", reqId);
                        return;
                    }
                    RemoteFile rf = (RemoteFile) rfObj;
                    byte[] bytes = Base64.getDecoder().decode(base64);
                    long offset = objFinal.getLongValue("offset");
                    // SSHJ SFTP 单次 write 约 32KB 限制，大块按 32KB 分段写入以兼顾效率与稳定性
                    final int maxWrite = 32 * 1024;
                    for (int off = 0; off < bytes.length; off += maxWrite) {
                        int len = Math.min(maxWrite, bytes.length - off);
                        rf.write(offset + off, bytes, off, len);
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_upload_chunk");
                    resp.put("ok", true);
                    resp.put("offset", offset);
                    resp.put("written", bytes.length);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_upload_end".equals(type)) {
                    // 关闭上传文件句柄
                    Object rfObj = wsSession.getAttributes().remove("uploadRemoteFile");
                    if (rfObj instanceof RemoteFile) {
                        try { ((RemoteFile) rfObj).close(); } catch (Exception ignored) {}
                    }
                    String targetPath = (String) wsSession.getAttributes().remove("uploadTargetPath");
                    if (targetPath == null) targetPath = path;
                    FileAttributes a = sftp.stat(targetPath);
                    String name = targetPath.substring(Math.max(0, targetPath.lastIndexOf('/') + 1));
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_upload");
                    resp.put("ok", true);
                    resp.put("path", targetPath);
                    resp.put("name", name);
                    resp.put("size", a.getSize());
                    resp.put("mtime", a.getMtime());
                    resp.put("mode", a.getMode().getMask());
                    resp.put("uid", a.getUID());
                    resp.put("gid", a.getGID());
                    resp.put("directory", a.getMode().getType() == FileMode.Type.DIRECTORY);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_upload_cancel".equals(type)) {
                    // 取消上传：关闭文件句柄并删除不完整文件
                    Object rfObj = wsSession.getAttributes().remove("uploadRemoteFile");
                    if (rfObj instanceof RemoteFile) {
                        try { ((RemoteFile) rfObj).close(); } catch (Exception ignored) {}
                    }
                    String targetPath = (String) wsSession.getAttributes().remove("uploadTargetPath");
                    if (targetPath != null) {
                        try { sftp.rm(targetPath); } catch (Exception ignored) {}
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_upload_cancel");
                    resp.put("ok", true);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_mkdir".equals(type)) {
                    sftp.mkdirs(path);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_mkdir");
                    resp.put("path", path);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, path);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_touch".equals(type)) {
                    String name = objFinal.getString("name");
                    String targetPath;
                    if (name != null && !name.isEmpty()) {
                        targetPath = path.endsWith("/") ? path + name : path + "/" + name;
                    } else {
                        targetPath = path;
                    }
                    if (targetPath.contains("..")) {
                        sendSftpError(wsSession, type, "路径不允许包含 ..", reqId);
                        return;
                    }
                    try (RemoteFile rf = sftp.open(targetPath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))) {
                        rf.write(0, new byte[0], 0, 0);
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_touch");
                    resp.put("path", targetPath);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, targetPath);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_rename".equals(type)) {
                    String newPath = objFinal.getString("newPath");
                    if (newPath == null || newPath.trim().isEmpty() || newPath.contains("..")) {
                        sendSftpError(wsSession, type, "newPath 无效", reqId);
                        return;
                    }
                    String newPathTrimmed = newPath.trim();
                    sftp.rename(path, newPathTrimmed);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_rename");
                    resp.put("path", path);
                    resp.put("newPath", newPathTrimmed);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, newPathTrimmed);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_delete".equals(type)) {
                    deleteRecursive(sftp, path);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_delete");
                    resp.put("path", path);
                    resp.put("ok", true);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_read_text".equals(type)) {
                    int maxBytes = 512 * 1024;
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try (RemoteFile rf = sftp.open(path)) {
                        InputStream rfis = rf.new ReadAheadRemoteFileInputStream(16);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = rfis.read(buf)) != -1) {
                            if (bout.size() + n > maxBytes) {
                                rfis.close();
                                throw new IOException("文件过大，仅支持 512KB 以内文本");
                            }
                            bout.write(buf, 0, n);
                        }
                        rfis.close();
                    }
                    String text = new String(bout.toByteArray(), StandardCharsets.UTF_8);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_read_text");
                    resp.put("path", path);
                    resp.put("content", text);
                    resp.put("ok", true);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_write_text".equals(type)) {
                    String content = objFinal.getString("content");
                    if (content == null) content = "";
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    try (RemoteFile rf = sftp.open(path, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))) {
                        rf.write(0, bytes, 0, bytes.length);
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_write_text");
                    resp.put("path", path);
                    resp.put("ok", true);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_copy".equals(type)) {
                    String dest = objFinal.getString("dest");
                    if (dest == null || dest.contains("..")) {
                        sendSftpError(wsSession, type, "dest 无效", reqId);
                        return;
                    }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try (RemoteFile rf = sftp.open(path)) {
                        InputStream rfis = rf.new ReadAheadRemoteFileInputStream(16);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = rfis.read(buf)) != -1) bout.write(buf, 0, n);
                        rfis.close();
                    }
                    try (RemoteFile rf = sftp.open(dest, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC))) {
                        rf.write(0, bout.toByteArray(), 0, bout.size());
                    }
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_copy");
                    resp.put("path", path);
                    resp.put("dest", dest);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, dest);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_move".equals(type)) {
                    String dest = objFinal.getString("dest");
                    if (dest == null || dest.contains("..")) {
                        sendSftpError(wsSession, type, "dest 无效", reqId);
                        return;
                    }
                    sftp.rename(path, dest);
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_move");
                    resp.put("path", path);
                    resp.put("dest", dest);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, dest);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_chmod".equals(type)) {
                    String modeStr = objFinal.getString("mode");
                    if (modeStr == null || modeStr.isEmpty()) {
                        sendSftpError(wsSession, type, "mode 无效", reqId);
                        return;
                    }
                    int mode = Integer.parseInt(modeStr, 8);
                    sftp.setattr(path, new FileAttributes.Builder().withPermissions(mode).build());
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_chmod");
                    resp.put("path", path);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, path);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else if ("sftp_chown".equals(type)) {
                    Integer uid = objFinal.getInteger("uid");
                    Integer gid = objFinal.getInteger("gid");
                    if (uid == null || gid == null) {
                        sendSftpError(wsSession, type, "uid 与 gid 均需提供", reqId);
                        return;
                    }
                    FileAttributes attrs = sftp.stat(path);
                    FileAttributes.Builder b = new FileAttributes.Builder();
                    b.withSize(attrs.getSize()).withAtimeMtime(attrs.getAtime(), attrs.getMtime())
                        .withPermissions(attrs.getMode().getMask()).withUIDGID(uid, gid);
                    sftp.setattr(path, b.build());
                    JSONObject resp = new JSONObject();
                    resp.put("type", "sftp_chown");
                    resp.put("path", path);
                    resp.put("ok", true);
                    putFileAttrs(resp, sftp, path);
                    if (reqId != null) resp.put("reqId", reqId);
                    synchronized (wsSession) {
                        if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(resp.toJSONString()));
                    }
                } else {
                    sendSftpError(wsSession, type, "未知类型", reqId);
                }
            } catch (Exception e) {
                log.debug("SFTP error: {}", e.getMessage());
                // 清除可能已失效的 SFTP 缓存
                wsSession.getAttributes().remove("sftpClient");
                sendSftpError(wsSession, type, e.getMessage(), reqId);
            }
        });
    }

    /** 递归删除文件或目录 */
    private void deleteRecursive(SFTPClient sftp, String path) throws IOException {
        FileAttributes attrs = sftp.stat(path);
        if (attrs.getMode().getType() == FileMode.Type.DIRECTORY) {
            List<RemoteResourceInfo> list = sftp.ls(path);
            for (RemoteResourceInfo r : list) {
                String name = r.getName();
                if (".".equals(name) || "..".equals(name)) continue;
                String childPath = path.endsWith("/") ? path + name : path + "/" + name;
                deleteRecursive(sftp, childPath);
            }
            sftp.rmdir(path);
        } else {
            sftp.rm(path);
        }
    }

    private void sendSftpError(WebSocketSession wsSession, String type, String message) {
        sendSftpError(wsSession, type, message, null);
    }

    private void sendSftpError(WebSocketSession wsSession, String type, String message, Object reqId) {
        try {
            JSONObject err = new JSONObject();
            err.put("type", type);
            err.put("error", message);
            if (reqId != null) err.put("reqId", reqId);
            synchronized (wsSession) {
                if (wsSession.isOpen()) wsSession.sendMessage(new TextMessage(err.toJSONString()));
            }
        } catch (Exception ignored) {}
    }

    private void sendErrorAndClose(WebSocketSession session, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("type", "error");
            err.put("message", message);
            session.sendMessage(new TextMessage(err.toJSONString()));
            session.close();
        } catch (Exception e) {
            log.debug("sendErrorAndClose: {}", e.getMessage());
        }
    }

    private void closeAndLog(WebSocketSession session, String reason) {
        log.warn("SSH WebSocket closed: {}", reason);
        try { session.close(); } catch (Exception ignored) {}
    }
}
