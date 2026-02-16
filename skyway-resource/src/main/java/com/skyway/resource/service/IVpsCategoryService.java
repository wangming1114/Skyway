package com.skyway.resource.service;

import java.util.List;
import com.skyway.resource.domain.VpsCategory;

/**
 * 资源分类与节点 服务层
 *
 * @author ruoyi
 */
public interface IVpsCategoryService {

    /**
     * 查询分类树（仅 type='1'）
     *
     * @return 树形列表
     */
    List<VpsCategory> listCategoryTree();

    /**
     * 查询节点扁平列表（仅 type='2'）
     *
     * @return 列表
     */
    List<VpsCategory> listNode();

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 实体
     */
    VpsCategory getById(Long id);

    /**
     * 新增
     *
     * @param row 实体
     * @return 影响行数
     */
    int insert(VpsCategory row);

    /**
     * 修改
     *
     * @param row 实体
     * @return 影响行数
     */
    int update(VpsCategory row);

    /**
     * 删除（校验：分类存在子分类或实例则禁止；节点存在实例则禁止）
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
