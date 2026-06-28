package com.skyway.resource.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.skyway.resource.domain.ProxyNodeRateLimit;

/**
 * 代理节点端口限速 数据层
 */
public interface ProxyNodeRateLimitMapper {

    ProxyNodeRateLimit selectById(Long id);

    ProxyNodeRateLimit selectActiveByNodeId(@Param("proxyNodeId") Long proxyNodeId);

    List<ProxyNodeRateLimit> selectActiveByNodeIds(@Param("nodeIds") List<Long> nodeIds);

    List<ProxyNodeRateLimit> selectActiveByInstanceId(@Param("instanceId") Long instanceId);

    List<ProxyNodeRateLimit> selectExpiredActive(@Param("now") Date now);

    int insert(ProxyNodeRateLimit row);

    int update(ProxyNodeRateLimit row);

    int markStatus(@Param("id") Long id,
                   @Param("status") String status,
                   @Param("lastApplyResult") String lastApplyResult,
                   @Param("updateBy") String updateBy);
}
