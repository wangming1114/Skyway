package com.skyway.resource.mapper;

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
     * 按实例与端口查询唯一节点（用于流量采集时映射 port -> node_id）
     */
    ProxyNode selectByInstanceIdAndPort(@Param("instanceId") Long instanceId, @Param("port") Integer port);

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
