package com.skyway.member.service;

import java.util.Date;
import java.util.List;
import com.skyway.member.domain.MbCustomerTempShare;
import com.skyway.resource.domain.ProxyNode;

/**
 * 客户订阅信息访问链接服务
 */
public interface IMbCustomerTempShareService {

    List<MbCustomerTempShare> listByCustomerId(Long customerId);

    MbCustomerTempShare create(Long customerId, String accessPassword, Date expireTime, String createBy);

    int revoke(Long id, String updateBy);

    List<ProxyNode> unlock(String token, String accessPassword);
}
