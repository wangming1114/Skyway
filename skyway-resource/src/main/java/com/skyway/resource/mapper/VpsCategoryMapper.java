package com.skyway.resource.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.skyway.resource.domain.VpsCategory;

/**
 * 资源分类与节点 数据层
 *
 * @author ruoyi
 */
public interface VpsCategoryMapper {

    /**
     * 按类型查询列表（type='1' 分类用于树，type='2' 节点用于下拉）
     *
     * @param type 类型
     * @return 列表
     */
    List<VpsCategory> selectListByType(@Param("type") String type);

    /**
     * 根据ID查询
     *
     * @param id 主键
     * @return 实体
     */
    VpsCategory selectById(Long id);

    /**
     * 是否存在子节点（仅分类 type='1' 时有层级）
     *
     * @param id 主键
     * @return 数量
     */
    int hasChildById(Long id);

    /**
     * 统计使用该分类的实例数量（category_id）
     *
     * @param categoryId 分类ID
     * @return 数量
     */
    int countInstanceByCategoryId(Long categoryId);

    /**
     * 统计使用该节点的实例数量（node_id）
     *
     * @param nodeId 节点ID
     * @return 数量
     */
    int countInstanceByNodeId(Long nodeId);

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
     * 删除
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
