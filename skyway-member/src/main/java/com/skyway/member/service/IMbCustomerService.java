package com.skyway.member.service;

import java.util.List;
import com.skyway.member.domain.MbCustomer;

/**
 * 会员客户 服务层
 *
 * @author ruoyi
 */
public interface IMbCustomerService {

    List<MbCustomer> selectList(MbCustomer query);

    /** 统计条数（条件同 selectList） */
    int count(MbCustomer query);

    MbCustomer getById(Long id);

    int insert(MbCustomer row);

    int update(MbCustomer row);

    int resetPwd(Long id, String newPassword, String updateBy);

    int updateStatus(Long id, String status, String updateBy);

    int deleteById(Long id);

    int deleteByIds(Long[] ids);

    boolean checkUsernameUnique(MbCustomer customer);
}
