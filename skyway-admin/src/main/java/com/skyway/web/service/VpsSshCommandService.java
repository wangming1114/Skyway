package com.skyway.web.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.domain.ProxyNodeDomainWhitelist;
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
    private static final String SPEED_SCRIPT_PATH = "/root/singbox_speed.sh";
    private static final String TC_MANAGER_SCRIPT_PATH = "/root/tc_manager.sh";
    private static final String TC_PORT_RULES_CONF = "/etc/tc_ports_rules.conf";
    private static final String SOCKS_RELAY_TAG_PREFIX = "SOCKS-";
    private static final String DOMAIN_EGRESS_TAG_PREFIX = "skyway-domain-egress-";
    private static final String DOMAIN_BLOCK_TAG_PREFIX = "skyway-domain-block-";
    private static final String RELAY_TAG_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern SPEED_LINE = Pattern.compile("^\\s*(\\d{1,5})\\s*\\|\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\|\\s*([0-9]+(?:\\.[0-9]+)?)\\s*$");
    private static final Pattern TC_RULE_LINE = Pattern.compile("^\\s*(\\d{1,5})\\s*:\\s*(\\d+)\\s*:\\s*(\\d+)\\s*$");
    private static final String PORT_SCAN_SCRIPT = ""
            + "socket_ok=1\n"
            + "for file in /proc/net/tcp /proc/net/tcp6 /proc/net/udp /proc/net/udp6; do\n"
            + "  if [ ! -r \"$file\" ]; then\n"
            + "    socket_ok=0\n"
            + "  elif ! awk 'NR > 1 { split($2, a, \":\"); print \"SOCKET_HEX=\" a[2] }' \"$file\"; then\n"
            + "    socket_ok=0\n"
            + "  fi\n"
            + "done\n"
            + "[ \"$socket_ok\" -eq 1 ] && echo SOCKETS_OK\n"
            + "conf_dir=\"/etc/sing-box/conf\"\n"
            + "if [ ! -e \"$conf_dir\" ]; then\n"
            + "  echo CONFIG_OK\n"
            + "elif [ -d \"$conf_dir\" ] && [ -r \"$conf_dir\" ] && [ -x \"$conf_dir\" ]; then\n"
            + "  config_ok=1\n"
            + "  for file in \"$conf_dir\"/*.json \"$conf_dir\"/*.json.disabled; do\n"
            + "    [ -e \"$file\" ] || continue\n"
            + "    if [ ! -r \"$file\" ]; then config_ok=0; continue; fi\n"
            + "    matches=$(grep -hoE '\"listen_port\"[[:space:]]*:[[:space:]]*[0-9]+' \"$file\" 2>/dev/null)\n"
            + "    grep_status=$?\n"
            + "    if [ \"$grep_status\" -gt 1 ]; then config_ok=0; else printf '%s\\n' \"$matches\" | grep -oE '[0-9]+$' | sed 's/^/CONFIG_PORT=/'; fi\n"
            + "  done\n"
            + "  [ \"$config_ok\" -eq 1 ] && echo CONFIG_OK\n"
            + "fi\n"
            + "if command -v docker >/dev/null 2>&1; then\n"
            + "  docker_ok=1\n"
            + "  ids=$(docker ps -q 2>/dev/null) || docker_ok=0\n"
            + "  if [ \"$docker_ok\" -eq 1 ]; then\n"
            + "    for id in $ids; do\n"
            + "      lines=$(docker port \"$id\" 2>/dev/null) || docker_ok=0\n"
            + "      printf '%s\\n' \"$lines\" | sed -n 's/.*:\\([0-9][0-9]*\\)$/DOCKER_PORT=\\1/p'\n"
            + "    done\n"
            + "  fi\n"
            + "  [ \"$docker_ok\" -eq 1 ] && echo DOCKER_OK\n"
            + "else\n"
            + "  echo DOCKER_OK\n"
            + "fi\n"
            + "if [ -r /proc/sys/net/ipv4/ip_local_port_range ]; then\n"
            + "  if awk 'NF == 2 { print \"EPHEMERAL=\" $1 \"-\" $2; ok=1 } END { exit ok ? 0 : 1 }' /proc/sys/net/ipv4/ip_local_port_range; then echo EPHEMERAL_OK; fi\n"
            + "fi\n";
    private static final String TC_MANAGER_SCRIPT = "#!/bin/bash\n"
            + "CONF_FILE=\"/etc/tc_ports_rules.conf\"\n"
            + "GLOBAL_CONF=\"/etc/tc_global_rules.conf\"\n"
            + "IFB_DEV=\"ifb0\"\n"
            + "[ ! -f \"$CONF_FILE\" ] && touch \"$CONF_FILE\"\n"
            + "[ ! -f \"$GLOBAL_CONF\" ] && echo \"OFF:0:0\" > \"$GLOBAL_CONF\"\n"
            + "get_interfaces() {\n"
            + "  ip -o link show | awk -F': ' '{print $2}' | awk -F'@' '{print $1}' | grep -vE '^(lo|docker.*|br-.*|veth.*|ifb.*)$'\n"
            + "}\n"
            + "stop_limits() {\n"
            + "  local interfaces=$(get_interfaces)\n"
            + "  for IFACE in $interfaces; do\n"
            + "    tc qdisc del dev \"$IFACE\" root 2>/dev/null\n"
            + "    tc qdisc del dev \"$IFACE\" ingress 2>/dev/null\n"
            + "  done\n"
            + "  tc qdisc del dev $IFB_DEV root 2>/dev/null\n"
            + "  ip link set dev $IFB_DEV down 2>/dev/null\n"
            + "}\n"
            + "apply_limits() {\n"
            + "  local interfaces=$(get_interfaces)\n"
            + "  if [ -z \"$interfaces\" ]; then echo \"[错误] 未检测到符合条件的物理网卡。\"; return 1; fi\n"
            + "  local global_status=\"OFF\" g_dn=\"0\" g_up=\"0\"\n"
            + "  IFS=':' read -r global_status g_dn g_up < \"$GLOBAL_CONF\"\n"
            + "  if [ \"$global_status\" = \"ON\" ]; then\n"
            + "    echo \"[提示] 全局限速模式已开启！正在覆盖挂起所有端口规则...\"\n"
            + "    stop_limits >/dev/null 2>&1\n"
            + "    modprobe ifb numifbs=1 2>/dev/null\n"
            + "    ip link set dev $IFB_DEV up\n"
            + "    tc qdisc add dev $IFB_DEV root handle 1: htb default 10\n"
            + "    tc class add dev $IFB_DEV parent 1: classid 1:10 htb rate \"${g_dn}mbit\"\n"
            + "    for IFACE in $interfaces; do\n"
            + "      echo \" -> 正在全局限制物理网卡: $IFACE\"\n"
            + "      tc qdisc add dev \"$IFACE\" root handle 1: htb default 10\n"
            + "      tc class add dev \"$IFACE\" parent 1: classid 1:10 htb rate \"${g_up}mbit\"\n"
            + "      tc qdisc add dev \"$IFACE\" handle ffff: ingress\n"
            + "      tc filter add dev \"$IFACE\" parent ffff: protocol ip u32 match u32 0 0 action mirred egress redirect dev $IFB_DEV\n"
            + "      tc filter add dev \"$IFACE\" parent ffff: protocol ipv6 u32 match u32 0 0 action mirred egress redirect dev $IFB_DEV\n"
            + "    done\n"
            + "    echo \"[成功] 全局限速 (下载: ${g_dn}Mbps | 上传: ${g_up}Mbps) 已强制生效！\"\n"
            + "    return 0\n"
            + "  fi\n"
            + "  if [ ! -s \"$CONF_FILE\" ]; then\n"
            + "    echo \"[提示] 配置文件中无任何端口规则，正在卸载底层限速...\"\n"
            + "    stop_limits >/dev/null 2>&1\n"
            + "    echo \"[成功] 限速已全部取消，恢复默认网络状态。\"\n"
            + "    return 0\n"
            + "  fi\n"
            + "  echo \"正在清理旧规则并应用端口配置...\"\n"
            + "  stop_limits >/dev/null 2>&1\n"
            + "  modprobe ifb numifbs=1 2>/dev/null\n"
            + "  ip link set dev $IFB_DEV up\n"
            + "  tc qdisc add dev $IFB_DEV root handle 1: htb default 20\n"
            + "  tc class add dev $IFB_DEV parent 1: classid 1:20 htb rate 10gbit\n"
            + "  for IFACE in $interfaces; do\n"
            + "    echo \" -> 正在挂载物理网卡: $IFACE\"\n"
            + "    tc qdisc add dev \"$IFACE\" root handle 1: htb default 20\n"
            + "    tc class add dev \"$IFACE\" parent 1: classid 1:20 htb rate 10gbit\n"
            + "    tc qdisc add dev \"$IFACE\" handle ffff: ingress\n"
            + "    local index=100\n"
            + "    while IFS=':' read -r port dn_rate up_rate || [ -n \"$port\" ]; do\n"
            + "      [[ -z \"$port\" || \"$port\" == \\#* ]] && continue\n"
            + "      local class_id=\"1:${index}\"\n"
            + "      tc class add dev \"$IFACE\" parent 1: classid \"$class_id\" htb rate \"${up_rate}mbit\"\n"
            + "      tc filter add dev \"$IFACE\" protocol ip parent 1:0 prio 1 u32 match ip sport \"$port\" 0xffff flowid \"$class_id\"\n"
            + "      tc filter add dev \"$IFACE\" protocol ipv6 parent 1:0 prio 1 u32 match ip6 sport \"$port\" 0xffff flowid \"$class_id\"\n"
            + "      tc class add dev $IFB_DEV parent 1: classid \"$class_id\" htb rate \"${dn_rate}mbit\"\n"
            + "      tc filter add dev \"$IFACE\" parent ffff: protocol ip prio 1 u32 match ip dport \"$port\" 0xffff action mirred egress redirect dev $IFB_DEV\n"
            + "      tc filter add dev \"$IFACE\" parent ffff: protocol ipv6 prio 1 u32 match ip6 dport \"$port\" 0xffff action mirred egress redirect dev $IFB_DEV\n"
            + "      tc filter add dev $IFB_DEV protocol ip parent 1:0 prio 1 u32 match ip dport \"$port\" 0xffff flowid \"$class_id\"\n"
            + "      tc filter add dev $IFB_DEV protocol ipv6 parent 1:0 prio 1 u32 match ip6 dport \"$port\" 0xffff flowid \"$class_id\"\n"
            + "      echo \"    └─ 端口 ${port} (下行: ${dn_rate}Mbps | 上行: ${up_rate}Mbps)\"\n"
            + "      index=$((index + 1))\n"
            + "    done < \"$CONF_FILE\"\n"
            + "  done\n"
            + "  echo \"[成功] 独立端口限速规则已全部生效！\"\n"
            + "}\n"
            + "menu_list_rules() { cat \"$CONF_FILE\"; read -p \"按回车键继续...\"; }\n"
            + "while true; do\n"
            + "  read -p \"请选择操作 [0-7]: \" choice\n"
            + "  case $choice in\n"
            + "    3) menu_list_rules ;;\n"
            + "    5) apply_limits; read -p \"按回车键继续...\" ;;\n"
            + "    6) stop_limits; echo \"[成功] 所有限速规则已清除，恢复无限制的默认网络状态。\"; read -p \"按回车键继续...\" ;;\n"
            + "    0) exit 0 ;;\n"
            + "    *) echo \"无效选项\" ;;\n"
            + "  esac\n"
            + "done\n";
    private static final String SINGBOX_SPEED_SCRIPT = "#!/bin/bash\n"
            + "\n"
            + "# 捕获 Ctrl+C 信号，优雅退出并恢复光标\n"
            + "trap 'tput cnorm; echo -e \"\\n🛑 监控已停止。\"; exit 0' SIGINT\n"
            + "\n"
            + "if [[ $EUID -ne 0 ]]; then\n"
            + "   echo \"❌ 错误: 需要 root 权限，请使用 sudo $0 运行。\"\n"
            + "   exit 1\n"
            + "fi\n"
            + "\n"
            + "CONF_DIR=\"/etc/sing-box/conf\"\n"
            + "\n"
            + "# 隐藏光标\n"
            + "tput civis\n"
            + "clear\n"
            + "\n"
            + "# 获取底层数据的函数\n"
            + "get_ss_data() {\n"
            + "    ss -itnp 2>/dev/null | awk '\n"
            + "    /\"sing-box\"/ {\n"
            + "        local_addr = $4\n"
            + "        peer_addr = $5\n"
            + "        \n"
            + "        getline \n"
            + "        acked=0; recv=0\n"
            + "        n = split($0, arr, \" \")\n"
            + "        for(i=1; i<=n; i++) {\n"
            + "            if (arr[i] ~ /^bytes_acked:/) { split(arr[i], kv, \":\"); acked = kv[2] }\n"
            + "            if (arr[i] ~ /^bytes_received:/) { split(arr[i], kv, \":\"); recv = kv[2] }\n"
            + "        }\n"
            + "        if (acked != \"\" || recv != \"\") {\n"
            + "            print local_addr, peer_addr, acked+0, recv+0\n"
            + "        }\n"
            + "    }'\n"
            + "}\n"
            + "\n"
            + "# 全局保存上一秒数据的字典\n"
            + "declare -A old_acked\n"
            + "declare -A old_recv\n"
            + "\n"
            + "# 初始化基准数据\n"
            + "while read -r local_addr peer_addr acked recv; do\n"
            + "    conn=\"${local_addr}->${peer_addr}\"\n"
            + "    old_acked[\"$conn\"]=$acked\n"
            + "    old_recv[\"$conn\"]=$recv\n"
            + "done < <(get_ss_data)\n"
            + "\n"
            + "while true; do\n"
            + "    sleep 1\n"
            + "    \n"
            + "    unset allowed_ports new_old_acked new_old_recv port_up port_down port_total output_lines\n"
            + "    declare -A allowed_ports\n"
            + "    declare -A new_old_acked\n"
            + "    declare -A new_old_recv\n"
            + "    declare -A port_up\n"
            + "    declare -A port_down\n"
            + "    declare -A port_total\n"
            + "    declare -a output_lines\n"
            + "    \n"
            + "    # 动态获取配置目录下的有效端口\n"
            + "    shopt -s nullglob\n"
            + "    for conf in \"$CONF_DIR\"/*.json; do\n"
            + "        port=$(grep -hoP '\"listen_port\":\\s*\\K\\d+' \"$conf\" 2>/dev/null)\n"
            + "        if [[ -n \"$port\" ]]; then\n"
            + "            allowed_ports[\"$port\"]=1\n"
            + "            port_up[\"$port\"]=0\n"
            + "            port_down[\"$port\"]=0\n"
            + "            port_total[\"$port\"]=0\n"
            + "        fi\n"
            + "    done\n"
            + "    shopt -u nullglob\n"
            + "    \n"
            + "    while read -r local_addr peer_addr acked recv; do\n"
            + "        conn=\"${local_addr}->${peer_addr}\"\n"
            + "        new_old_acked[\"$conn\"]=$acked\n"
            + "        new_old_recv[\"$conn\"]=$recv\n"
            + "        \n"
            + "        l_port=\"${local_addr##*:}\"\n"
            + "        l_port=\"${l_port/\\]/}\"\n"
            + "        \n"
            + "        if [[ -z \"${allowed_ports[$l_port]}\" ]]; then\n"
            + "            continue\n"
            + "        fi\n"
            + "\n"
            + "        o_ack=${old_acked[\"$conn\"]}\n"
            + "        o_recv=${old_recv[\"$conn\"]}\n"
            + "        \n"
            + "        if [[ -z \"$o_ack\" ]]; then o_ack=$acked; fi\n"
            + "        if [[ -z \"$o_recv\" ]]; then o_recv=$recv; fi\n"
            + "        \n"
            + "        up_bytes=$(( acked - o_ack ))\n"
            + "        down_bytes=$(( recv - o_recv ))\n"
            + "        \n"
            + "        port_up[\"$l_port\"]=$(( ${port_up[\"$l_port\"]:-0} + up_bytes ))\n"
            + "        port_down[\"$l_port\"]=$(( ${port_down[\"$l_port\"]:-0} + down_bytes ))\n"
            + "        port_total[\"$l_port\"]=$(( ${port_total[\"$l_port\"]:-0} + up_bytes + down_bytes ))\n"
            + "        \n"
            + "    done < <(get_ss_data)\n"
            + "    \n"
            + "    # 刷新旧数据字典\n"
            + "    unset old_acked old_recv\n"
            + "    declare -A old_acked old_recv\n"
            + "    for k in \"${!new_old_acked[@]}\"; do\n"
            + "        old_acked[\"$k\"]=\"${new_old_acked[$k]}\"\n"
            + "        old_recv[\"$k\"]=\"${new_old_recv[$k]}\"\n"
            + "    done\n"
            + "    \n"
            + "    for port in \"${!allowed_ports[@]}\"; do\n"
            + "        total=${port_total[\"$port\"]:-0}\n"
            + "        up_val=${port_up[\"$port\"]:-0}\n"
            + "        down_val=${port_down[\"$port\"]:-0}\n"
            + "        \n"
            + "        # 核心修改：将字节转换为 MB (除以 1024*1024 = 1048576)\n"
            + "        up_mb=$(awk \"BEGIN {printf \\\"%.2f\\\", $up_val / 1048576}\")\n"
            + "        down_mb=$(awk \"BEGIN {printf \\\"%.2f\\\", $down_val / 1048576}\")\n"
            + "        \n"
            + "        line=$(printf \"%-12s | %-15s | %-15s\" \"$port\" \"$up_mb\" \"$down_mb\")\n"
            + "        sort_key=$(printf \"%012d\" \"$total\")\n"
            + "        output_lines+=(\"${sort_key}|${line}\")\n"
            + "    done\n"
            + "    \n"
            + "    # 动态获取当前终端窗口的实际高度 (如果获取失败默认24行)\n"
            + "    term_lines=$(tput lines 2>/dev/null || echo 24)\n"
            + "    max_display=$(( term_lines - 6 ))\n"
            + "    if [ \"$max_display\" -lt 5 ]; then max_display=5; fi\n"
            + "\n"
            + "    # 表头单位更新为 MB/s\n"
            + "    output_buffer=\"\\033[2J\\033[H=================================================\\n\"\n"
            + "    output_buffer+=\" 🚀 节点端口实时网速 (按总速度倒序 | 空闲显示0.00)\\n\"\n"
            + "    output_buffer+=\"=================================================\\n\"\n"
            + "    output_buffer+=$(printf \"%-12s | %-15s | %-15s\\n\" \"端口 (Port)\" \"上传 (MB/s)\" \"下载 (MB/s)\")\n"
            + "    output_buffer+=\"\\n-------------------------------------------------\\n\"\n"
            + "    \n"
            + "    if [ ${#allowed_ports[@]} -eq 0 ] 2>/dev/null; then\n"
            + "        output_buffer+=\"⚠️  未在 $CONF_DIR 找到有效的 .json 配置或监听端口。\\n\"\n"
            + "    elif [ ${#output_lines[@]} -gt 0 ]; then\n"
            + "        sorted_lines=$(printf \"%s\\n\" \"${output_lines[@]}\" | sort -r -t'|' -k1,1 | cut -d'|' -f2- | head -n \"$max_display\")\n"
            + "        output_buffer+=\"$sorted_lines\\n\"\n"
            + "    else\n"
            + "        output_buffer+=\"获取端口状态中... \\n\"\n"
            + "    fi\n"
            + "    \n"
            + "    echo -ne \"$output_buffer\"\n"
            + "done\n";

    @Autowired
    private IVpsInstanceService vpsInstanceService;

    public static final class Socks5RelayConfig {
        private final String server;
        private final int serverPort;
        private final String username;
        private final String password;

        public Socks5RelayConfig(String server, int serverPort, String username, String password) {
            if (StringUtils.isEmpty(server)) {
                throw new IllegalArgumentException("SOCKS5 服务器不能为空");
            }
            if (serverPort < 1 || serverPort > 65535) {
                throw new IllegalArgumentException("SOCKS5 端口范围为 1-65535");
            }
            if (StringUtils.isEmpty(username)) {
                throw new IllegalArgumentException("SOCKS5 用户名不能为空");
            }
            if (StringUtils.isEmpty(password)) {
                throw new IllegalArgumentException("SOCKS5 密码不能为空");
            }
            this.server = server.trim();
            this.serverPort = serverPort;
            this.username = username.trim();
            this.password = password.trim();
        }

        public String getServer() {
            return server;
        }

        public int getServerPort() {
            return serverPort;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public JSONObject toConfigJson() {
            JSONObject relay = new JSONObject();
            relay.put("type", "socks5");
            relay.put("server", server);
            relay.put("serverPort", serverPort);
            relay.put("username", username);
            relay.put("password", password);
            return relay;
        }
    }

    public static Socks5RelayConfig parseSocks5RelayText(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        String[] parts = text.trim().split(":", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("SOCKS5 中转格式应为 host:port:username:password");
        }
        int port;
        try {
            port = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SOCKS5 端口无效");
        }
        return new Socks5RelayConfig(parts[0], port, parts[2], parts[3]);
    }

    public static String applySocks5RelayToSingBoxConfig(String configJson, Socks5RelayConfig relay, String socksTag) {
        if (StringUtils.isEmpty(configJson)) {
            throw new IllegalArgumentException("sing-box 配置不能为空");
        }
        if (relay == null) {
            return configJson;
        }
        if (StringUtils.isEmpty(socksTag)) {
            throw new IllegalArgumentException("SOCKS5 outbound tag 不能为空");
        }
        JSONObject root = JSON.parseObject(configJson);
        JSONArray inbounds = root.getJSONArray("inbounds");
        if (inbounds == null || inbounds.isEmpty()) {
            throw new IllegalArgumentException("sing-box 配置缺少 inbounds");
        }
        JSONObject firstInbound = inbounds.getJSONObject(0);
        String inboundTag = firstInbound != null ? firstInbound.getString("tag") : null;
        if (StringUtils.isEmpty(inboundTag)) {
            throw new IllegalArgumentException("sing-box inbound tag 不能为空");
        }
        JSONArray outbounds = root.getJSONArray("outbounds");
        if (outbounds == null || outbounds.isEmpty()) {
            throw new IllegalArgumentException("sing-box 配置缺少 outbounds");
        }

        JSONObject socksOutbound = new JSONObject();
        socksOutbound.put("tag", socksTag);
        socksOutbound.put("type", "socks");
        socksOutbound.put("server", relay.getServer());
        socksOutbound.put("server_port", relay.getServerPort());
        socksOutbound.put("version", "5");
        socksOutbound.put("username", relay.getUsername());
        socksOutbound.put("password", relay.getPassword());
        outbounds.set(0, socksOutbound);

        refreshManagedDomainEgress(outbounds, socksOutbound);
        JSONObject route = root.getJSONObject("route");
        if (route == null) route = new JSONObject();
        JSONArray rules = route.getJSONArray("rules");
        if (rules == null) rules = new JSONArray();
        removeRelayRouteRules(rules, Collections.singleton(socksTag));
        JSONObject rule = new JSONObject();
        rule.put("inbound", inboundTag);
        rule.put("outbound", socksTag);
        rules.add(managedDomainRuleCount(rules, inboundTag), rule);
        route.put("rules", rules);
        root.put("route", route);
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    public static String upsertSocks5RelayToSingBoxConfig(String configJson, Socks5RelayConfig relay, String socksTagIfNew) {
        if (StringUtils.isEmpty(configJson)) {
            throw new IllegalArgumentException("sing-box 配置不能为空");
        }
        if (relay == null) {
            return configJson;
        }
        JSONObject root = JSON.parseObject(configJson);
        JSONArray outbounds = root.getJSONArray("outbounds");
        if (outbounds == null || outbounds.isEmpty()) {
            return applySocks5RelayToSingBoxConfig(configJson, relay, socksTagIfNew);
        }

        String existingSocksTag = null;
        JSONObject route = root.getJSONObject("route");
        if (route != null) {
            JSONArray rules = route.getJSONArray("rules");
            if (rules != null && !rules.isEmpty()) {
                JSONObject firstRule = rules.getJSONObject(0);
                if (firstRule != null) {
                    existingSocksTag = firstRule.getString("outbound");
                }
            }
        }

        JSONObject socksOutbound = null;
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            if (outbound == null) continue;
            String tag = outbound.getString("tag");
            boolean routeMatched = StringUtils.isNotEmpty(existingSocksTag) && existingSocksTag.equals(tag);
            boolean typeMatched = "socks".equals(outbound.getString("type"));
            if (routeMatched || typeMatched) {
                socksOutbound = outbound;
                break;
            }
        }
        if (socksOutbound == null) {
            return applySocks5RelayToSingBoxConfig(configJson, relay, socksTagIfNew);
        }

        socksOutbound.put("type", "socks");
        socksOutbound.put("server", relay.getServer());
        socksOutbound.put("server_port", relay.getServerPort());
        socksOutbound.put("version", "5");
        socksOutbound.put("username", relay.getUsername());
        socksOutbound.put("password", relay.getPassword());
        alignSocksRelayRouteInbound(root, existingSocksTag, socksOutbound.getString("tag"));
        refreshManagedDomainEgress(outbounds, socksOutbound);
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static void alignSocksRelayRouteInbound(JSONObject root, String... socksTags) {
        JSONArray inbounds = root != null ? root.getJSONArray("inbounds") : null;
        if (inbounds == null || inbounds.isEmpty()) {
            return;
        }
        JSONObject firstInbound = inbounds.getJSONObject(0);
        String inboundTag = firstInbound != null ? firstInbound.getString("tag") : null;
        if (StringUtils.isEmpty(inboundTag)) {
            return;
        }

        List<String> outboundTags = new ArrayList<>();
        if (socksTags != null) {
            for (String socksTag : socksTags) {
                addIfNotEmpty(outboundTags, socksTag);
            }
        }
        if (outboundTags.isEmpty()) {
            return;
        }

        JSONObject route = root.getJSONObject("route");
        JSONArray rules = route != null ? route.getJSONArray("rules") : null;
        if (rules == null) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null) {
                continue;
            }
            Object outboundValue = rule.get("outbound");
            if (containsString(outboundTags, outboundValue)) {
                rule.put("inbound", inboundTag);
            }
        }
    }

    public static String updateSingBoxListenPortAndInboundName(String configJson, String oldInboundTag, String newInboundTag, Integer newPort) {
        if (StringUtils.isEmpty(configJson)) {
            throw new IllegalArgumentException("sing-box 配置不能为空");
        }
        if (newPort == null || newPort < 1 || newPort > 65535) {
            throw new IllegalArgumentException("端口范围为 1-65535");
        }
        JSONObject root = JSON.parseObject(configJson);
        JSONArray inbounds = root.getJSONArray("inbounds");
        if (inbounds == null || inbounds.isEmpty()) {
            throw new IllegalArgumentException("sing-box 配置缺少 inbounds");
        }

        String effectiveOldTag = oldInboundTag;
        String effectiveNewTag = StringUtils.isNotEmpty(newInboundTag) ? newInboundTag : oldInboundTag;
        String matchedOldTag = null;
        Integer matchedOldPort = null;
        boolean matched = false;
        for (int i = 0; i < inbounds.size(); i++) {
            JSONObject inbound = inbounds.getJSONObject(i);
            if (inbound == null) {
                continue;
            }
            String tag = inbound.getString("tag");
            boolean shouldPatch = StringUtils.isEmpty(oldInboundTag) || oldInboundTag.equals(tag);
            if (!matched && !shouldPatch && i == 0) {
                shouldPatch = true;
            }
            if (shouldPatch) {
                if (StringUtils.isEmpty(effectiveOldTag)) {
                    effectiveOldTag = tag;
                }
                matchedOldTag = tag;
                matchedOldPort = inbound.getInteger("listen_port");
                inbound.put("listen_port", newPort);
                if (StringUtils.isNotEmpty(effectiveNewTag)) {
                    inbound.put("tag", effectiveNewTag);
                }
                matched = true;
            }
        }
        if (!matched) {
            throw new IllegalArgumentException("sing-box 配置未找到可更新的 inbound");
        }

        if (StringUtils.isNotEmpty(effectiveOldTag) && StringUtils.isNotEmpty(effectiveNewTag) && !effectiveOldTag.equals(effectiveNewTag)) {
            List<String> oldRouteInboundTags = new ArrayList<>();
            addIfNotEmpty(oldRouteInboundTags, effectiveOldTag);
            addIfNotEmpty(oldRouteInboundTags, matchedOldTag);
            if (matchedOldPort != null) {
                addIfNotEmpty(oldRouteInboundTags, "VLESS-REALITY-" + matchedOldPort + ".json");
            }
            JSONObject route = root.getJSONObject("route");
            JSONArray rules = route != null ? route.getJSONArray("rules") : null;
            if (rules != null) {
                for (int i = 0; i < rules.size(); i++) {
                    JSONObject rule = rules.getJSONObject(i);
                    if (rule == null || !rule.containsKey("inbound")) {
                        continue;
                    }
                    Object inboundValue = rule.get("inbound");
                    if (inboundValue instanceof String && containsString(oldRouteInboundTags, inboundValue)) {
                        rule.put("inbound", effectiveNewTag);
                    } else if (inboundValue instanceof JSONArray) {
                        JSONArray inboundArray = (JSONArray) inboundValue;
                        for (int j = 0; j < inboundArray.size(); j++) {
                            Object item = inboundArray.get(j);
                            if (containsString(oldRouteInboundTags, item)) {
                                inboundArray.set(j, effectiveNewTag);
                            }
                        }
                    }
                }
            }
        }
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static void addIfNotEmpty(List<String> values, String value) {
        if (StringUtils.isNotEmpty(value) && !values.contains(value)) {
            values.add(value);
        }
    }

    private static boolean containsString(List<String> values, Object value) {
        return value instanceof String && values.contains(value);
    }

    public static String removeSocks5RelayFromSingBoxConfig(String configJson) {
        if (StringUtils.isEmpty(configJson)) {
            throw new IllegalArgumentException("sing-box 配置不能为空");
        }
        JSONObject root = JSON.parseObject(configJson);
        JSONArray outbounds = root.getJSONArray("outbounds");
        if (outbounds == null || outbounds.isEmpty()) return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);

        int socksIndex = -1;
        Set<String> socksTags = new LinkedHashSet<>();
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            if (outbound != null && "socks".equals(outbound.getString("type"))
                    && !isManagedDomainOutbound(outbound)) {
                if (StringUtils.isNotEmpty(outbound.getString("tag"))) socksTags.add(outbound.getString("tag"));
                socksIndex = i;
                break;
            }
        }
        if (socksIndex < 0) return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
        JSONObject direct = new JSONObject();
        direct.put("type", "direct");
        outbounds.set(socksIndex, direct);
        refreshManagedDomainEgress(outbounds, direct);
        JSONObject route = root.getJSONObject("route");
        JSONArray rules = route != null ? route.getJSONArray("rules") : null;
        if (rules != null) {
            removeRelayRouteRules(rules, socksTags);
            if (rules.isEmpty()) route.remove("rules"); else route.put("rules", rules);
            if (route.isEmpty()) root.remove("route");
        }
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static boolean isManagedDomainOutbound(JSONObject outbound) {
        String tag = outbound != null ? outbound.getString("tag") : null;
        return tag != null && (tag.startsWith(DOMAIN_EGRESS_TAG_PREFIX) || tag.startsWith(DOMAIN_BLOCK_TAG_PREFIX));
    }

    private static void refreshManagedDomainEgress(JSONArray outbounds, JSONObject source) {
        if (outbounds == null || source == null) return;
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            String tag = outbound != null ? outbound.getString("tag") : null;
            if (tag == null || !tag.startsWith(DOMAIN_EGRESS_TAG_PREFIX)) continue;
            JSONObject refreshed = JSON.parseObject(source.toJSONString());
            refreshed.put("tag", tag);
            outbounds.set(i, refreshed);
        }
    }

    private static void removeRelayRouteRules(JSONArray rules, Set<String> socksTags) {
        if (rules == null || socksTags == null || socksTags.isEmpty()) return;
        for (int i = rules.size() - 1; i >= 0; i--) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule != null && socksTags.contains(rule.getString("outbound"))) rules.remove(i);
        }
    }

    private static int managedDomainRuleCount(JSONArray rules, String inboundTag) {
        int count = 0;
        while (count < rules.size()) {
            JSONObject rule = rules.getJSONObject(count);
            if (rule == null || !matchesInbound(rule.get("inbound"), inboundTag)) break;
            String outbound = rule.getString("outbound");
            boolean managed = "sniff".equals(rule.getString("action"))
                    || "reject".equals(rule.getString("action"))
                    || (outbound != null && (outbound.startsWith(DOMAIN_EGRESS_TAG_PREFIX)
                    || outbound.startsWith(DOMAIN_BLOCK_TAG_PREFIX)));
            if (!managed) break;
            count++;
        }
        return count;
    }

    /** Adds/removes the legacy whitelist policy. */
    public static String applyDomainWhitelistToSingBoxConfig(String configJson, List<String> domains, boolean modernRules) {
        return applyDomainPolicyToSingBoxConfig(configJson, domains, "whitelist", modernRules);
    }

    /**
     * Applies a mutually-exclusive whitelist or blacklist while preserving unrelated rules.
     * An empty domain list means unrestricted routing.
     */
    public static String applyDomainPolicyToSingBoxConfig(String configJson, List<String> domains, String mode,
                                                           boolean modernRules) {
        if (!"whitelist".equalsIgnoreCase(mode) && !"blacklist".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("域名策略模式必须为 whitelist 或 blacklist");
        }
        if (StringUtils.isEmpty(configJson)) throw new IllegalArgumentException("sing-box 配置不能为空");
        JSONObject root = JSON.parseObject(configJson);
        JSONArray inbounds = root.getJSONArray("inbounds");
        if (inbounds == null || inbounds.isEmpty()) throw new IllegalArgumentException("sing-box 配置缺少 inbounds");
        JSONObject inbound = inbounds.getJSONObject(0);
        String inboundTag = inbound != null ? inbound.getString("tag") : null;
        if (StringUtils.isEmpty(inboundTag)) throw new IllegalArgumentException("sing-box inbound tag 不能为空");

        JSONObject route = root.getJSONObject("route");
        if (route == null) route = new JSONObject();
        JSONArray rules = route.getJSONArray("rules");
        if (rules == null) rules = new JSONArray();
        boolean hadLegacyPolicy = hasManagedLegacyDomainRule(rules, inboundTag);
        Set<String> managedOutboundTags = managedDomainOutboundTags(rules, inboundTag);

        JSONArray outbounds = root.getJSONArray("outbounds");
        if (outbounds == null || outbounds.isEmpty()) throw new IllegalArgumentException("sing-box 配置缺少 outbounds");
        String suffix = Integer.toHexString(inboundTag.hashCode());
        boolean hadManagedMarker = hasOutboundTag(outbounds, DOMAIN_EGRESS_TAG_PREFIX + suffix)
                || hasOutboundTag(outbounds, DOMAIN_BLOCK_TAG_PREFIX + suffix);
        removeManagedDomainRules(rules, inboundTag, hadManagedMarker);
        managedOutboundTags.add(DOMAIN_EGRESS_TAG_PREFIX + suffix);
        managedOutboundTags.add(DOMAIN_BLOCK_TAG_PREFIX + suffix);
        removeManagedDomainOutbounds(outbounds, managedOutboundTags);

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (domains != null) {
            for (String domain : domains) {
                if (StringUtils.isNotEmpty(domain) && StringUtils.isNotEmpty(domain.trim())) normalized.add(domain.trim().toLowerCase());
            }
        }
        if (normalized.isEmpty()) {
            if (hadLegacyPolicy) {
                inbound.remove("sniff");
                inbound.remove("sniff_override_destination");
            }
            if (rules.isEmpty()) route.remove("rules"); else route.put("rules", rules);
            if (route.isEmpty()) root.remove("route"); else root.put("route", route);
            return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
        }

        String blockTag = DOMAIN_BLOCK_TAG_PREFIX + suffix;
        JSONArray exactDomains = new JSONArray();
        JSONArray suffixDomains = new JSONArray();
        for (String domain : normalized) {
            exactDomains.add(domain);
            suffixDomains.add("." + domain);
        }

        JSONArray managed = new JSONArray();
        boolean blacklist = "blacklist".equalsIgnoreCase(mode);
        if (modernRules) {
            if (hadLegacyPolicy) {
                inbound.remove("sniff");
                inbound.remove("sniff_override_destination");
            }
            JSONObject sniff = inboundRule(inboundTag);
            sniff.put("action", "sniff");
            sniff.put("sniffer", Arrays.asList("tls", "http", "quic"));
            managed.add(sniff);

            if (blacklist) {
                // Keep an unused, namespaced outbound as an ownership marker for safe/idempotent cleanup.
                createManagedEgress(outbounds, suffix);
                JSONObject reject = inboundRule(inboundTag);
                reject.put("domain", exactDomains);
                reject.put("domain_suffix", suffixDomains);
                reject.put("action", "reject");
                reject.put("method", "default");
                managed.add(reject);
            } else {
                String egressTag = createManagedEgress(outbounds, suffix);
                JSONObject allow = inboundRule(inboundTag);
                allow.put("domain", exactDomains);
                allow.put("domain_suffix", suffixDomains);
                allow.put("action", "route");
                allow.put("outbound", egressTag);
                managed.add(allow);

                JSONObject reject = inboundRule(inboundTag);
                reject.put("action", "reject");
                reject.put("method", "default");
                managed.add(reject);
            }
        } else {
            inbound.put("sniff", true);
            inbound.put("sniff_override_destination", true);
            JSONObject block = new JSONObject();
            block.put("type", "block");
            block.put("tag", blockTag);
            outbounds.add(block);

            if (blacklist) {
                JSONObject reject = inboundRule(inboundTag);
                reject.put("domain", exactDomains);
                reject.put("domain_suffix", suffixDomains);
                reject.put("outbound", blockTag);
                managed.add(reject);
            } else {
                String egressTag = createManagedEgress(outbounds, suffix);
                JSONObject allow = inboundRule(inboundTag);
                allow.put("domain", exactDomains);
                allow.put("domain_suffix", suffixDomains);
                allow.put("outbound", egressTag);
                managed.add(allow);
                JSONObject reject = inboundRule(inboundTag);
                reject.put("outbound", blockTag);
                managed.add(reject);
            }
        }
        for (int i = managed.size() - 1; i >= 0; i--) rules.add(0, managed.get(i));
        route.put("rules", rules);
        root.put("route", route);
        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private static JSONObject inboundRule(String inboundTag) {
        JSONObject rule = new JSONObject();
        rule.put("inbound", Collections.singletonList(inboundTag));
        return rule;
    }

    private static void removeManagedDomainRules(JSONArray rules, String inboundTag) {
        removeManagedDomainRules(rules, inboundTag, true);
    }

    private static void removeManagedDomainRules(JSONArray rules, String inboundTag, boolean hadManagedMarker) {
        // Modern blacklist is the managed sniff + domain-scoped reject pair at the rule prefix.
        if (hadManagedMarker && rules.size() >= 2) {
            JSONObject sniff = rules.getJSONObject(0);
            JSONObject reject = rules.getJSONObject(1);
            if (sniff != null && reject != null && "sniff".equals(sniff.getString("action"))
                    && "reject".equals(reject.getString("action"))
                    && matchesInbound(sniff.get("inbound"), inboundTag)
                    && matchesInbound(reject.get("inbound"), inboundTag)
                    && (reject.containsKey("domain") || reject.containsKey("domain_suffix"))) {
                rules.remove(1);
                rules.remove(0);
            }
        }
        for (int i = 0; i < rules.size();) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null || !matchesInbound(rule.get("inbound"), inboundTag)) {
                i++;
                continue;
            }
            String outbound = rule.getString("outbound");
            boolean managedBlock = outbound != null && outbound.startsWith(DOMAIN_BLOCK_TAG_PREFIX);
            boolean managedAllow = outbound != null && outbound.startsWith(DOMAIN_EGRESS_TAG_PREFIX);
            if (managedAllow) {
                if (i > 0) {
                    JSONObject previous = rules.getJSONObject(i - 1);
                    if (previous != null && "sniff".equals(previous.getString("action"))
                            && matchesInbound(previous.get("inbound"), inboundTag)) {
                        rules.remove(i - 1);
                        i--;
                    }
                }
                rules.remove(i);
                if (i < rules.size()) {
                    JSONObject next = rules.getJSONObject(i);
                    if (next != null && "reject".equals(next.getString("action"))
                            && matchesInbound(next.get("inbound"), inboundTag)) rules.remove(i);
                }
                continue;
            }
            if (managedBlock) {
                rules.remove(i);
                continue;
            }
            i++;
        }
    }

    private static boolean hasOutboundTag(JSONArray outbounds, String expectedTag) {
        if (outbounds == null || expectedTag == null) return false;
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            if (outbound != null && expectedTag.equals(outbound.getString("tag"))) return true;
        }
        return false;
    }

    private static boolean hasManagedLegacyDomainRule(JSONArray rules, String inboundTag) {
        if (rules == null) return false;
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null || !matchesInbound(rule.get("inbound"), inboundTag)) continue;
            String outbound = rule.getString("outbound");
            if (outbound != null && outbound.startsWith(DOMAIN_BLOCK_TAG_PREFIX)) return true;
        }
        return false;
    }

    private static Set<String> managedDomainOutboundTags(JSONArray rules, String inboundTag) {
        Set<String> tags = new LinkedHashSet<>();
        if (rules == null) return tags;
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            if (rule == null || !matchesInbound(rule.get("inbound"), inboundTag)) continue;
            String outbound = rule.getString("outbound");
            if (outbound != null && (outbound.startsWith(DOMAIN_EGRESS_TAG_PREFIX)
                    || outbound.startsWith(DOMAIN_BLOCK_TAG_PREFIX))) tags.add(outbound);
        }
        return tags;
    }

    private static boolean matchesInbound(Object value, String inboundTag) {
        if (value instanceof String) return inboundTag.equals(value);
        if (value instanceof JSONArray) return ((JSONArray) value).contains(inboundTag);
        if (value instanceof List) return ((List<?>) value).contains(inboundTag);
        return false;
    }

    private static void removeManagedDomainOutbounds(JSONArray outbounds, Set<String> tags) {
        for (int i = outbounds.size() - 1; i >= 0; i--) {
            JSONObject outbound = outbounds.getJSONObject(i);
            String tag = outbound != null ? outbound.getString("tag") : null;
            if (tag != null && tags.contains(tag)) {
                outbounds.remove(i);
            }
        }
    }

    private static String createManagedEgress(JSONArray outbounds, String suffix) {
        JSONObject direct = null;
        JSONObject source = null;
        JSONObject socksFallback = null;
        for (int i = 0; i < outbounds.size(); i++) {
            JSONObject outbound = outbounds.getJSONObject(i);
            if (outbound == null) continue;
            if ("socks".equals(outbound.getString("type"))) {
                if (socksFallback == null) socksFallback = outbound;
                String tag = outbound.getString("tag");
                if (tag != null && tag.startsWith(SOCKS_RELAY_TAG_PREFIX)) {
                    source = outbound;
                    break;
                }
            }
            if (direct == null && "direct".equals(outbound.getString("type"))) direct = outbound;
        }
        if (source == null) source = socksFallback;
        if (source == null) source = direct;
        if (source == null) throw new IllegalArgumentException("sing-box 配置缺少 direct 或 socks outbound");
        String tag = DOMAIN_EGRESS_TAG_PREFIX + suffix;
        JSONObject managed = JSON.parseObject(source.toJSONString());
        managed.put("tag", tag);
        outbounds.add(managed);
        return tag;
    }

    private static String randomRelayTag() {
        StringBuilder sb = new StringBuilder(SOCKS_RELAY_TAG_PREFIX);
        for (int i = 0; i < 8; i++) {
            sb.append(RELAY_TAG_CHARS.charAt(SECURE_RANDOM.nextInt(RELAY_TAG_CHARS.length())));
        }
        return sb.toString();
    }

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
        String baseName = normalizeNodeBaseName(nodeName);
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

    /**
     * Rename proxy node config file by old/new base names while keeping current suffix.
     * Suffix is kept as either ".json" or ".json.disabled" according to {@code disabled}.
     *
     * @param instanceId  instance id
     * @param oldNodeName old base name (may include .json/.json.disabled)
     * @param newNodeName new base name (may include .json/.json.disabled)
     * @param disabled    current suffix flag, true=.json.disabled, false=.json
     */
    public void renameProxyNodeConfig(Long instanceId, String oldNodeName, String newNodeName, boolean disabled) throws IOException {
        if (instanceId == null || StringUtils.isEmpty(oldNodeName) || StringUtils.isEmpty(newNodeName)) {
            throw new IllegalArgumentException("instanceId, oldNodeName and newNodeName cannot be empty");
        }
        String oldBaseName = normalizeNodeBaseName(oldNodeName);
        String newBaseName = normalizeNodeBaseName(newNodeName);
        if (oldBaseName.equals(newBaseName)) {
            return;
        }
        String suffix = disabled ? ".json" + DISABLED_SUFFIX : ".json";
        String fromPath = CONF_DIR + "/" + oldBaseName + suffix;
        String toPath = CONF_DIR + "/" + newBaseName + suffix;

        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            try (Session session = ssh.startSession()) {
                String fromQuoted = shellQuote(fromPath);
                String toQuoted = shellQuote(toPath);
                String cmd = "sh -c \"if [ ! -f " + fromQuoted + " ]; then echo __SRC_MISSING__; " +
                        "elif [ -f " + toQuoted + " ]; then echo __DST_EXISTS__; " +
                        "elif mv " + fromQuoted + " " + toQuoted + "; then echo __OK__; else echo __MV_FAIL__; fi\"";
                String out = execAndRead(session, cmd);
                String marker = out != null ? out : "";
                if (marker.contains("__SRC_MISSING__")) {
                    throw new IOException("source config file not found: " + fromPath);
                }
                if (marker.contains("__DST_EXISTS__")) {
                    throw new IOException("target config file already exists: " + toPath);
                }
                if (!marker.contains("__OK__")) {
                    throw new IOException("rename config file failed: " + fromPath + " -> " + toPath);
                }
            }
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

    public void updateProxyNodeConfigPortAndName(Long instanceId, String oldNodeName, String newNodeName, boolean disabled, Integer oldPort, Integer newPort) throws IOException {
        if (instanceId == null || StringUtils.isEmpty(oldNodeName) || StringUtils.isEmpty(newNodeName)) {
            throw new IllegalArgumentException("instanceId, oldNodeName and newNodeName cannot be empty");
        }
        if (newPort == null || newPort < 1 || newPort > 65535) {
            throw new IllegalArgumentException("端口范围为 1-65535");
        }
        String oldBaseName = normalizeNodeBaseName(oldNodeName);
        String newBaseName = normalizeNodeBaseName(newNodeName);
        String suffix = disabled ? ".json" + DISABLED_SUFFIX : ".json";
        String toPath = CONF_DIR + "/" + newBaseName + suffix;
        String oldInboundTag = oldBaseName + ".json";
        String newInboundTag = newBaseName + ".json";

        SSHClient ssh = null;
        String fromPath = null;
        String tempPath = null;
        String backupPath = null;
        boolean replacementStarted = false;
        boolean success = false;
        try {
            ssh = createSshClient(instanceId);
            ProxyNode locator = new ProxyNode();
            locator.setNodeName(oldBaseName);
            locator.setPort(oldPort);
            locator.setStatus(disabled ? "1" : "0");
            fromPath = resolveProxyNodeConfigPath(ssh, locator);
            String original;
            try (Session readSession = ssh.startSession()) {
                original = execAndRead(readSession, "cat -- " + shellQuote(fromPath));
            }
            if (StringUtils.isEmpty(original)) {
                throw new IOException("读取 sing-box 配置失败: " + fromPath);
            }
            String patched = updateSingBoxListenPortAndInboundName(original, oldInboundTag, newInboundTag, newPort);
            String operationToken = Long.toHexString(System.nanoTime());
            tempPath = fromPath + ".skyway-port-" + operationToken + ".tmp";
            backupPath = fromPath + ".skyway-port-" + operationToken + ".bak";
            writeAndValidateCandidate(ssh, tempPath, patched);

            boolean samePath = fromPath.equals(toPath);
            StringBuilder replace = new StringBuilder("set -e;");
            if (!samePath) {
                replace.append(" [ ! -e ").append(shellQuote(toPath)).append(" ];");
            }
            replace.append(" mv -- ").append(shellQuote(fromPath)).append(' ').append(shellQuote(backupPath)).append(';')
                    .append(" mv -- ").append(shellQuote(tempPath)).append(' ').append(shellQuote(toPath)).append(';')
                    .append(" echo __OK__");
            replacementStarted = true;
            try (Session replaceSession = ssh.startSession()) {
                String out = execAndRead(replaceSession, "sh -c " + quoteSh(replace.toString()) + " 2>&1");
                if (out == null || !out.contains("__OK__")) {
                    throw new IOException("替换 sing-box 配置失败: " + (out != null ? out.trim() : "无输出"));
                }
            }
            if (!disabled) restartSingBoxChecked(ssh);
            success = true;
        } catch (IOException | RuntimeException e) {
            if (ssh != null && replacementStarted && fromPath != null && backupPath != null) {
                try (Session rollback = ssh.startSession()) {
                    String command = "if [ -f " + shellQuote(backupPath) + " ]; then rm -f -- "
                            + shellQuote(toPath) + "; mv -- " + shellQuote(backupPath) + ' '
                            + shellQuote(fromPath) + "; fi";
                    execAndRead(rollback, "sh -c " + quoteSh(command) + " 2>&1");
                } catch (Exception rollbackError) {
                    log.error("rollback proxy port config failed: instanceId={}, oldPort={}, newPort={}",
                            instanceId, oldPort, newPort, rollbackError);
                }
                if (!disabled) {
                    try { restartSingBox(ssh); } catch (Exception ignored) {}
                }
            }
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e.getMessage(), e);
        } finally {
            if (ssh != null) {
                if (tempPath != null) {
                    try (Session cleanup = ssh.startSession()) {
                        String command = "rm -f -- " + shellQuote(tempPath)
                                + (success && backupPath != null ? " " + shellQuote(backupPath) : "");
                        execAndRead(cleanup, "sh -c " + quoteSh(command) + " 2>&1");
                    } catch (Exception ignored) {}
                }
                try {
                    ssh.close();
                } catch (IOException e) {
                    log.debug("SSH close: {}", e.getMessage());
                }
            }
        }
    }

    private static String normalizeNodeBaseName(String nodeName) {
        String baseName = nodeName.trim();
        if (baseName.endsWith(".json" + DISABLED_SUFFIX)) {
            return baseName.substring(0, baseName.length() - (5 + DISABLED_SUFFIX.length()));
        }
        if (baseName.endsWith(".json")) {
            return baseName.substring(0, baseName.length() - 5);
        }
        if (baseName.endsWith(DISABLED_SUFFIX)) {
            return baseName.substring(0, baseName.length() - DISABLED_SUFFIX.length());
        }
        return baseName;
    }

    private static String shellQuote(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "'\"'\"'") + "'";
    }

    private SSHClient createSshClient(Long instanceId) throws IOException {
        VpsInstance inst = vpsInstanceService.getById(instanceId);
        if (inst == null || StringUtils.isEmpty(inst.getIp()) || inst.getSshPort() == null
                || StringUtils.isEmpty(inst.getSshUsername())) {
            throw new IllegalStateException("实例不存在或 SSH 信息不完整");
        }
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(15_000);
        ssh.connect(inst.getIp(), inst.getSshPort() != null ? inst.getSshPort() : 22);
        ssh.authPassword(inst.getSshUsername(), inst.getSshPassword() != null ? inst.getSshPassword() : "");
        return ssh;
    }

    /**
     * 检查实例 SSH 是否可连接（用于用户详情等场景：先确认 SSH 再执行添加节点等命令）。
     * @throws IOException 连接失败或执行失败时抛出，消息可直接返回给前端
     */
    public void checkSshConnection(Long instanceId) throws IOException {
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            try (Session session = ssh.startSession()) {
                Command cmd = session.exec("true");
                consumeStream(cmd.getInputStream());
                consumeStream(cmd.getErrorStream());
                cmd.join(10, TimeUnit.SECONDS);
                Integer exit = cmd.getExitStatus();
                if (exit == null) {
                    throw new IOException("SSH 命令执行超时或未正常结束");
                }
                if (exit != 0) {
                    throw new IOException("SSH 执行异常，退出码: " + exit);
                }
            }
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    /** Reads live TCP/UDP sockets, sing-box configs, Docker mappings and the kernel ephemeral range. */
    public RemotePortScan scanRemotePorts(Long instanceId) throws IOException {
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            return scanRemotePorts(ssh);
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    public RemotePortScan scanRemotePorts(SSHClient ssh) throws IOException {
        if (ssh == null || !ssh.isConnected()) {
            throw new IOException("SSH 连接不可用");
        }
        try (Session session = ssh.startSession()) {
            String output = execAndRead(session, "sh -c " + quoteSh(PORT_SCAN_SCRIPT));
            return parseRemotePortScan(output);
        }
    }

    public void assertRemotePortAvailable(SSHClient ssh, int port) throws IOException {
        RemotePortScan scan = scanRemotePorts(ssh);
        if (!scan.isComplete()) {
            throw new IOException("服务器端口扫描不完整：" + String.join("、", scan.getMissingSources()));
        }
        if (scan.getUnavailablePorts().contains(port)) {
            String sources = scan.describeSources(port);
            throw new IllegalStateException("端口 (" + port + ") 已被占用"
                    + (sources.isEmpty() ? "" : "（" + sources + "）") + "，请更换端口");
        }
    }

    static RemotePortScan parseRemotePortScan(String output) throws IOException {
        if (output == null) throw new IOException("服务器端口扫描无输出");
        RemotePortScan scan = new RemotePortScan();
        String[] lines = output.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw != null ? raw.trim() : "";
            if (line.isEmpty()) continue;
            if ("SOCKETS_OK".equals(line)) scan.socketsComplete = true;
            else if ("CONFIG_OK".equals(line)) scan.configComplete = true;
            else if ("DOCKER_OK".equals(line)) scan.dockerComplete = true;
            else if ("EPHEMERAL_OK".equals(line)) scan.ephemeralComplete = true;
            else if (line.startsWith("SOCKET_HEX=")) {
                String hex = line.substring("SOCKET_HEX=".length()).trim();
                try { scan.addUnavailable(Integer.parseInt(hex, 16), "系统 TCP/UDP socket"); } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("CONFIG_PORT=")) {
                scan.addDecimalPort(line.substring("CONFIG_PORT=".length()), "sing-box 配置");
            } else if (line.startsWith("DOCKER_PORT=")) {
                scan.addDecimalPort(line.substring("DOCKER_PORT=".length()), "Docker 映射");
            } else if (line.startsWith("EPHEMERAL=")) {
                String[] range = line.substring("EPHEMERAL=".length()).split("-", -1);
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        if (start >= 1 && end <= 65535 && start <= end) {
                            scan.ephemeralStart = start;
                            scan.ephemeralEnd = end;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (scan.ephemeralStart == null || scan.ephemeralEnd == null) scan.ephemeralComplete = false;
        return scan;
    }

    public static final class RemotePortScan {
        private final Set<Integer> unavailablePorts = new LinkedHashSet<>();
        private final Map<Integer, Set<String>> sources = new LinkedHashMap<>();
        private boolean socketsComplete;
        private boolean configComplete;
        private boolean dockerComplete;
        private boolean ephemeralComplete;
        private Integer ephemeralStart;
        private Integer ephemeralEnd;

        private void addDecimalPort(String value, String source) {
            try { addUnavailable(Integer.parseInt(value.trim()), source); } catch (NumberFormatException ignored) {}
        }

        private void addUnavailable(int port, String source) {
            if (port < 1 || port > 65535) return;
            unavailablePorts.add(port);
            sources.computeIfAbsent(port, ignored -> new LinkedHashSet<>()).add(source);
        }

        public Set<Integer> getUnavailablePorts() { return new LinkedHashSet<>(unavailablePorts); }
        public Integer getEphemeralStart() { return ephemeralStart; }
        public Integer getEphemeralEnd() { return ephemeralEnd; }
        public boolean isComplete() { return socketsComplete && configComplete && dockerComplete && ephemeralComplete; }

        public List<String> getMissingSources() {
            List<String> missing = new ArrayList<>();
            if (!socketsComplete) missing.add("系统 socket");
            if (!configComplete) missing.add("sing-box 配置");
            if (!dockerComplete) missing.add("Docker 映射");
            if (!ephemeralComplete) missing.add("动态端口范围");
            return missing;
        }

        public String describeSources(int port) {
            Set<String> values = sources.get(port);
            return values == null ? "" : String.join("、", values);
        }
    }

    /**
     * 检测实例 SSH 可达性，返回应设置的状态：running / stopped / abnormal。
     * 供定时任务同步 VPS 状态使用。
     */
    public String detectInstanceStatus(Long instanceId) {
        try {
            SSHClient ssh = createSshClient(instanceId);
            try {
                try (Session session = ssh.startSession()) {
                    Command cmd = session.exec("true");
                    consumeStream(cmd.getInputStream());
                    consumeStream(cmd.getErrorStream());
                    cmd.join(10, TimeUnit.SECONDS);
                    Integer exit = cmd.getExitStatus();
                    if (exit != null && exit == 0) return "running";
                }
            } finally {
                try {
                    ssh.close();
                } catch (IOException e) {
                    log.debug("SSH close: {}", e.getMessage());
                }
            }
            return "running";
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("refused") || msg.contains("timeout") || msg.contains("connection") || msg.contains("timed out")) {
                return "stopped";
            }
            return "abnormal";
        } catch (Exception e) {
            log.debug("detectInstanceStatus instanceId={} error: {}", instanceId, e.getMessage());
            return "abnormal";
        }
    }

    /**
     * 连接测试并拉取 CPU/内存/磁盘规格（用于新增/编辑 VPS 时「连接测试」按钮）。
     * 不查库，直接使用传入的 SSH 参数。
     *
     * @return Map: success (boolean), message (String), cpu (String), memory (String), disk (String)
     */
    public Map<String, Object> testConnectionAndFetchSpec(String ip, Integer sshPort, String sshUsername, String sshPassword) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("cpu", "");
        result.put("memory", "");
        result.put("disk", "");
        result.put("osType", "");
        result.put("osVersion", "");
        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(sshUsername)) {
            result.put("message", "请填写 IP 和 SSH 账号");
            return result;
        }
        int port = sshPort != null && sshPort > 0 ? sshPort : 22;
        String pass = sshPassword != null ? sshPassword : "";
        SSHClient ssh = null;
        try {
            ssh = createSshClient(ip, port, sshUsername, pass);
            // SSHJ 的 Session 一次 exec 后通道即耗尽，每个命令需单独开 Session
            try (Session s1 = ssh.startSession()) {
                String nprocOut = execAndRead(s1, "nproc 2>/dev/null || grep -c ^processor /proc/cpuinfo 2>/dev/null || echo 0");
                int cores = 0;
                if (nprocOut != null && !nprocOut.trim().isEmpty()) {
                    try {
                        cores = Integer.parseInt(nprocOut.trim().split("\\s+")[0]);
                    } catch (NumberFormatException ignored) {}
                }
                result.put("cpu", cores > 0 ? cores + "核" : "");
            }
            try (Session s2 = ssh.startSession()) {
                String freeOut = execAndRead(s2, "free -m 2>/dev/null | grep '^Mem:'");
                if (freeOut != null && freeOut.trim().length() > 0) {
                    String[] parts = freeOut.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int totalMb = Integer.parseInt(parts[1]);
                            if (totalMb >= 1024) {
                                result.put("memory", Math.round(totalMb / 1024.0) + "G");
                            } else {
                                result.put("memory", totalMb + "M");
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            try (Session s3 = ssh.startSession()) {
                String dfOut = execAndRead(s3, "df -h / 2>/dev/null | tail -1");
                if (dfOut != null && dfOut.trim().length() > 0) {
                    String[] parts = dfOut.trim().split("\\s+");
                    if (parts.length >= 2) {
                        result.put("disk", formatDiskSizeRounded(parts[1].trim()));
                    }
                }
            }
            try (Session s4 = ssh.startSession()) {
                String osOut = execAndRead(s4, "cat /etc/os-release 2>/dev/null || cat /etc/redhat-release 2>/dev/null");
                if (osOut != null && !osOut.trim().isEmpty()) {
                    java.util.Map<String, String> os = parseOsRelease(osOut);
                    if (!os.isEmpty()) {
                        result.put("osType", os.getOrDefault("osType", ""));
                        result.put("osVersion", os.getOrDefault("osVersion", ""));
                    }
                }
            }
            result.put("success", true);
            result.put("message", "连接成功，已回写规格");
        } catch (Exception e) {
            result.put("message", friendlyConnectionErrorMessage(e));
            log.debug("testConnectionAndFetchSpec failed: {}", e.getMessage());
        } finally {
            if (ssh != null) {
                try {
                    ssh.close();
                } catch (IOException e) {
                    log.debug("SSH close: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    public RealtimeSpeedSnapshot readRealtimeSpeed(Long instanceId) throws IOException {
        VpsInstance inst = vpsInstanceService.getById(instanceId);
        if (inst == null) {
            throw new IllegalStateException("实例不存在");
        }
        if (!isRealtimeSpeedAllowedStatus(inst.getStatus())) {
            RealtimeSpeedSnapshot snapshot = new RealtimeSpeedSnapshot();
            snapshot.setSkipped(true);
            snapshot.setMessage("实例状态非正常，已跳过实时网速采集");
            return snapshot;
        }
        SSHClient ssh = null;
        try {
            ssh = openSshClient(instanceId);
            ensureRealtimeSpeedScript(ssh);
            try (Session speedSession = ssh.startSession()) {
                String output = execAndReadWithTimeout(speedSession,
                        "sh -c 'timeout 4s " + SPEED_SCRIPT_PATH + " 2>/dev/null || true'", 6);
                return parseRealtimeSpeedOutput(output);
            }
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

    public SSHClient openSshClient(Long instanceId) throws IOException {
        return createSshClient(instanceId);
    }

    public void ensureRealtimeSpeedScript(SSHClient ssh) throws IOException {
        if (ssh == null) {
            throw new IOException("SSH 连接为空");
        }
        try (Session installSession = ssh.startSession()) {
            installRealtimeSpeedScript(installSession);
        }
    }

    public Command startRealtimeSpeedCommand(Session session) throws IOException {
        return session.exec("sh -c '" + SPEED_SCRIPT_PATH + " 2>&1'");
    }

    public static boolean isRealtimeSpeedAllowedStatus(String status) {
        return "running".equals(status);
    }

    private static void installRealtimeSpeedScript(Session session) throws IOException {
        String encodedScript = Base64.getEncoder().encodeToString(SINGBOX_SPEED_SCRIPT.getBytes(StandardCharsets.UTF_8));
        String install = "if [ ! -x " + SPEED_SCRIPT_PATH + " ]; then "
                + "printf '%s' '" + encodedScript + "' | base64 -d > " + SPEED_SCRIPT_PATH
                + " && chmod +x " + SPEED_SCRIPT_PATH + "; fi";
        String out = execAndRead(session, "sh -c " + quoteSh(install));
        if (out != null && (out.contains("Permission denied") || out.contains("base64:"))) {
            throw new IOException("实时网速脚本安装失败: " + out.trim());
        }
    }

    public static RealtimeSpeedSnapshot parseRealtimeSpeedOutput(String output) {
        RealtimeSpeedSnapshot snapshot = new RealtimeSpeedSnapshot();
        if (output == null || output.trim().isEmpty()) {
            return snapshot;
        }
        String normalized = stripAnsi(output).replace('\r', '\n');
        for (String line : normalized.split("\\n")) {
            Matcher matcher = SPEED_LINE.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String port = matcher.group(1);
            double upMbps = parseDouble(matcher.group(2));
            double downMbps = parseDouble(matcher.group(3));
            if (Integer.parseInt(port) > 0 && Integer.parseInt(port) <= 65535) {
                snapshot.putPortSpeed(port, upMbps, downMbps);
            }
        }
        snapshot.recalculateTotals();
        return snapshot;
    }

    public PortRateLimitRemoteResult setPortRateLimit(Long instanceId, int port, int downloadMbps, int uploadMbps) throws IOException {
        String line = buildTcPortRuleLine(port, downloadMbps, uploadMbps);
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            ensureTcManagerScript(ssh);
            try (Session session = ssh.startSession()) {
                String script = "touch " + TC_PORT_RULES_CONF + "; "
                        + "sed -i '/^" + port + ":/d' " + TC_PORT_RULES_CONF + "; "
                        + "printf '%s\\n' " + shellQuote(line) + " >> " + TC_PORT_RULES_CONF + "; "
                        + "printf '5\\n\\n0\\n' | " + TC_MANAGER_SCRIPT_PATH + " 2>&1";
                String out = execAndReadWithTimeout(session, "sh -c " + quoteSh(script), 45);
                return new PortRateLimitRemoteResult(port, normalizeApplyOutput(out));
            }
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    public PortRateLimitRemoteResult removePortRateLimit(Long instanceId, int port) throws IOException {
        validatePortRateLimit(port, 1, 1);
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            ensureTcManagerScript(ssh);
            try (Session session = ssh.startSession()) {
                String script = "touch " + TC_PORT_RULES_CONF + "; "
                        + "sed -i '/^" + port + ":/d' " + TC_PORT_RULES_CONF + "; "
                        + "printf '5\\n\\n0\\n' | " + TC_MANAGER_SCRIPT_PATH + " 2>&1";
                String out = execAndReadWithTimeout(session, "sh -c " + quoteSh(script), 45);
                return new PortRateLimitRemoteResult(port, normalizeApplyOutput(out));
            }
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    public List<PortRateLimitRule> listRemotePortRateLimits(Long instanceId) throws IOException {
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            ensureTcManagerScript(ssh);
            try (Session session = ssh.startSession()) {
                String out = execAndRead(session, "sh -c " + quoteSh("touch " + TC_PORT_RULES_CONF + "; cat " + TC_PORT_RULES_CONF));
                return parseTcPortRules(out);
            }
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    public static String buildTcPortRuleLine(int port, int downloadMbps, int uploadMbps) {
        validatePortRateLimit(port, downloadMbps, uploadMbps);
        return port + ":" + downloadMbps + ":" + uploadMbps;
    }

    public static List<PortRateLimitRule> parseTcPortRules(String output) {
        List<PortRateLimitRule> rules = new ArrayList<>();
        if (output == null || output.trim().isEmpty()) {
            return rules;
        }
        for (String line : output.replace('\r', '\n').split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            Matcher matcher = TC_RULE_LINE.matcher(trimmed);
            if (!matcher.matches()) {
                continue;
            }
            int port = Integer.parseInt(matcher.group(1));
            int down = Integer.parseInt(matcher.group(2));
            int up = Integer.parseInt(matcher.group(3));
            if (isValidPort(port) && down > 0 && up > 0) {
                rules.add(new PortRateLimitRule(port, down, up));
            }
        }
        return rules;
    }

    private void ensureTcManagerScript(SSHClient ssh) throws IOException {
        try (Session session = ssh.startSession()) {
            String encodedScript = Base64.getEncoder().encodeToString(TC_MANAGER_SCRIPT.getBytes(StandardCharsets.UTF_8));
            String install = "if [ ! -x " + TC_MANAGER_SCRIPT_PATH + " ]; then "
                    + "printf '%s' " + shellQuote(encodedScript) + " | base64 -d > " + TC_MANAGER_SCRIPT_PATH
                    + " && chmod +x " + TC_MANAGER_SCRIPT_PATH + "; fi; "
                    + "touch " + TC_PORT_RULES_CONF + "; "
                    + "[ -f /etc/tc_global_rules.conf ] || echo 'OFF:0:0' > /etc/tc_global_rules.conf";
            String out = execAndReadWithTimeout(session, "sh -c " + quoteSh(install) + " 2>&1", 30);
            if (out != null && (out.contains("Permission denied") || out.contains("base64:"))) {
                throw new IOException("TC 管理脚本安装失败: " + out.trim());
            }
        }
    }

    private static void validatePortRateLimit(int port, int downloadMbps, int uploadMbps) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("端口范围为 1-65535");
        }
        if (downloadMbps <= 0 || uploadMbps <= 0) {
            throw new IllegalArgumentException("限速带宽必须大于 0 Mbps");
        }
    }

    private static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    private static String normalizeApplyOutput(String output) {
        String text = stripAnsi(output == null ? "" : output).trim();
        if (text.length() > 2000) {
            return text.substring(text.length() - 2000);
        }
        return text;
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0D;
        }
    }

    public static final class RealtimeSpeedSnapshot {
        private double totalUpMbps;
        private double totalDownMbps;
        private boolean skipped;
        private String message;
        private final Map<String, PortSpeed> ports = new LinkedHashMap<>();

        public double getTotalUpMbps() {
            return totalUpMbps;
        }

        public double getTotalDownMbps() {
            return totalDownMbps;
        }

        public Map<String, PortSpeed> getPorts() {
            return ports;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public void setSkipped(boolean skipped) {
            this.skipped = skipped;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        private void putPortSpeed(String port, double upMbps, double downMbps) {
            ports.put(port, new PortSpeed(upMbps, downMbps));
        }

        private void recalculateTotals() {
            totalUpMbps = 0D;
            totalDownMbps = 0D;
            for (PortSpeed speed : ports.values()) {
                totalUpMbps += speed.getUpMbps();
                totalDownMbps += speed.getDownMbps();
            }
        }
    }

    public static final class PortRateLimitRule {
        private final int port;
        private final int downloadMbps;
        private final int uploadMbps;

        public PortRateLimitRule(int port, int downloadMbps, int uploadMbps) {
            this.port = port;
            this.downloadMbps = downloadMbps;
            this.uploadMbps = uploadMbps;
        }

        public int getPort() {
            return port;
        }

        public int getDownloadMbps() {
            return downloadMbps;
        }

        public int getUploadMbps() {
            return uploadMbps;
        }
    }

    public static final class PortRateLimitRemoteResult {
        private final int port;
        private final String output;

        public PortRateLimitRemoteResult(int port, String output) {
            this.port = port;
            this.output = output;
        }

        public int getPort() {
            return port;
        }

        public String getOutput() {
            return output;
        }
    }

    public static final class PortSpeed {
        private final double upMbps;
        private final double downMbps;

        public PortSpeed(double upMbps, double downMbps) {
            this.upMbps = upMbps;
            this.downMbps = downMbps;
        }

        public double getUpMbps() {
            return upMbps;
        }

        public double getDownMbps() {
            return downMbps;
        }
    }

    /**
     * 解析 /etc/os-release 或 /etc/redhat-release 输出，返回 osType（centos/ubuntu/debian/alpine/other）和 osVersion。
     */
    private static java.util.Map<String, String> parseOsRelease(String output) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (output == null || output.isEmpty()) return map;
        String id = null;
        String versionId = null;
        // /etc/os-release: ID=ubuntu, VERSION_ID="24.04"
        if (output.contains("=")) {
            for (String line : output.split("\n")) {
                String lineTrim = line.trim();
                if (lineTrim.startsWith("ID=")) {
                    id = lineTrim.substring(3).trim().replaceAll("^\"|\"$", "").toLowerCase();
                } else if (lineTrim.startsWith("VERSION_ID=")) {
                    versionId = lineTrim.substring(11).trim().replaceAll("^\"|\"$", "");
                }
            }
        } else {
            // /etc/redhat-release: "CentOS Linux release 7.9.2009 (Core)"
            String lower = output.toLowerCase();
            if (lower.contains("centos") || lower.contains("rhel") || lower.contains("red hat") || lower.contains("rocky") || lower.contains("alma")) {
                id = "centos";
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+\\.\\d+)");
                java.util.regex.Matcher m = p.matcher(output);
                if (m.find()) versionId = m.group(1);
            }
        }
        if (id != null) {
            if ("rhel".equals(id) || "redhat".equals(id) || "rocky".equals(id) || "almalinux".equals(id)) id = "centos";
            else if (!("ubuntu".equals(id) || "debian".equals(id) || "alpine".equals(id) || "centos".equals(id))) id = "other";
            map.put("osType", id);
        }
        if (versionId != null && !versionId.isEmpty()) {
            map.put("osVersion", versionId);
        }
        return map;
    }

    /** 将 df -h 的 Size（如 7.8G、50G、512M）四舍五入为整数后返回，如 7.8G -> 8G */
    private static String formatDiskSizeRounded(String size) {
        if (size == null || size.isEmpty()) return size;
        String s = size.trim().toUpperCase();
        if (s.endsWith("G")) {
            try {
                double val = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(val) + "G";
            } catch (NumberFormatException ignored) {}
        } else if (s.endsWith("M")) {
            try {
                double val = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(val) + "M";
            } catch (NumberFormatException ignored) {}
        } else if (s.endsWith("T")) {
            try {
                double val = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(val) + "T";
            } catch (NumberFormatException ignored) {}
        } else if (s.endsWith("K")) {
            try {
                double val = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(val) + "K";
            } catch (NumberFormatException ignored) {}
        }
        return size;
    }

    /** 连接测试用：带超时（8 秒），避免账号错误或网络不通时长时间卡住 */
    private SSHClient createSshClient(String ip, int port, String username, String password) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(8000);
        ssh.connect(ip, port);
        ssh.authPassword(username, password);
        return ssh;
    }

    /** 将连接/认证异常转为前端可展示的简短提示 */
    private static String friendlyConnectionErrorMessage(Throwable e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("auth") || msg.contains("password") || msg.contains("denied") || msg.contains("invalid")
                || msg.contains("authentication") || msg.contains("access denied")) {
            return "SSH 账号或密码错误，请检查后重试";
        }
        if (msg.contains("refused") || msg.contains("timeout") || msg.contains("timed out") || msg.contains("connect")
                || msg.contains("unreachable") || msg.contains("no route") || msg.contains("network is unreachable")) {
            return "网络不通或连接超时，请检查 IP、端口与网络";
        }
        return e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : "连接失败，请检查 IP、端口、账号密码与网络";
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

    private String readAndApplySocksRelay(SSHClient ssh, String confPath, Socks5RelayConfig relay) throws IOException {
        String original;
        try (Session readSession = ssh.startSession()) {
            original = execAndRead(readSession, "cat " + shellQuote(confPath));
        }
        if (StringUtils.isEmpty(original)) {
            throw new IOException("读取 sing-box 配置失败: " + confPath);
        }
        return upsertSocks5RelayToSingBoxConfig(original, relay, randomRelayTag());
    }

    public void applySocks5RelayToRemoteConfig(SSHClient ssh, String confPath, Socks5RelayConfig relay) throws IOException {
        if (ssh == null) {
            throw new IOException("SSH 连接为空");
        }
        if (relay == null) {
            return;
        }
        String patchedConfig = readAndApplySocksRelay(ssh, confPath, relay);
        try (Session writeSession = ssh.startSession()) {
            writeRemoteTextFile(writeSession, confPath, patchedConfig);
        }
        restartSingBox(ssh);
    }

    public void applySocks5RelayToProxyNodeConfig(ProxyNode node, Socks5RelayConfig relay) throws IOException {
        if (node == null || node.getInstanceId() == null) {
            throw new IllegalArgumentException("节点或实例ID为空");
        }
        if (StringUtils.isEmpty(node.getNodeName())) {
            throw new IllegalArgumentException("节点名称为空");
        }
        SSHClient ssh = null;
        try {
            ssh = createSshClient(node.getInstanceId());
            String baseName = normalizeNodeBaseName(node.getNodeName());
            String suffix = "1".equals(node.getStatus()) ? ".json" + DISABLED_SUFFIX : ".json";
            String confPath = CONF_DIR + "/" + baseName + suffix;
            applySocks5RelayToRemoteConfig(ssh, confPath, relay);
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    public void removeSocks5RelayFromProxyNodeConfig(ProxyNode node) throws IOException {
        if (node == null || node.getInstanceId() == null) {
            throw new IllegalArgumentException("节点或实例ID为空");
        }
        if (StringUtils.isEmpty(node.getNodeName())) {
            throw new IllegalArgumentException("节点名称为空");
        }
        SSHClient ssh = null;
        try {
            ssh = createSshClient(node.getInstanceId());
            String baseName = normalizeNodeBaseName(node.getNodeName());
            String suffix = "1".equals(node.getStatus()) ? ".json" + DISABLED_SUFFIX : ".json";
            String confPath = CONF_DIR + "/" + baseName + suffix;
            String original;
            try (Session readSession = ssh.startSession()) {
                original = execAndRead(readSession, "cat " + shellQuote(confPath));
            }
            if (StringUtils.isEmpty(original)) {
                throw new IOException("读取 sing-box 配置失败: " + confPath);
            }
            String patchedConfig = removeSocks5RelayFromSingBoxConfig(original);
            try (Session writeSession = ssh.startSession()) {
                writeRemoteTextFile(writeSession, confPath, patchedConfig);
            }
            restartSingBox(ssh);
        } finally {
            if (ssh != null) {
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    /** Applies a domain policy to an existing active or disabled node config. */
    public void applyDomainWhitelistToProxyNodeConfig(ProxyNode node, ProxyNodeDomainWhitelist policy) throws IOException {
        Map<ProxyNode, ProxyNodeDomainWhitelist> updates = new LinkedHashMap<>();
        updates.put(node, policy);
        applyDomainWhitelistsToProxyNodeConfigs(updates);
    }

    /**
     * Applies multiple policies on one VPS as a transaction: one SSH connection, all candidates
     * validated before replacement, one service restart, and group rollback on any failure.
     */
    public void applyDomainWhitelistsToProxyNodeConfigs(Map<ProxyNode, ProxyNodeDomainWhitelist> updates) throws IOException {
        if (updates == null || updates.isEmpty()) return;
        Long instanceId = null;
        for (ProxyNode node : updates.keySet()) {
            if (node == null || node.getInstanceId() == null || StringUtils.isEmpty(node.getNodeName())
                    || node.getPort() == null || node.getPort() < 1 || node.getPort() > 65535) {
                throw new IllegalArgumentException("节点、实例ID、节点名称或端口无效");
            }
            if (instanceId == null) instanceId = node.getInstanceId();
            if (!instanceId.equals(node.getInstanceId())) throw new IllegalArgumentException("批量节点必须属于同一 VPS");
        }
        SSHClient ssh = null;
        List<DomainConfigCandidate> candidates = new ArrayList<>();
        boolean success = false;
        boolean activeChanged = false;
        boolean replacementStarted = false;
        boolean rollbackSucceeded = false;
        try {
            ssh = createSshClient(instanceId);
            boolean modern = isModernSingBox(ssh);
            Set<String> resolvedPaths = new LinkedHashSet<>();
            String operationToken = Long.toHexString(System.nanoTime());
            for (Map.Entry<ProxyNode, ProxyNodeDomainWhitelist> entry : updates.entrySet()) {
                ProxyNode node = entry.getKey();
                String confPath = resolveProxyNodeConfigPath(ssh, node);
                if (!resolvedPaths.add(confPath)) throw new IOException("多个节点解析到同一配置文件: " + confPath);
                String original;
                try (Session readSession = ssh.startSession()) {
                    original = execAndRead(readSession, "cat -- " + shellQuote(confPath));
                }
                if (StringUtils.isEmpty(original)) throw new IOException("读取 sing-box 配置失败: " + confPath);
                List<String> domains = entry.getValue() != null ? entry.getValue().getDomains() : Collections.emptyList();
                String mode = entry.getValue() != null && entry.getValue().getMode() != null
                        ? entry.getValue().getMode() : "whitelist";
                String patched = applyDomainPolicyToSingBoxConfig(original, domains, mode, modern);
                if (JSON.parseObject(original).equals(JSON.parseObject(patched))) continue;
                DomainConfigCandidate candidate = new DomainConfigCandidate(node, confPath, operationToken);
                candidates.add(candidate);
                try {
                    writeAndValidateCandidate(ssh, candidate.tempPath, patched);
                } catch (IOException modernError) {
                    if (!modern) throw modernError;
                    String legacy = applyDomainPolicyToSingBoxConfig(original, domains, mode, false);
                    try {
                        writeAndValidateCandidate(ssh, candidate.tempPath, legacy);
                    } catch (IOException legacyError) {
                        modernError.addSuppressed(legacyError);
                        throw modernError;
                    }
                }
                if (!candidate.disabled) activeChanged = true;
            }
            if (candidates.isEmpty()) return;

            StringBuilder replace = new StringBuilder("set -e;");
            for (DomainConfigCandidate candidate : candidates) {
                replace.append(" cp -- ").append(shellQuote(candidate.confPath)).append(' ').append(shellQuote(candidate.backupPath)).append(';')
                        .append(" mv -- ").append(shellQuote(candidate.tempPath)).append(' ').append(shellQuote(candidate.confPath)).append(';');
            }
            replace.append(" echo __OK__");
            replacementStarted = true;
            try (Session replaceSession = ssh.startSession()) {
                String out = execAndRead(replaceSession, "sh -c " + quoteSh(replace.toString()) + " 2>&1");
                if (out == null || !out.contains("__OK__")) throw new IOException("替换 sing-box 配置失败: " + out);
            }
            if (activeChanged) restartSingBoxChecked(ssh);
            success = true;
        } catch (IOException | RuntimeException e) {
            if (ssh != null && replacementStarted) {
                try (Session rollback = ssh.startSession()) {
                    StringBuilder command = new StringBuilder();
                    for (DomainConfigCandidate candidate : candidates) {
                        command.append("if [ -f ").append(shellQuote(candidate.backupPath)).append(" ]; then cp -- ")
                                .append(shellQuote(candidate.backupPath)).append(' ').append(shellQuote(candidate.confPath)).append("; fi;");
                    }
                    execAndRead(rollback, "sh -c " + quoteSh(command.toString()) + " 2>&1");
                    rollbackSucceeded = true;
                } catch (Exception rollbackError) {
                    log.error("rollback domain whitelist configs failed: instanceId={}", instanceId, rollbackError);
                }
                if (activeChanged) {
                    try { restartSingBox(ssh); } catch (Exception ignored) {}
                }
            }
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e.getMessage(), e);
        } finally {
            if (ssh != null) {
                if (!candidates.isEmpty()) {
                    try (Session cleanup = ssh.startSession()) {
                        StringBuilder command = new StringBuilder("rm -f --");
                        for (DomainConfigCandidate candidate : candidates) command.append(' ').append(shellQuote(candidate.tempPath));
                        if (success || rollbackSucceeded) {
                            for (DomainConfigCandidate candidate : candidates) command.append(' ').append(shellQuote(candidate.backupPath));
                        }
                        execAndRead(cleanup, "sh -c " + quoteSh(command.toString()) + " 2>&1");
                    } catch (Exception ignored) {}
                }
                try { ssh.close(); } catch (IOException e) { log.debug("SSH close: {}", e.getMessage()); }
            }
        }
    }

    private String resolveProxyNodeConfigPath(SSHClient ssh, ProxyNode node) throws IOException {
        boolean disabled = "1".equals(node.getStatus());
        String expected = CONF_DIR + "/" + normalizeNodeBaseName(node.getNodeName())
                + (disabled ? ".json" + DISABLED_SUFFIX : ".json");
        String credentialId = extractNodeCredentialId(node.getConfigJson());
        StringBuilder script = new StringBuilder();
        script.append("if [ -f ").append(shellQuote(expected)).append(" ]; then printf '__CONF__%s\\n' ")
                .append(shellQuote(expected)).append("; else ")
                .append("for f in ").append(CONF_DIR).append("/*.json ").append(CONF_DIR).append("/*.json.disabled; do ")
                .append("[ -f \"$f\" ] || continue; ")
                .append("if ");
        if (StringUtils.isNotEmpty(credentialId)) {
            script.append("grep -Fq -- ").append(shellQuote(credentialId)).append(" \"$f\"");
        } else {
            script.append("grep -Eq ").append(shellQuote("\"listen_port\"[[:space:]]*:[[:space:]]*" + node.getPort() + "([^0-9]|$)"))
                    .append(" \"$f\"");
        }
        script.append("; then printf '__CONF__%s\\n' \"$f\"; fi; done; fi");
        String output;
        try (Session session = ssh.startSession()) {
            output = execAndRead(session, "sh -c " + quoteSh(script.toString()) + " 2>&1");
        }
        List<String> matches = new ArrayList<>();
        if (output != null) {
            for (String line : output.split("\\R")) {
                if (line.startsWith("__CONF__")) matches.add(line.substring("__CONF__".length()));
            }
        }
        if (matches.isEmpty()) throw new IOException("未找到节点配置文件，节点端口: " + node.getPort());
        if (matches.size() > 1) throw new IOException("节点端口匹配到多个配置文件: " + node.getPort());
        return matches.get(0);
    }

    private static String extractNodeCredentialId(String configJson) {
        if (StringUtils.isEmpty(configJson)) return null;
        try {
            JSONObject config = JSON.parseObject(configJson);
            String id = config != null ? config.getString("id") : null;
            return StringUtils.isNotEmpty(id) ? id.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class DomainConfigCandidate {
        private final boolean disabled;
        private final String confPath;
        private final String tempPath;
        private final String backupPath;

        private DomainConfigCandidate(ProxyNode node, String confPath, String operationToken) {
            this.disabled = "1".equals(node.getStatus());
            this.confPath = confPath;
            this.tempPath = confPath + ".skyway-domain-" + node.getId() + "-" + operationToken + ".tmp";
            this.backupPath = confPath + ".skyway-domain-" + node.getId() + "-" + operationToken + ".bak";
        }
    }

    private boolean isModernSingBox(SSHClient ssh) {
        try (Session session = ssh.startSession()) {
            String output = execAndRead(session, "sing-box version 2>/dev/null || true");
            Matcher matcher = Pattern.compile("(?i)version\\s+([0-9]+)\\.([0-9]+)").matcher(output != null ? output : "");
            if (!matcher.find()) return true;
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            return major > 1 || (major == 1 && minor >= 11);
        } catch (Exception ignored) {
            return true;
        }
    }

    private void writeAndValidateCandidate(SSHClient ssh, String tempPath, String content) throws IOException {
        try (Session writeSession = ssh.startSession()) {
            writeRemoteTextFile(writeSession, tempPath, content);
        }
        try (Session checkSession = ssh.startSession()) {
            String command = "sing-box check -c " + shellQuote(tempPath) + " 2>&1; rc=$?; echo __RC__$rc";
            String output = execAndRead(checkSession, "sh -c " + quoteSh(command));
            if (output == null || !output.contains("__RC__0")) {
                throw new IOException("sing-box 配置校验失败: " + (output != null ? output.replaceAll("__RC__\\d+", "").trim() : "无输出"));
            }
        }
    }

    private void restartSingBoxChecked(SSHClient ssh) throws IOException {
        try (Session session = ssh.startSession()) {
            String command = "if command -v systemctl >/dev/null 2>&1 && systemctl cat sing-box >/dev/null 2>&1; then "
                    + "systemctl restart sing-box >/tmp/skyway-singbox-restart.log 2>&1; rc=$?; "
                    + "else printf '5\\n3\\n' | sb >/tmp/skyway-singbox-restart.log 2>&1; rc=$?; fi; "
                    + "if command -v systemctl >/dev/null 2>&1; then systemctl is-active --quiet sing-box || rc=1; fi; "
                    + "cat /tmp/skyway-singbox-restart.log; echo __RC__$rc";
            String output = execAndReadWithTimeout(session, "sh -c " + quoteSh(command), 45);
            if (output == null || !output.contains("__RC__0")) {
                throw new IOException("sing-box 重启或状态检查失败: " + (output != null ? output.replaceAll("__RC__\\d+", "").trim() : "无输出"));
            }
        }
    }

    private static void writeRemoteTextFile(Session session, String path, String content) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String command = "printf '%s' " + shellQuote(encoded) + " | base64 -d > " + shellQuote(path);
        String out = execAndRead(session, "sh -c " + quoteSh(command) + " 2>&1");
        if (out != null && (out.contains("Permission denied") || out.toLowerCase().contains("base64"))) {
            throw new IOException("写入 sing-box 配置失败: " + out.trim());
        }
    }

    /**
     * 执行命令并读取输出，带超时。超时后关闭 session 以解除阻塞，并抛出 IOException。
     * 用于 sb 等可能卡住不结束的命令（如等待 TTY 输入）。
     */
    private static String execAndReadWithTimeout(Session session, String command, int timeoutSeconds) throws IOException {
        java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<IOException> err = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        Thread worker = new Thread(() -> {
            try (Command cmd = session.exec(command)) {
                InputStream in = cmd.getInputStream();
                byte[] buf = new byte[4096];
                StringBuilder sb = new StringBuilder();
                int n;
                while ((n = in.read(buf)) > 0) {
                    sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
                cmd.join(5, TimeUnit.SECONDS);
                result.set(sb.toString());
            } catch (IOException e) {
                err.set(e);
            } finally {
                done.countDown();
            }
        }, "ssh-exec-timeout");
        worker.setDaemon(true);
        worker.start();
        try {
            if (!done.await(timeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    session.close();
                } catch (IOException ignored) {}
                worker.join(3000);
                throw new IOException("SSH 命令执行超时（" + timeoutSeconds + " 秒），请检查服务器状态后重试");
            }
            if (err.get() != null) throw err.get();
            return result.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("执行被中断", e);
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
    private static final Pattern P_VMESS_URL = Pattern.compile("(vmess://[^\\s]+)");
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]|\\[[0-9;]+m");

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
            try { port = Integer.parseInt(portStr.trim()); } catch (NumberFormatException ignored) {}
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
        if (id == null || id.isEmpty()) id = stripAnsi(match1(P_ID, output));
        if (id == null || id.isEmpty()) return null;
        if (url == null || url.isEmpty()) {
            String aidStr = stripAnsi(match1(Pattern.compile("alterId\\s*[=：:]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE), output));
            if (aidStr != null && !aidStr.isEmpty()) {
                try { aid = Integer.parseInt(aidStr.trim()); } catch (NumberFormatException ignored) {}
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

    /**
     * 在指定实例上添加节点（执行 sb、重命名、解析），返回可入库的节点对象。支持 VLESS-REALITY、VMess-TCP。
     */
    public ProxyNode addProxyNodeOnInstance(Long instanceId, Long customerId, int port, String expireTimeStr, String nodeType) throws IOException {
        return addProxyNodeOnInstance(instanceId, customerId, port, expireTimeStr, nodeType, null);
    }

    /**
     * 在指定实例上添加节点（执行 sb、重命名、解析），返回可入库的节点对象。支持 VLESS-REALITY、VMess-TCP。
     * relay 仅对 VLESS-REALITY 生效；为空时保持原创建流程不变。
     */
    public ProxyNode addProxyNodeOnInstance(Long instanceId, Long customerId, int port, String expireTimeStr, String nodeType,
                                            Socks5RelayConfig relay) throws IOException {
        if (customerId == null) throw new IllegalArgumentException("请选择归属客户");
        if (nodeType == null || nodeType.isEmpty()) nodeType = "VLESS-REALITY";
        if (!"VLESS-REALITY".equals(nodeType) && !"VMess-TCP".equals(nodeType)) {
            throw new IllegalArgumentException("不支持的协议类型: " + nodeType);
        }
        if (relay != null && !"VLESS-REALITY".equals(nodeType)) {
            throw new IllegalArgumentException("当前仅 VLESS-REALITY 支持 SOCKS5 中转");
        }
        SSHClient ssh = null;
        try {
            ssh = createSshClient(instanceId);
            try (Session checkSession = ssh.startSession()) {
                String sbCheck = execAndRead(checkSession, "command -v sb 2>/dev/null || which sb 2>/dev/null || echo ''");
                if (sbCheck == null || sbCheck.trim().isEmpty()) {
                    throw new IllegalStateException("未检测到 sing-box 脚本 (sb)，请先在服务器上安装 sing-box");
                }
            }
            // 在真正执行 sb 前再次检查数据库以外的系统端口、配置和 Docker 映射。
            assertRemotePortAvailable(ssh, port);
            String runCmd;
            String oldJsonName;
            String typeLabel;
            if ("VMess-TCP".equals(nodeType)) {
                typeLabel = "VMess-TCP";
                oldJsonName = "VMess-TCP-" + port + ".json";
                runCmd = "printf '1\\n5\\n" + port + "\\n' | sb";
            } else {
                typeLabel = "VLESS-REALITY";
                oldJsonName = "VLESS-REALITY-" + port + ".json";
                runCmd = "printf '1\\n18\\n" + port + "\\n' | sb";
            }
            String output;
            try (Session runSession = ssh.startSession()) {
                String toRun = "sh -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                output = execAndReadWithTimeout(runSession, toRun, 25);
            }
            if (output == null) output = "";
            output = stripAnsi(output);
            if (output.contains("无法使用") && output.contains("端口")) {
                throw new IllegalStateException("端口 (" + port + ") 已被占用，请选择其他端口重试");
            }
            ProxyNode parsed = "VMess-TCP".equals(nodeType) ? parseSbVmessTcpOutput(output, port) : parseSbVlessRealityOutput(output, port);
            if (parsed == null || parsed.getUrl() == null || parsed.getUrl().isEmpty()) {
                throw new IllegalStateException("解析 sb 输出失败，请检查服务器上 sing-box 是否正常");
            }
            Date expireDate = parseExpireTime(expireTimeStr);
            String customPart = String.valueOf(customerId);
            String targetBaseName = buildNodeBaseName(typeLabel, parsed.getAddress(), port, customPart, expireDate);
            String newJsonName = targetBaseName + ".json";
            String mvCmd = "mv " + CONF_DIR + "/" + oldJsonName + " " + CONF_DIR + "/" + newJsonName + " 2>&1";
            try (Session mvSession = ssh.startSession()) {
                String mvOut = execAndRead(mvSession, mvCmd);
                if (mvOut != null && mvOut.toLowerCase().contains("no such file")) {
                    throw new IOException("重命名失败: 未找到 " + oldJsonName);
                }
            }
            if (relay != null) {
                String confPath = CONF_DIR + "/" + newJsonName;
                applySocks5RelayToRemoteConfig(ssh, confPath, relay);
                JSONObject config = JSON.parseObject(parsed.getConfigJson());
                config.put("relay", relay.toConfigJson());
                parsed.setConfigJson(config.toJSONString());
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

    private static String buildNodeBaseName(String nodeType, String address, Integer port, String customerId, Date expireDate) {
        String typePart = StringUtils.isNotEmpty(nodeType) ? nodeType.trim() : "UNKNOWN";
        String addressPart = StringUtils.isNotEmpty(address) ? address.trim() : "unknown";
        String portPart = port != null ? String.valueOf(port) : "0";
        String customerPart = StringUtils.isNotEmpty(customerId) ? customerId.trim() : "0";
        String expiryTag = expireDate == null ? "permanent" : new SimpleDateFormat("yyyyMMdd").format(expireDate);
        return typePart + "-" + addressPart + "-" + portPart + "-" + customerPart + "-" + expiryTag;
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
                String toRun = "sh -c \"" + runCmd.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
    /** CentOS 等系统 iptables 常在 /sbin，SSH 非登录 shell 的 PATH 可能不包含，此处统一加上 */
    private static final String PATH_PREFIX = "PATH=/usr/sbin:/sbin:$PATH; ";
    private static final String CHAIN_IN = "NODE_TRAFFIC_IN";
    private static final String CHAIN_OUT = "NODE_TRAFFIC_OUT";
    // 兼容 Ubuntu 与 CentOS：前两列为 pkts/bytes，端口为 dpt:PORT 或 dpt: PORT（允许空格）
    private static final Pattern IPTABLES_LINE = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+.*?dpt:\\s*(\\d+)");
    private static final Pattern IPTABLES_LINE_OUT = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+.*?spt:\\s*(\\d+)");
    // 部分环境行首为 target（ACCEPT），pkts/bytes 紧跟其后：ACCEPT  0  0  tcp ... dpt:PORT
    private static final Pattern IPTABLES_LINE_ACCEPT_IN = Pattern.compile("ACCEPT\\s+(\\d+)\\s+(\\d+)\\s+.*?dpt:\\s*(\\d+)");
    private static final Pattern IPTABLES_LINE_ACCEPT_OUT = Pattern.compile("ACCEPT\\s+(\\d+)\\s+(\\d+)\\s+.*?spt:\\s*(\\d+)");
    private static final Pattern IPTABLES_PORT_ONLY_IN = Pattern.compile("dpt:\\s*(\\d+)");
    private static final Pattern IPTABLES_PORT_ONLY_OUT = Pattern.compile("spt:\\s*(\\d+)");

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
                String detect = execAndRead(detectSession, "sh -c '" + PATH_PREFIX + "command -v iptables >/dev/null 2>&1 && echo iptables; command -v nft >/dev/null 2>&1 && echo nft'");
                boolean hasIptables = detect != null && detect.contains("iptables");
                boolean hasNft = detect != null && detect.contains("nft");
                // 两者都存在时优先 iptables（CentOS 8/Ubuntu 上兼容更好）；仅 nft 时用 nft
                useNft = hasNft && !hasIptables;
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
        // 先删再插到链首；PATH 保证 CentOS 能找到 iptables；整段 2>&1 便于收错；先试 sudo -n，失败再试直接执行
        String block = "( " + PATH_PREFIX +
            "iptables -N " + CHAIN_IN + " 2>/dev/null; iptables -N " + CHAIN_OUT + " 2>/dev/null; " +
            "iptables -D INPUT -p tcp -j " + CHAIN_IN + " 2>/dev/null; iptables -I INPUT 1 -p tcp -j " + CHAIN_IN + "; " +
            "iptables -D OUTPUT -p tcp -j " + CHAIN_OUT + " 2>/dev/null; iptables -I OUTPUT 1 -p tcp -j " + CHAIN_OUT + "; " +
            addIn + "; " + addOut + " ) 2>&1";
        String out = execAndRead(session, "sudo -n sh -c " + quoteSh(block));
        boolean usedFallback = out != null && (out.contains("password") || out.contains("sudo:") || out.contains("not allowed"));
        if (usedFallback) {
            out = execAndRead(session, "sh -c " + quoteSh(block));
        }
        if (out != null && !out.isEmpty() && (out.contains("Permission denied") || out.contains("Operation not permitted"))) {
            log.warn("ensureTrafficRulesIptables port={} failed (need root or sudo): {}", port, out.trim().split("\\r?\\n")[0]);
        }
    }

    private static String quoteSh(String s) {
        if (s == null) return "''";
        return "'" + s.replace("'", "'\"'\"'") + "'";
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
        execAndRead(session, "sh -c '" + create + cleanIn + "; " + cleanOut + "; " + addIn + "; " + addOut + "'");
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
                String detect = execAndRead(detectSession, "sh -c '" + PATH_PREFIX + "command -v iptables >/dev/null 2>&1 && echo iptables; command -v nft >/dev/null 2>&1 && echo nft'");
                boolean hasIptables = detect != null && detect.contains("iptables");
                boolean hasNft = detect != null && detect.contains("nft");
                useNft = hasNft && !hasIptables;
            }
            try (Session dataSession = ssh.startSession()) {
                if (useNft) {
                    String del = "sh -c 'while nft delete rule inet node_traffic in tcp dport " + port + " counter accept 2>/dev/null; do :; done; " +
                        "while nft delete rule inet node_traffic out tcp sport " + port + " counter accept 2>/dev/null; do :; done'";
                    execAndRead(dataSession, del);
                } else {
                    execAndRead(dataSession, "sh -c '" + PATH_PREFIX + "iptables -D " + CHAIN_IN + " -p tcp --dport " + port + " -j ACCEPT 2>/dev/null; iptables -D " + CHAIN_OUT + " -p tcp --sport " + port + " -j ACCEPT 2>/dev/null 2>&1'");
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
                String detect = execAndRead(detectSession, "sh -c '" + PATH_PREFIX + "command -v iptables >/dev/null 2>&1 && echo iptables; command -v nft >/dev/null 2>&1 && echo nft'");
                boolean hasIptables = detect != null && detect.contains("iptables");
                boolean hasNft = detect != null && detect.contains("nft");
                useNft = hasNft && !hasIptables;
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

    private static final String CHAIN_DELIM = "___CHAIN_OUT___";
    private void readTrafficCountersIptables(Session session, Map<Integer, TrafficPair> result) throws IOException {
        // 单次命令同时拉两条链，避免同一 session 第二次 exec 失败导致 outOut=null（见 debug 日志 after_exec outNull:true）
        String cmd = "sh -c '" + PATH_PREFIX + "iptables -L " + CHAIN_IN + " -v -n -x 2>&1; echo " + CHAIN_DELIM + "; iptables -L " + CHAIN_OUT + " -v -n -x 2>&1'";
        String combined = execAndRead(session, cmd);
        String inOut = null;
        String outOut = null;
        if (combined != null && combined.contains(CHAIN_DELIM)) {
            int idx = combined.indexOf(CHAIN_DELIM);
            inOut = combined.substring(0, idx).trim();
            outOut = combined.substring(idx + CHAIN_DELIM.length()).trim();
        } else if (combined != null && combined.contains("Permission denied")) {
            cmd = "sh -c '" + PATH_PREFIX + "sudo -n iptables -L " + CHAIN_IN + " -v -n -x 2>&1; echo " + CHAIN_DELIM + "; sudo -n iptables -L " + CHAIN_OUT + " -v -n -x 2>&1'";
            combined = execAndRead(session, cmd);
            if (combined != null && combined.contains(CHAIN_DELIM)) {
                int idx = combined.indexOf(CHAIN_DELIM);
                inOut = combined.substring(0, idx).trim();
                outOut = combined.substring(idx + CHAIN_DELIM.length()).trim();
            }
        } else if (combined != null) {
            inOut = combined.trim();
        }
        String inFirst = inOut != null && inOut.length() > 0 ? inOut.split("\\r?\\n")[0].trim() : null;
        String outFirst = outOut != null && outOut.length() > 0 ? outOut.split("\\r?\\n")[0].trim() : null;
        boolean inNoChain = inOut != null && (inOut.contains("No chain") || inOut.contains("does not exist"));
        boolean outNoChain = outOut != null && (outOut.contains("No chain") || outOut.contains("does not exist"));
        if (inOut != null && inNoChain && result.isEmpty()) {
            log.debug("readTrafficCountersIptables: chains missing (ensureTrafficRules may have failed), inOut snippet: {}", inFirst);
        }
        if (inOut != null) {
            for (String line : inOut.split("\\r?\\n")) {
                int port = -1;
                long bytes = 0L;
                Matcher m = IPTABLES_LINE.matcher(line);
                if (m.find()) {
                    port = Integer.parseInt(m.group(3));
                    bytes = Long.parseLong(m.group(2));
                } else {
                    m = IPTABLES_LINE_ACCEPT_IN.matcher(line);
                    if (m.find()) {
                        port = Integer.parseInt(m.group(3));
                        bytes = Long.parseLong(m.group(2));
                    } else {
                        m = IPTABLES_PORT_ONLY_IN.matcher(line);
                        if (m.find()) port = Integer.parseInt(m.group(1));
                    }
                }
                if (port > 0) result.computeIfAbsent(port, p -> new TrafficPair(0, 0)).rx = bytes;
            }
        }
        if (outOut != null) {
            for (String line : outOut.split("\\r?\\n")) {
                int port = -1;
                long bytes = 0L;
                Matcher m = IPTABLES_LINE_OUT.matcher(line);
                if (m.find()) {
                    port = Integer.parseInt(m.group(3));
                    bytes = Long.parseLong(m.group(2));
                } else {
                    m = IPTABLES_LINE_ACCEPT_OUT.matcher(line);
                    if (m.find()) {
                        port = Integer.parseInt(m.group(3));
                        bytes = Long.parseLong(m.group(2));
                    } else {
                        m = IPTABLES_PORT_ONLY_OUT.matcher(line);
                        if (m.find()) port = Integer.parseInt(m.group(1));
                    }
                }
                if (port > 0) result.computeIfAbsent(port, p -> new TrafficPair(0, 0)).tx = bytes;
            }
        }
    }

    private void readTrafficCountersNft(Session session, Map<Integer, TrafficPair> result) throws IOException {
        String full = execAndRead(session, "nft list table inet node_traffic 2>&1");
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
