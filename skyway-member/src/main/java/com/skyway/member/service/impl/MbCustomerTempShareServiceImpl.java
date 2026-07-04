package com.skyway.member.service.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.common.utils.SecurityUtils;
import com.skyway.common.utils.StringUtils;
import com.skyway.common.utils.uuid.IdUtils;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.domain.MbCustomerTempShare;
import com.skyway.member.mapper.MbCustomerMapper;
import com.skyway.member.mapper.MbCustomerTempShareMapper;
import com.skyway.member.service.IMbCustomerTempShareService;
import com.skyway.resource.domain.ProxyNode;
import com.skyway.resource.service.IProxyNodeService;

/**
 * 客户订阅临时访问链接服务实现
 */
@Service
public class MbCustomerTempShareServiceImpl implements IMbCustomerTempShareService {

    @Autowired
    private MbCustomerTempShareMapper tempShareMapper;

    @Autowired
    private MbCustomerMapper customerMapper;

    @Autowired
    private IProxyNodeService proxyNodeService;

    @Override
    public List<MbCustomerTempShare> listByCustomerId(Long customerId) {
        return tempShareMapper.selectByCustomerId(customerId);
    }

    @Override
    public MbCustomerTempShare create(Long customerId, String accessPassword, Date expireTime, String createBy) {
        if (customerId == null || StringUtils.isBlank(accessPassword) || expireTime == null) {
            throw new IllegalArgumentException("参数错误");
        }
        MbCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        MbCustomerTempShare row = new MbCustomerTempShare();
        row.setCustomerId(customerId);
        row.setToken(IdUtils.fastSimpleUUID());
        row.setAccessPassword(SecurityUtils.encryptPassword(accessPassword));
        row.setExpireTime(expireTime);
        row.setStatus("0");
        row.setCreateBy(createBy);
        tempShareMapper.insert(row);
        return row;
    }

    @Override
    public int revoke(Long id, String updateBy) {
        if (id == null) {
            throw new IllegalArgumentException("参数错误");
        }
        return tempShareMapper.revoke(id, updateBy);
    }

    @Override
    public List<ProxyNode> unlock(String token, String accessPassword) {
        if (StringUtils.isBlank(token) || StringUtils.isBlank(accessPassword)) {
            throw new IllegalArgumentException("参数错误");
        }
        MbCustomerTempShare share = tempShareMapper.selectByToken(token);
        validateShare(share, accessPassword);
        ProxyNode query = new ProxyNode();
        query.setCustomerId(share.getCustomerId());
        query.setStatus("0");
        query.setExpireStatus("unexpired");
        return proxyNodeService.selectList(query).stream()
                .map(this::toPublicNode)
                .collect(Collectors.toList());
    }

    private ProxyNode toPublicNode(ProxyNode source) {
        ProxyNode node = new ProxyNode();
        node.setId(source.getId());
        node.setInstanceId(source.getInstanceId());
        node.setNodeName(source.getNodeName());
        node.setNodeType(source.getNodeType());
        node.setAddress(source.getAddress());
        node.setPort(source.getPort());
        node.setUrl(source.getUrl());
        node.setExpireTime(source.getExpireTime());
        node.setStatus(source.getStatus());
        node.setCreateTime(source.getCreateTime());
        return node;
    }

    private void validateShare(MbCustomerTempShare share, String accessPassword) {
        if (share == null) {
            throw new IllegalArgumentException("临时访问不存在");
        }
        if (!"0".equals(share.getStatus())) {
            throw new IllegalArgumentException("临时访问已作废");
        }
        if (share.getExpireTime() == null || share.getExpireTime().before(new Date())) {
            throw new IllegalArgumentException("临时访问已过期");
        }
        if (!SecurityUtils.matchesPassword(accessPassword, share.getAccessPassword())) {
            throw new IllegalArgumentException("访问密码错误");
        }
    }
}
