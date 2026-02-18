package com.skyway.resource.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.mapper.ProxyNodeMapper;
import com.skyway.resource.service.IProxyNodeService;

/**
 * 代理节点 服务实现
 *
 * @author ruoyi
 */
@Service
public class ProxyNodeServiceImpl implements IProxyNodeService {

    @Autowired
    private ProxyNodeMapper proxyNodeMapper;

    @Override
    public List<ProxyNode> selectList(ProxyNode proxyNode) {
        return proxyNodeMapper.selectList(proxyNode);
    }

    @Override
    public int count(ProxyNode proxyNode) {
        return proxyNodeMapper.count(proxyNode);
    }

    @Override
    public ProxyNode getById(Long id) {
        return proxyNodeMapper.selectById(id);
    }

    @Override
    public List<ProxyNode> listExpiredAndNormal(Date maxExpireTime) {
        return proxyNodeMapper.selectExpiredAndNormal(maxExpireTime);
    }

    @Override
    public List<ProxyNode> listExpiringWithin(Date fromTime, Date toTime) {
        return proxyNodeMapper.selectExpiringWithin(fromTime, toTime);
    }

    @Override
    public ProxyNode getByInstanceIdAndPort(Long instanceId, Integer port) {
        return proxyNodeMapper.selectByInstanceIdAndPort(instanceId, port);
    }

    @Override
    public Integer recommendPort(Long instanceId) {
        if (instanceId == null) {
            return 10000;
        }
        List<Integer> used = proxyNodeMapper.selectPortsByInstanceId(instanceId);
        Set<Integer> usedSet = new HashSet<>(used != null ? used : Collections.emptyList());
        for (int p = 10000; p <= 65535; p++) {
            if (!usedSet.contains(p)) {
                return p;
            }
        }
        return 10000;
    }

    @Override
    public int insert(ProxyNode row) {
        row.setUrl(buildShareUrl(row));
        return proxyNodeMapper.insert(row);
    }

    @Override
    public int update(ProxyNode row) {
        row.setUrl(buildShareUrl(row));
        return proxyNodeMapper.update(row);
    }

    @Override
    public int deleteById(Long id) {
        return proxyNodeMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        return proxyNodeMapper.deleteByIds(ids);
    }

    /**
     * 根据节点类型和配置参数自动生成分享链接
     */
    private String buildShareUrl(ProxyNode node) {
        if (node.getConfigJson() == null || node.getConfigJson().isEmpty()) {
            return null;
        }
        try {
            JSONObject cfg = JSON.parseObject(node.getConfigJson());
            String nodeType = node.getNodeType();
            if (nodeType == null) {
                return null;
            }

            if ("VLESS-REALITY".equals(nodeType)) {
                return buildVlessRealityUrl(node, cfg);
            }
            // 其他协议类型后续扩展
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建 VLESS-REALITY 分享链接
     * 格式: vless://{uuid}@{address}:{port}?encryption=none&security=reality&flow={flow}&type={network}&sni={sni}&pbk={publicKey}&fp={fingerprint}#{nodeName}
     */
    private String buildVlessRealityUrl(ProxyNode node, JSONObject cfg) {
        String uuid = cfg.getString("id");
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("vless://").append(uuid);
        sb.append("@").append(node.getAddress());
        sb.append(":").append(node.getPort());
        sb.append("?encryption=none");
        sb.append("&security=reality");
        appendParam(sb, "flow", cfg.getString("flow"));
        appendParam(sb, "type", cfg.getString("network"));
        appendParam(sb, "sni", cfg.getString("sni"));
        appendParam(sb, "pbk", cfg.getString("publicKey"));
        appendParam(sb, "fp", cfg.getString("fingerprint"));
        appendParam(sb, "sid", cfg.getString("shortId"));
        // fragment = nodeName
        if (node.getNodeName() != null && !node.getNodeName().isEmpty()) {
            try {
                sb.append("#").append(URLEncoder.encode(node.getNodeName(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                sb.append("#").append(node.getNodeName());
            }
        }
        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("&").append(key).append("=").append(value);
        }
    }
}
