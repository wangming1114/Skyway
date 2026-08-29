package com.skyway.resource.service;

import java.util.List;
import com.skyway.resource.domain.VpsInstance;

/**
 * VPS 实例 服务层
 *
 * @author ruoyi
 */
public interface IVpsInstanceService {

    /**
     * 展开分类筛选条件。分页查询必须在 startPage() 前调用，
     * 避免分类查询消耗 PageHelper 的分页上下文。
     */
    void prepareListFilter(VpsInstance instance);

    /**
     * 分页查询列表（由 Controller 调用 startPage 后执行，返回当前页列表）
     *
     * @param instance 查询条件（含 keyword、categoryId、nodeId、status）
     * @return 列表
     */
    List<VpsInstance> selectList(VpsInstance instance);

    /** 统计条数（条件同 selectList） */
    int count(VpsInstance instance);

    /**
     * 根据ID查询（含 categoryName、nodeName）
     *
     * @param id 主键
     * @return 实体
     */
    VpsInstance getById(Long id);

    /**
     * 新增
     *
     * @param row 实体
     * @return 影响行数
     */
    int insert(VpsInstance row);

    /**
     * 修改
     *
     * @param row 实体
     * @return 影响行数
     */
    int update(VpsInstance row);

    /**
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
