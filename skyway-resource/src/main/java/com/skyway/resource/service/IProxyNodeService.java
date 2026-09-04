package com.skyway.resource.service;

import java.util.Date;
import java.util.List;
import com.skyway.resource.domain.ProxyNode;

/**
 * 代理节点 服务层
 *
 * @author ruoyi
 */
public interface IProxyNodeService {

    /**
     * 查询列表
     *
     * @param proxyNode 查询条件（含 instanceId、nodeType、status）
     * @return 列表
     */
    List<ProxyNode> selectList(ProxyNode proxyNode);

    /** 统计条数（条件同 selectList） */
    int count(ProxyNode proxyNode);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 实体
     */
    ProxyNode getById(Long id);

    /**
     * 查询已到期且状态为正常的节点（用于定时任务自动停用）
     *
     * @param maxExpireTime 到期时间上限
     * @return 节点列表
     */
    List<ProxyNode> listExpiredAndNormal(Date maxExpireTime);

    /**
     * 查询在指定时间区间内到期且状态为正常的节点（用于即将到期提醒）
     *
     * @param fromTime 到期时间下限
     * @param toTime   到期时间上限
     * @return 节点列表
     */
    List<ProxyNode> listExpiringWithin(Date fromTime, Date toTime);

    /**
     * 按实例与端口查询唯一节点
     */
    ProxyNode getByInstanceIdAndPort(Long instanceId, Integer port);

    /**
     * 推荐可用端口：从 10000 起连续查找第一个未被该实例占用的端口
     *
     * @param instanceId 实例ID
     * @return 推荐端口（10000~65535），若无可用则返回 10000
     */
    Integer recommendPort(Long instanceId);

    /**
     * 查询实例下数据库已登记的全部节点端口。
     */
    List<Integer> listUsedPorts(Long instanceId);

    /**
     * 新增（自动生成分享链接）
     *
     * @param row 实体
     * @return 影响行数
     */
    int insert(ProxyNode row);

    /**
     * 修改（自动重新生成分享链接）
     *
     * @param row 实体
     * @return 影响行数
     */
    int update(ProxyNode row);

    int updateDomainPolicy(Long id, String domainPolicyJson, String updateBy);

    /**
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 批量删除
     *
     * @param ids 主键数组
     * @return 影响行数
     */
    int deleteByIds(Long[] ids);
}
