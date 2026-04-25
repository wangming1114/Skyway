package com.skyway.resource.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.skyway.resource.domain.ProxyNodeTraffic;

/**
 * 代理节点流量明细 数据层
 *
 * @author ruoyi
 */
public interface ProxyNodeTrafficMapper {

    int insert(ProxyNodeTraffic row);

    /**
     * 按节点汇总总流量（SUM 可能返回 BigDecimal，调用方需安全转 long）
     */
    Map<String, Object> selectSumByNodeId(Long nodeId);

    /**
     * 按实例汇总总流量（该实例下所有节点的 rx+tx 合计）
     */
    Map<String, Object> selectSumByInstanceId(Long instanceId);

    List<Map<String, Object>> selectSumByInstanceIds(@Param("instanceIds") List<Long> instanceIds);

    /**
     * 按节点查近期明细（最近 N 条，用于曲线/速率）
     */
    List<ProxyNodeTraffic> selectRecentByNodeId(@Param("nodeId") Long nodeId, @Param("limit") int limit);

    /**
     * 删除该节点所有流量明细（节点删除时重置计数用）
     */
    int deleteByNodeId(Long nodeId);
}
