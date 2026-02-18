package com.skyway.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.common.exception.ServiceException;
import com.skyway.common.utils.StringUtils;
import com.skyway.member.domain.MbCustomer;
import com.skyway.member.mapper.MbCustomerMapper;
import com.skyway.member.service.IMbCustomerService;
import com.skyway.resource.mapper.ProxyNodeMapper;

/**
 * 会员客户 服务实现
 *
 * @author ruoyi
 */
@Service
public class MbCustomerServiceImpl implements IMbCustomerService {

    @Autowired
    private MbCustomerMapper mbCustomerMapper;

    @Autowired
    private ProxyNodeMapper proxyNodeMapper;

    @Override
    public List<MbCustomer> selectList(MbCustomer query) {
        return mbCustomerMapper.selectList(query);
    }

    @Override
    public int count(MbCustomer query) {
        return mbCustomerMapper.count(query);
    }

    @Override
    public MbCustomer getById(Long id) {
        return mbCustomerMapper.selectById(id);
    }

    @Override
    public int insert(MbCustomer row) {
        return mbCustomerMapper.insert(row);
    }

    @Override
    public int update(MbCustomer row) {
        return mbCustomerMapper.update(row);
    }

    @Override
    public int resetPwd(Long id, String newPassword, String updateBy) {
        return mbCustomerMapper.updatePassword(id, newPassword, updateBy);
    }

    @Override
    public int updateStatus(Long id, String status, String updateBy) {
        return mbCustomerMapper.updateStatus(id, status, updateBy);
    }

    @Override
    public int deleteById(Long id) {
        if (id != null && proxyNodeMapper.countByCustomerId(id) > 0) {
            throw new ServiceException("该客户下存在代理节点，无法删除，请先移除相关代理节点");
        }
        return mbCustomerMapper.deleteById(id);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                if (proxyNodeMapper.countByCustomerId(id) > 0) {
                    throw new ServiceException("该客户下存在代理节点，无法删除，请先移除相关代理节点");
                }
            }
        }
        return mbCustomerMapper.deleteByIds(ids);
    }

    @Override
    public boolean checkUsernameUnique(MbCustomer customer) {
        if (customer == null || StringUtils.isEmpty(customer.getUsername())) {
            return false;
        }
        int c = mbCustomerMapper.checkUsernameUnique(customer.getUsername(), customer.getId());
        return c == 0;
    }
}
