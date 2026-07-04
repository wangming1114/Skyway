package com.skyway.member.mapper;

import java.util.List;
import com.skyway.member.domain.MbCustomerTempShare;

/**
 * 客户订阅信息访问链接 Mapper
 */
public interface MbCustomerTempShareMapper {

    List<MbCustomerTempShare> selectByCustomerId(Long customerId);

    MbCustomerTempShare selectByToken(String token);

    int insert(MbCustomerTempShare row);

    int revoke(Long id, String updateBy);
}
