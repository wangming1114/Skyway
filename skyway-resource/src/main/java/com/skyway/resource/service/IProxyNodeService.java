package com.skyway.resource.service;

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

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 实体
     */
    ProxyNode getById(Long id);

    /**
     * 按实例与端口查询唯一节点
     */
    ProxyNode getByInstanceIdAndPort(Long instanceId, Integer port);

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
