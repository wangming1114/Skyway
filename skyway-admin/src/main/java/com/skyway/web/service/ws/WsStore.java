package com.skyway.web.service.ws;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;
import java.sql.ResultSet;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Durable intent precedes every external allocation. DNS bindings survive node deletion attempts.
 */
@Repository
public class WsStore {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final IProxyNodeService nodes;

    public WsStore(JdbcTemplate db, TransactionTemplate tx, IProxyNodeService nodes) {
        this.db = db;
        this.tx = tx;
        this.nodes = nodes;
    }

    JdbcTemplate jdbc() {
        return db;
    }

    TransactionTemplate transactions() {
        return tx;
    }

    IProxyNodeService nodeService() {
        return nodes;
    }

    static class Operation {
        String id, kind, actor, state, stage;
        long instance;
        Long node;
        JSONObject request, context;
        int attempts;
    }

    public static class Domain {
        String id, key, host, zone, record, origin, state;
        long instance;
        boolean proxied;
    }

    private static final RowMapper<Operation> OPERATION =
            (rs, row) -> {
                Operation o = new Operation();
                o.id = rs.getString("id");
                o.kind = rs.getString("kind");
                o.actor = rs.getString("actor");
                o.instance = rs.getLong("instance_id");
                o.node = (Long) rs.getObject("node_id");
                o.state = rs.getString("state");
                o.stage = rs.getString("stage");
                o.attempts = rs.getInt("attempts");
                o.request = JSON.parseObject(rs.getString("request_json"));
                o.context = JSON.parseObject(rs.getString("context_json"));
                return o;
            };

    private static final RowMapper<Domain> DOMAIN =
            (rs, row) -> {
                Domain d = new Domain();
                d.id = rs.getString("id");
                d.key = rs.getString("node_key");
                d.instance = rs.getLong("instance_id");
                d.host = rs.getString("hostname");
                d.zone = rs.getString("zone_id");
                d.record = rs.getString("record_id");
                d.origin = rs.getString("origin");
                d.proxied = rs.getBoolean("proxied");
                d.state = rs.getString("state");
                return d;
            };

    static String uuid() {
        return UUID.randomUUID().toString();
    }

    Operation submit(
            String id, String kind, long instance, Long node, JSONObject request, String actor) {
        if (!UUID.fromString(id).toString().equals(id))
            throw new IllegalArgumentException("操作 ID 无效");
        try {
            db.update(
                    "INSERT INTO res_ws_operation (id,kind,instance_id,node_id,actor,request_json,context_json,state,stage,next_run,created_at) "
                            + "VALUES (?,?,?,?,?,?,'{}','QUEUED','等待执行',NOW(),NOW())",
                    id,
                    kind,
                    instance,
                    node,
                    actor,
                    request.toJSONString());
        } catch (DuplicateKeyException duplicate) {
            Operation o = operation(id);
            if (!o.kind.equals(kind)
                    || o.instance != instance
                    || (node != null && !node.equals(o.node))
                    || !o.actor.equals(actor)
                    || !o.request.equals(JSON.parseObject(request.toJSONString())))
                throw new IllegalArgumentException("操作 ID 与原请求不匹配");
        }
        return operation(id);
    }

    Operation operation(String id) {
        List<Operation> rows = db.query("SELECT * FROM res_ws_operation WHERE id=?", OPERATION, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    List<String> due() {
        return db.queryForList(
                "SELECT id FROM res_ws_operation WHERE state IN ('QUEUED','RUNNING','RETRY') "
                        + "AND next_run<=NOW() ORDER BY next_run,created_at LIMIT 32",
                String.class);
    }

    void progress(Operation o, String stage) {
        db.update(
                "UPDATE res_ws_operation SET stage=?,state='RUNNING',context_json=? WHERE id=?",
                stage,
                o.context.toJSONString(),
                o.id);
    }

    void done(Operation o, Long nodeId) {
        db.update(
                "UPDATE res_ws_operation SET state='DONE',stage='完成',node_id=?,reserved_port=NULL,context_json='{}' WHERE id=?",
                nodeId,
                o.id);
    }

    void error(Operation o, String message, boolean retry) {
        int seconds = Math.min(1800, 15 << Math.min(7, o.attempts));
        db.update(
                "UPDATE res_ws_operation SET state=?,stage=?,reserved_port=CASE WHEN ? THEN reserved_port ELSE NULL END,attempts=attempts+1,next_run=TIMESTAMPADD(SECOND,?,NOW()) WHERE id=?",
                retry ? "RETRY" : "FAILED",
                message.substring(0, Math.min(490, message.length())),
                retry,
                seconds,
                o.id);
    }

    List<Domain> domains(String key) {
        return db.query("SELECT * FROM res_ws_domain WHERE node_key=?", DOMAIN, key);
    }

    Domain allocated(String opId) {
        List<Domain> list =
                db.query("SELECT * FROM res_ws_domain WHERE operation_id=?", DOMAIN, opId);
        return list.isEmpty() ? null : list.get(0);
    }

    Domain reserve(
            Operation o, String key, String host, String zone, String origin, boolean proxied) {
        db.update(
                "INSERT INTO res_ws_domain (id,node_key,instance_id,operation_id,hostname,zone_id,origin,proxied,state)"
                        + " VALUES (?,?,?,?,?,?,?,?,'PREPARING')",
                uuid(),
                key,
                o.instance,
                o.id,
                host,
                zone,
                origin,
                proxied);
        return allocated(o.id);
    }

    void confirmed(Domain d, String id, String ip, boolean proxied) {
        db.update(
                "UPDATE res_ws_domain SET record_id=?,origin=?,proxied=? WHERE id=?",
                id,
                ip,
                proxied,
                d.id);
        d.record = id;
        d.origin = ip;
        d.proxied = proxied;
    }

    ProxyNode findNode(String column, String value) {
        if (!"ws_key".equals(column) && !"subscription_token".equals(column))
            throw new IllegalArgumentException();
        List<Long> ids =
                db.queryForList(
                        "SELECT id FROM res_proxy_node WHERE " + column + "=?", Long.class, value);
        return ids.isEmpty() ? null : nodes.getById(ids.get(0));
    }

    interface Work {
        void run();
    }

    void withInstanceLock(long instanceId, Work work) {
        db.execute(
                (ConnectionCallback<Void>)
                        connection -> {
                            String name = "skyway.ws." + instanceId;
                            try (java.sql.PreparedStatement take =
                                    connection.prepareStatement("SELECT GET_LOCK(?,0)")) {
                                take.setString(1, name);
                                try (ResultSet result = take.executeQuery()) {
                                    if (!result.next() || result.getInt(1) != 1) return null;
                                }
                            }
                            try {
                                work.run();
                            } finally {
                                try (java.sql.PreparedStatement release =
                                        connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                                    release.setString(1, name);
                                    release.execute();
                                }
                            }
                            return null;
                        });
    }
}
