package com.skyway.resource.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.skyway.resource.domain.ProxyNode;

/**
 * 代理节点 数据层
 *
 * @author ruoyi
 */
public interface ProxyNodeMapper {

    /**
     * 查询列表（支持 instanceId、nodeType、status）
     *
     * @param proxyNode 查询条件
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
    ProxyNode selectById(Long id);

    /**
     * 查询已到期且状态为正常的节点（用于定时任务自动停用）
     *
     * @param maxExpireTime 到期时间上限，查询 expire_time &lt; maxExpireTime
     * @return 节点列表
     */
    List<ProxyNode> selectExpiredAndNormal(@Param("maxExpireTime") Date maxExpireTime);

    /**
     * 查询在指定时间区间内到期且状态为正常的节点（用于即将到期提醒）
     *
     * @param fromTime 到期时间下限，expire_time &gt;= fromTime
     * @param toTime   到期时间上限，expire_time &lt;= toTime
     * @return 节点列表
     */
    List<ProxyNode> selectExpiringWithin(@Param("fromTime") Date fromTime, @Param("toTime") Date toTime);

    /**
     * 按实例与端口查询唯一节点（用于流量采集时映射 port -> node_id）
     */
    ProxyNode selectByInstanceIdAndPort(@Param("instanceId") Long instanceId, @Param("port") Integer port);

    /**
     * 查询某实例下已占用的端口列表（用于推荐可用端口）
     *
     * @param instanceId 实例ID
     * @return 已占用的端口列表，按端口升序
     */
    List<Integer> selectPortsByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 统计某实例下的代理节点数量（删除实例前校验用）
     */
    int countByInstanceId(Long instanceId);

    /**
     * 统计某客户下的代理节点数量（删除客户前校验用）
     */
    int countByCustomerId(Long customerId);

    /**
     * 新增
     *
     * @param row 实体
     * @return 影响行数
     */
    int insert(ProxyNode row);

    /**
     * 修改
     *
     * @param row 实体
     * @return 影响行数
     */
    int update(ProxyNode row);

    int updateDomainPolicy(@Param("id") Long id, @Param("domainPolicyJson") String domainPolicyJson,
                           @Param("updateBy") String updateBy);

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
