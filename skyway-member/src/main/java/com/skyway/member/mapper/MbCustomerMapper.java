package com.skyway.member.mapper;

import java.util.List;
import com.skyway.member.domain.MbCustomer;

/**
 * 会员客户 mb_customer Mapper
 *
 * @author ruoyi
 */
public interface MbCustomerMapper {

    List<MbCustomer> selectList(MbCustomer query);

    /** 统计条数（条件同 selectList） */
    int count(MbCustomer query);

    MbCustomer selectById(Long id);

    /**
     * 按邮箱查询客户（用于 C 端登录）
     */
    MbCustomer selectByEmail(String email);

    /**
     * 按用户名查询客户（用于 C 端登录）
     */
    MbCustomer selectByUsername(String username);

    int insert(MbCustomer row);

    int update(MbCustomer row);

    int updateStatus(Long id, String status, String updateBy);

    int updatePassword(Long id, String password, String updateBy);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);

    int checkUsernameUnique(String username, Long excludeId);

    /**
     * 检查邮箱是否已存在（用于 C 端注册）
     */
    int countByEmail(String email);
}
