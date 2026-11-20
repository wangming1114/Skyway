package com.skyway.web.service.ws;

import com.alibaba.fastjson2.*;
import com.skyway.common.utils.DateUtils;
import com.skyway.resource.domain.*;
import com.skyway.resource.service.*;
import com.skyway.web.service.*;
import com.skyway.web.service.ws.WsStore.Domain;
import com.skyway.web.service.ws.WsStore.Operation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;
import net.schmizz.sshj.SSHClient;
import org.slf4j.*;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class WsNodeService {
    private static final Logger log = LoggerFactory.getLogger(WsNodeService.class);
    private final WsStore store;
    private final CloudflareDns dns;
    private final WsRemote remote;
    private final WsCertificates certificates;
    private final IVpsInstanceService instances;
    private final VpsPortAvailabilityService ports;
    private final VpsInstanceOperationCoordinator locks;
    private final ProxyDomainWhitelistService policies;
    private final IProxyNodeTrafficService traffic;
    private final IProxyNodeRateLimitService limits;
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final Set<String> submitted = ConcurrentHashMap.newKeySet();

    public WsNodeService(
            WsStore store,
            CloudflareDns dns,
            WsRemote remote,
            WsCertificates certificates,
            IVpsInstanceService instances,
            VpsPortAvailabilityService ports,
            VpsInstanceOperationCoordinator locks,
            ProxyDomainWhitelistService policies,
            IProxyNodeTrafficService traffic,
            IProxyNodeRateLimitService limits) {
        this.store = store;
        this.dns = dns;
        this.remote = remote;
        this.certificates = certificates;
        this.instances = instances;
        this.ports = ports;
        this.locks = locks;
        this.policies = policies;
        this.traffic = traffic;
        this.limits = limits;
    }

    @PreDestroy
    public void close() {
        workers.shutdownNow();
    }

    public static boolean managed(ProxyNode node) {
        return node != null
                && ProxyNodeProtocol.WS_TLS.equals(node.getNodeType())
                && node.getWsKey() != null
                && node.getListenPort() != null;
    }

    public String prefix(String address) {
        String root = "." + dns.root();
        return address.endsWith(root)
                ? address.substring(0, address.length() - root.length())
                : address;
    }

    public VpsSshCommandService.PortRateLimitRemoteResult rate(
            ProxyNode node, int download, int upload) throws IOException {
        try (SSHClient ssh = remote.connect(node.getInstanceId())) {
            String port = node.getListenPort().toString();
            if (download == 0 && upload == 0) remote.run(ssh, "unlimit", port);
            else remote.run(ssh, "limit", port, String.valueOf(download), String.valueOf(upload));
            return new VpsSshCommandService.PortRateLimitRemoteResult(
                    node.getListenPort(), "本机端口限速已更新");
        }
    }

    public JSONObject submit(
            String kind, long instance, Long node, Map<String, ?> body, String actor) {
        JSONObject request = new JSONObject();
        for (String field :
                Arrays.asList(
                        "subdomain",
                        "port",
                        "customerId",
                        "expireTime",
                        "remark",
                        "relayEnabled",
                        "relayText",
                        "domainPolicy",
                        "domainWhitelist",
                        "status",
                        "cloudflareProxied")) {
            if (body.containsKey(field)) request.put(field, body.get(field));
        }
        String id =
                body.get("operationId") == null
                        ? WsStore.uuid()
                        : String.valueOf(body.get("operationId"));
        Operation op = store.submit(id, kind, instance, node, request, actor);
        if ("FAILED".equals(op.state)) {
            store.jdbc().update(
                    "UPDATE res_ws_operation SET state='QUEUED',stage='重新执行',attempts=0,next_run=NOW(),context_json='{}' WHERE id=? AND state='FAILED'",
                    id);
        }
        dispatch(id);
        return status(id, actor);
    }

    public JSONObject status(String id, String actor) {
        Operation op = store.operation(id);
        if (op == null || !op.actor.equals(actor)) throw new IllegalArgumentException("操作不存在");
        JSONObject result = new JSONObject();
        result.put("operationId", id);
        result.put("state", op.state);
        result.put("stage", op.stage);
        if ("DONE".equals(op.state)
                && op.node != null
                && ("CREATE".equals(op.kind) || "EDIT".equals(op.kind))) {
            ProxyNode node = store.nodeService().getById(op.node);
            policies.hydrate(node);
            result.put("node", node);
        }
        return result;
    }

    private void dispatch(String id) {
        if (submitted.add(id))
            workers.execute(
                    () -> {
                        try {
                            run(id);
                        } catch (Exception error) {
                            log.warn("WS 操作 {} 将重试", id, error);
                        } finally {
                            submitted.remove(id);
                        }
                    });
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void recover() {
        for (String id : store.due()) dispatch(id);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void maintain() {
        List<Map<String, Object>> rows =
                store.jdbc().queryForList(
                        "SELECT d.id,d.instance_id,n.id node_id,d.renew_at,d.origin,v.ip "
                                + "FROM res_ws_domain d JOIN res_proxy_node n ON n.ws_key=d.node_key AND n.address=d.hostname "
                                + "JOIN res_instance v ON v.id=d.instance_id WHERE d.state='ACTIVE' "
                                + "AND (d.renew_at<=NOW() OR NOT (INET6_ATON(d.origin)<=>INET6_ATON(v.ip))) LIMIT 64");
        for (Map<String, Object> row : rows) {
            String id =
                    stableId(
                            "maintain:"
                                    + row.get("id")
                                    + ":"
                                    + row.get("renew_at")
                                    + ":"
                                    + row.get("ip")
                                    + ":"
                                    + row.get("origin"));
            Operation op =
                    store.submit(
                            id,
                            "MAINTAIN",
                            ((Number) row.get("instance_id")).longValue(),
                            ((Number) row.get("node_id")).longValue(),
                            new JSONObject(),
                            "system");
            if (!"DONE".equals(op.state)) dispatch(id);
        }
    }

    public static String stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void run(String id) {
        Operation first = store.operation(id);
        if (first == null) return;
        try (VpsInstanceOperationCoordinator.LockHandle ignored = locks.lock(first.instance)) {
            store.withInstanceLock(
                    first.instance,
                    () -> {
                        Operation op = store.operation(id);
                        if ("FAILED".equals(op.state) || "DONE".equals(op.state)) return;
                        try {
                            if ("EDIT".equals(op.kind)
                                    || "MAINTAIN".equals(op.kind)
                                    || "CREATE".equals(op.kind)) {
                                int removing =
                                        store.jdbc().queryForObject(
                                                "SELECT COUNT(*) FROM res_ws_operation WHERE instance_id=? "
                                                        + "AND (kind='FORCE_VPS' OR (node_id=? AND kind IN ('DELETE','FORCE'))) "
                                                        + "AND state NOT IN ('DONE','FAILED')",
                                                Integer.class,
                                                op.instance,
                                                op.node);
                                if (removing > 0) {
                                    store.jdbc().update(
                                            "UPDATE res_ws_operation SET next_run=TIMESTAMPADD(SECOND,30,NOW()) WHERE id=?",
                                            op.id);
                                    return;
                                }
                            }
                            if (op.node != null
                                    && ("EDIT".equals(op.kind) || "MAINTAIN".equals(op.kind))) {
                                Integer earlier =
                                        store.jdbc().queryForObject(
                                                "SELECT COUNT(*) FROM res_ws_operation WHERE node_id=? "
                                                        + "AND kind='EDIT' AND state NOT IN ('DONE','FAILED') AND sequence_no < "
                                                        + "(SELECT sequence_no FROM res_ws_operation WHERE id=?)",
                                                Integer.class,
                                                op.node,
                                                op.id);
                                if (earlier > 0) {
                                    store.jdbc().update(
                                            "UPDATE res_ws_operation SET next_run=TIMESTAMPADD(SECOND,10,NOW()) WHERE id=?",
                                            op.id);
                                    return;
                                }
                            }
                            switch (op.kind) {
                                case "CREATE":
                                    create(op);
                                    break;
                                case "EDIT":
                                    edit(op);
                                    break;
                                case "DELETE":
                                case "FORCE":
                                    remove(op);
                                    break;
                                case "FORCE_VPS":
                                    removeVps(op);
                                    break;
                                case "MAINTAIN":
                                    maintainNode(op);
                                    break;
                                default:
                                    throw new IllegalStateException("未知操作类型");
                            }
                        } catch (Exception ex) {
                            String detail = ex.getMessage() == null ? "后台操作失败" : ex.getMessage();
                            boolean persistent =
                                    op.kind.startsWith("FORCE")
                                            || "DELETE".equals(op.kind)
                                            || "MAINTAIN".equals(op.kind)
                                            || op.context.getBooleanValue("committed");
                            boolean retry =
                                    persistent
                                            || !(ex instanceof IllegalArgumentException)
                                                    && op.attempts < 2;
                            if (!retry) {
                                try {
                                    compensate(op);
                                } catch (Exception cleanup) {
                                    op.context.put("aborting", detail);
                                    store.progress(op, "等待清理未完成的操作");
                                    retry = true;
                                }
                            }
                            store.error(op, detail, retry);
                        }
                    });
        }
    }

    private ProxyNode instanceNode(Operation op) {
        ProxyNode node = store.nodeService().getById(op.node);
        if (!managed(node) || node.getInstanceId() != op.instance)
            throw new IllegalArgumentException("托管节点不存在");
        return node;
    }

    private String ip(long id) {
        VpsInstance instance = instances.getById(id);
        if (instance == null) throw new IllegalArgumentException("VPS 不存在");
        return CloudflareDns.origin(instance.getIp());
    }

    private void checkPublicPort(Operation op, int port, boolean proxied) {
        VlessWsTls.checkPort(port, proxied);
        int reserved =
                store.jdbc().queryForObject(
                        "SELECT (SELECT COUNT(*) FROM res_proxy_node WHERE instance_id=? AND "
                                + "(listen_port=? OR (ws_key IS NULL AND port=? AND node_type NOT IN ('Hysteria2','TUIC')))) "
                                + "+ (SELECT COUNT(*) FROM res_ws_operation WHERE instance_id=? AND reserved_port=?)",
                        Integer.class,
                        op.instance,
                        port,
                        port,
                        op.instance,
                        port);
        if (reserved > 0) throw new IllegalArgumentException("公网端口已由其他入站预留");
    }

    private static Date expiry(JSONObject req) {
        String value = req.getString("expireTime");
        if (value == null || value.trim().isEmpty()) return null;
        Date parsed = DateUtils.parseDate(value);
        if (parsed == null) throw new IllegalArgumentException("节点有效期无效");
        return parsed;
    }

    private static String subscriptionToken() {
        return (WsStore.uuid() + WsStore.uuid()).replace("-", "");
    }

    private static void applyFields(ProxyNode node, JSONObject req, JSONObject config) {
        if (req.containsKey("customerId")) node.setCustomerId(req.getLong("customerId"));
        if (node.getCustomerId() == null || node.getCustomerId() <= 0)
            throw new IllegalArgumentException("请选择归属客户");
        node.setCustomId(String.valueOf(node.getCustomerId()));
        if (req.containsKey("expireTime")) node.setExpireTime(expiry(req));
        if (req.containsKey("remark")) node.setRemark(req.getString("remark"));
        if (req.containsKey("status")) {
            String status = req.getString("status");
            if (!"0".equals(status) && !"1".equals(status))
                throw new IllegalArgumentException("节点状态无效");
            node.setStatus(status);
        }
        if (req.containsKey("relayEnabled") && !req.getBooleanValue("relayEnabled")) {
            if (config.remove("relay") != null) node.setRemark("");
        } else if (req.getString("relayText") != null
                && !req.getString("relayText").trim().isEmpty()) {
            VpsSshCommandService.Socks5RelayConfig relay =
                    VpsSshCommandService.parseSocks5RelayText(req.getString("relayText").trim());
            config.put("relay", relay.toConfigJson());
            node.setRemark(req.getString("relayText").trim());
        }
    }

    private Domain bind(Operation op, String key, String prefix, String origin, boolean proxied)
            throws Exception {
        Domain d = store.allocated(op.id);
        boolean auto = prefix == null || prefix.trim().isEmpty();
        for (int attempt = 0; attempt < 8; attempt++) {
            if (d == null) {
                String host =
                        VlessWsTls.hostname(
                                auto
                                        ? "ws-" + WsStore.uuid().substring(0, 12).replace("-", "")
                                        : prefix,
                                dns.root());
                try {
                    d = store.reserve(op, key, host, dns.zone(), origin, proxied);
                } catch (DuplicateKeyException collision) {
                    if (auto) continue;
                    throw new CloudflareDns.Conflict();
                }
            }
            try {
                store.confirmed(d, dns.apply(d, origin, proxied), origin, proxied);
                return d;
            } catch (CloudflareDns.Conflict conflict) {
                if (!auto || d.record != null) throw conflict;
                store.jdbc().update("DELETE FROM res_ws_domain WHERE id=? AND record_id IS NULL", d.id);
                d = null;
            }
        }
        throw new IllegalArgumentException("无法分配空闲子域名");
    }

    private boolean aborting(Operation op) throws Exception {
        String reason = op.context.getString("aborting");
        if (reason == null) return false;
        compensate(op);
        store.error(op, reason, false);
        return true;
    }

    private void prepareSignerForNewDomain(Operation op) throws IOException {
        if (store.allocated(op.id) == null) {
            store.progress(op, "正在检查管理端证书工具");
            certificates.signer();
        }
    }

    private void create(Operation op) throws Exception {
        if (aborting(op)) return;
        JSONObject req = op.request, ctx = op.context;
        int port = req.getInteger("port") == null ? 443 : req.getIntValue("port");
        checkPublicPort(op, port, false);
        String origin = ip(op.instance);
        ProxyNode node = new ProxyNode();
        node.setWsKey(op.id);
        node.setNodeType(ProxyNodeProtocol.WS_TLS);
        node.setInstanceId(op.instance);
        node.setNodeName("VLESS-WS-TLS-" + op.id);
        node.setStatus("0");
        node.setPort(port);
        JSONObject config = new JSONObject();
        applyFields(node, req, config);
        ProxyNodeDomainWhitelist policy = policies.resolveRequest(req);
        node.setDomainPolicyJson(policies.serialize(policy));
        prepareSignerForNewDomain(op);
        store.progress(op, "正在创建 Cloudflare DNS 记录");
        Domain domain = bind(op, op.id, req.getString("subdomain"), origin, false);
        try (SSHClient ssh = remote.connect(op.instance)) {
            remote.run(ssh, "preflight", String.valueOf(port));
            if (!ctx.containsKey("uuid")) {
                ctx.put("uuid", WsStore.uuid());
                ctx.put("path", "/" + WsStore.uuid());
                int listen = ports.resolveAutoPortForCreate(op.instance, null);
                if (listen == port)
                    listen = ports.recommend(op.instance, Collections.singleton(port)).getPort();
                ports.assertAvailableForCreate(op.instance, listen);
                ctx.put("listen", listen);
                store.jdbc().update(
                        "UPDATE res_ws_operation SET reserved_port=?, context_json=? WHERE id=?",
                        listen,
                        ctx.toJSONString(),
                        op.id);
            }
            node.setListenPort(ctx.getInteger("listen"));
            node.setAddress(domain.host);
            config.put("id", ctx.getString("uuid"));
            config.put("path", ctx.getString("path"));
            config.put("protocol", "vless");
            config.put("network", "ws");
            config.put("security", "tls");
            endpoint(node, config, domain.host, false);
            ctx.put("remoteTouched", true);
            store.progress(op, "正在签发源站 TLS 证书");
            Instant renew = certificates.ensure(ssh, domain);
            store.progress(op, "正在配置 Caddy 和 sing-box");
            remote.inbound(ssh, node, policy);
            remote.site(ssh, domain, node);
            store.progress(op, "正在验证 TLS 和 WebSocket 握手");
            remote.probe(ssh, domain, node);
            remote.probePublic(node, origin, false);
            remote.run(ssh, "counters", node.getListenPort().toString());
            node.setSubscriptionToken(subscriptionToken());
            node.setCreateBy(op.actor);
            store.transactions().execute(
                    status -> {
                        if (store.nodeService().insert(node) != 1)
                            throw new IllegalStateException("节点保存失败");
                        active(domain, renew);
                        store.done(op, node.getId());
                        return null;
                    });
        }
    }

    private void active(Domain d, Instant renew) {
        store.jdbc().update(
                "UPDATE res_ws_domain SET state='ACTIVE',renew_at=? WHERE id=?",
                Timestamp.from(renew),
                d.id);
    }

    private static void endpoint(ProxyNode node, JSONObject config, String host, boolean proxied) {
        node.setAddress(host);
        config.put("host", host);
        config.put("sni", host);
        config.put("cloudflareProxied", proxied);
        node.setConfigJson(config.toJSONString());
        node.setUrl(VlessWsTls.share(node, config));
    }

    private Domain current(ProxyNode node) {
        return store.domains(node.getWsKey()).stream()
                .filter(d -> d.host.equals(node.getAddress()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("托管域名绑定丢失"));
    }

    private void edit(Operation op) throws Exception {
        if (aborting(op)) return;
        ProxyNode old = instanceNode(op);
        if (op.context.getBooleanValue("committed")) {
            finishEdit(op, old);
            return;
        }
        ProxyNode node = new ProxyNode();
        BeanUtils.copyProperties(old, node);
        JSONObject config = JSON.parseObject(old.getConfigJson()), req = op.request;
        boolean previousProxy = config.getBooleanValue("cloudflareProxied");
        boolean proxied =
                req.containsKey("cloudflareProxied")
                        ? req.getBooleanValue("cloudflareProxied")
                        : previousProxy;
        if (req.containsKey("port")) node.setPort(req.getInteger("port"));
        if (node.getPort() == null) throw new IllegalArgumentException("请输入公网端口");
        checkPublicPort(op, node.getPort(), proxied);
        Domain oldDomain = current(old);
        String label = req.getString("subdomain");
        boolean rename =
                label != null
                        && !oldDomain
                                .host
                                .substring(0, oldDomain.host.indexOf('.'))
                                .equalsIgnoreCase(label.trim())
                        && !oldDomain.host.equals(VlessWsTls.hostname(label, dns.root()));
        JSONObject previousRelay = config.getJSONObject("relay");
        applyFields(node, req, config);
        boolean changeInbound =
                !Objects.equals(previousRelay, config.getJSONObject("relay"))
                        || !old.getStatus().equals(node.getStatus());
        ProxyNodeDomainWhitelist policy = policies.parseStored(old.getDomainPolicyJson());
        if (req.containsKey("domainPolicy") || req.containsKey("domainWhitelist")) {
            policy = policies.resolveRequest(req);
            node.setDomainPolicyJson(policies.serialize(policy));
            changeInbound |= !Objects.equals(node.getDomainPolicyJson(), old.getDomainPolicyJson());
        }
        if (!old.getCustomerId().equals(node.getCustomerId()))
            node.setSubscriptionToken(subscriptionToken());
        node.setUpdateBy(op.actor);
        String origin = ip(op.instance);
        if (rename) prepareSignerForNewDomain(op);
        store.progress(op, "正在更新节点及托管记录");
        Domain target = rename ? bind(op, old.getWsKey(), label, origin, proxied) : oldDomain;
        endpoint(node, config, target.host, proxied);
        boolean siteChanged = rename || !old.getPort().equals(node.getPort());
        Instant renew = null;
        if (siteChanged || changeInbound) {
            op.context.put("remoteTouched", true);
            store.progress(op, "正在更新源站配置");
            try (SSHClient ssh = remote.connect(op.instance)) {
                remote.run(ssh, "preflight", node.getPort().toString());
                if (siteChanged) renew = certificates.ensure(ssh, target);
                if (changeInbound) remote.inbound(ssh, node, policy);
                if (siteChanged) remote.site(ssh, target, node);
                remote.probe(ssh, target, node);
                if (siteChanged && (rename || previousProxy == proxied)) {
                    store.progress(op, "正在验证新公网地址的 TLS 和 WebSocket 握手");
                    remote.probePublic(node, origin, proxied);
                }
            }
        }
        if (!rename && (previousProxy != proxied || !origin.equals(target.origin))) {
            op.context.put("dnsTouched", true);
            store.progress(op, "正在确认 Cloudflare 代理状态");
            store.confirmed(target, dns.apply(target, origin, proxied), origin, proxied);
            if (siteChanged && previousProxy != proxied) remote.probePublic(node, origin, proxied);
        }
        final Instant expiry = renew;
        store.transactions().execute(
                status -> {
                    if (store.nodeService().update(node) != 1) throw new IllegalStateException("节点更新失败");
                    store.nodeService().updateDomainPolicy(
                            node.getId(), node.getDomainPolicyJson(), op.actor);
                    if (expiry != null) active(target, expiry);
                    if (rename)
                        store.jdbc().update(
                                "UPDATE res_ws_domain SET state='RETIRING' WHERE id=?",
                                oldDomain.id);
                    op.context.put("committed", true);
                    store.progress(op, "正在清理旧域名");
                    return null;
                });
        finishEdit(op, node);
    }

    private void finishEdit(Operation op, ProxyNode node) throws Exception {
        for (Domain d : store.domains(node.getWsKey()))
            if ("RETIRING".equals(d.state)) release(d, true);
        store.done(op, node.getId());
    }

    private void compensate(Operation op) throws Exception {
        Domain d = store.allocated(op.id);
        boolean touched = op.context.getBooleanValue("remoteTouched");
        // Remove the public record even if an offline origin delays local rollback.
        if (d == null && !touched && !op.context.getBooleanValue("dnsTouched")) return;
        if (d != null) dns.remove(d);
        if ("EDIT".equals(op.kind)) {
            ProxyNode old = instanceNode(op);
            Domain original = current(old);
            if (touched)
                try (SSHClient ssh = remote.connect(op.instance)) {
                    remote.inbound(ssh, old, policies.parseStored(old.getDomainPolicyJson()));
                    remote.site(ssh, original, old);
                }
            if (op.context.getBooleanValue("dnsTouched")) {
                boolean proxy =
                        JSON.parseObject(old.getConfigJson()).getBooleanValue("cloudflareProxied");
                store.confirmed(
                        original,
                        dns.apply(original, original.origin, proxy),
                        original.origin,
                        proxy);
            }
        } else if ("CREATE".equals(op.kind) && touched) {
            try (SSHClient ssh = remote.connect(op.instance)) {
                remote.run(ssh, "remove-inbound", "VLESS-WS-TLS-" + op.id);
                if (op.context.containsKey("listen"))
                    remote.run(ssh, "uncount", op.context.getString("listen"));
            }
        }
        if (d != null) finishRelease(d, touched);
    }

    private void release(Domain d, boolean useSsh) throws Exception {
        dns.remove(d);
        finishRelease(d, useSsh);
    }

    private void finishRelease(Domain d, boolean useSsh) throws Exception {
        if (useSsh)
            try (SSHClient ssh = remote.connect(d.instance)) {
                remote.run(ssh, "remove-site", d.id);
            }
        store.jdbc().update("DELETE FROM res_ws_domain WHERE id=?", d.id);
    }

    public ProxyNode stopIfExpired(ProxyNode expected, Date now) {
        java.util.concurrent.atomic.AtomicReference<ProxyNode> stopped =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (VpsInstanceOperationCoordinator.LockHandle ignored =
                locks.lock(expected.getInstanceId())) {
            store.withInstanceLock(
                    expected.getInstanceId(),
                    () -> {
                        ProxyNode node = store.nodeService().getById(expected.getId());
                        if (node == null
                                || !"0".equals(node.getStatus())
                                || node.getExpireTime() == null
                                || node.getExpireTime().after(now)) return;
                        node.setStatus("1");
                        node.setUpdateBy("system");
                        try (SSHClient ssh = remote.connect(node.getInstanceId())) {
                            remote.inbound(
                                    ssh, node, policies.parseStored(node.getDomainPolicyJson()));
                        } catch (IOException e) {
                            throw new IllegalStateException("停用源站入站失败", e);
                        }
                        if (store.nodeService().update(node) == 1) stopped.set(node);
                    });
        }
        return stopped.get();
    }

    public void delete(ProxyNode node, boolean force, String actor) {
        blockingDelete(
                force ? "FORCE" : "DELETE",
                node.getInstanceId(),
                node.getId(),
                node.getWsKey(),
                actor);
    }

    public void forceVps(long id, String actor) {
        blockingDelete("FORCE_VPS", id, null, String.valueOf(id), actor);
    }

    private void blockingDelete(String kind, long instance, Long node, String key, String actor) {
        String id = stableId(kind + ":" + key);
        Operation op = store.operation(id);
        if (op == null) store.submit(id, kind, instance, node, new JSONObject(), actor);
        run(id);
        op = store.operation(id);
        if (!"DONE".equals(op.state)) throw new IllegalStateException("删除待完成，后台会重试。" + op.stage);
    }

    public boolean hasBindings(long instance) {
        return store.jdbc().queryForObject(
                        "SELECT (EXISTS(SELECT 1 FROM res_ws_domain WHERE instance_id=?) OR "
                                + "EXISTS(SELECT 1 FROM res_ws_operation WHERE instance_id=? AND kind='CREATE' AND state NOT IN ('DONE','FAILED')))",
                        Integer.class,
                        instance,
                        instance)
                != 0;
    }

    public void syncAddress(long instance) {
        List<Long> nodeIds =
                store.jdbc().queryForList(
                        "SELECT n.id FROM res_proxy_node n JOIN res_ws_domain d "
                                + "ON n.ws_key=d.node_key WHERE d.instance_id=? AND d.state='ACTIVE'",
                        Long.class,
                        instance);
        for (Long nodeId : nodeIds) {
            String id = WsStore.uuid();
            store.submit(id, "MAINTAIN", instance, nodeId, new JSONObject(), "system");
            dispatch(id);
        }
    }

    private void maintainNode(Operation op) throws Exception {
        ProxyNode node = store.nodeService().getById(op.node);
        if (node != null) {
            Domain d = current(node);
            String origin = ip(op.instance);
            store.confirmed(d, dns.apply(d, origin, d.proxied), origin, d.proxied);
            try (SSHClient ssh = remote.connect(op.instance)) {
                Instant renew = certificates.ensure(ssh, d);
                remote.site(ssh, d, node);
                remote.probe(ssh, d, node);
                active(d, renew);
            }
        }
        store.done(op, op.node);
    }

    private void remove(Operation op) throws Exception {
        store.progress(op, "正在清理节点和托管 DNS");
        ProxyNode node = store.nodeService().getById(op.node);
        if (node != null) removeNode(node, "FORCE".equals(op.kind), op.actor);
        store.done(op, op.node);
    }

    private void removeNode(ProxyNode node, boolean force, String actor) throws Exception {
        ProxyNodeRateLimit limit = limits.getActiveByNodeId(node.getId());
        if (managed(node)) {
            store.jdbc().update(
                    "UPDATE res_ws_domain SET state='DELETING' WHERE node_key=?", node.getWsKey());
            if (!force)
                try (SSHClient ssh = remote.connect(node.getInstanceId())) {
                    remote.run(ssh, "unlimit", node.getListenPort().toString());
                    remote.run(ssh, "remove-inbound", node.getNodeName());
                    remote.run(ssh, "uncount", node.getListenPort().toString());
                }
            for (Domain d : store.domains(node.getWsKey())) release(d, !force);
        }
        store.transactions().execute(
                status -> {
                    store.jdbc().update(
                            "UPDATE res_ws_operation SET state='FAILED',stage='节点已删除',reserved_port=NULL "
                                    + "WHERE node_id=? AND kind IN ('EDIT','MAINTAIN') AND state NOT IN ('DONE','FAILED')",
                            node.getId());
                    if (limit != null) limits.markRemoved(limit.getId(), "节点已删除", actor);
                    traffic.deleteByNodeId(node.getId());
                    store.nodeService().deleteById(node.getId());
                    return null;
                });
    }

    private void removeVps(Operation op) throws Exception {
        store.progress(op, "正在清理 VPS 的节点和托管 DNS");
        ProxyNode filter = new ProxyNode();
        filter.setInstanceId(op.instance);
        for (ProxyNode node : store.nodeService().selectList(filter)) removeNode(node, true, op.actor);
        for (String key :
                store.jdbc().queryForList(
                        "SELECT DISTINCT node_key FROM res_ws_domain WHERE instance_id=?",
                        String.class,
                        op.instance)) for (Domain d : store.domains(key)) release(d, false);
        store.transactions().execute(
                status -> {
                    store.jdbc().update(
                            "UPDATE res_ws_operation SET state='FAILED',stage='VPS 已删除',reserved_port=NULL WHERE instance_id=? AND id<>? AND state NOT IN ('DONE','FAILED')",
                            op.instance,
                            op.id);
                    instances.deleteById(op.instance);
                    store.done(op, null);
                    return null;
                });
    }

    public ProxyNode subscription(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) return null;
        ProxyNode node = store.findNode("subscription_token", token);
        if (!managed(node)
                || !"0".equals(node.getStatus())
                || node.getExpireTime() != null && !node.getExpireTime().after(new Date()))
            return null;
        int pending =
                store.jdbc().queryForObject(
                        "SELECT count(*) FROM res_ws_operation WHERE state NOT IN ('DONE','FAILED') "
                                + "AND ((node_id=? AND kind IN ('DELETE','FORCE')) OR (instance_id=? AND kind='FORCE_VPS'))",
                        Integer.class,
                        node.getId(),
                        node.getInstanceId());
        return pending == 0 ? node : null;
    }
}
