package com.skyway.resource.mapper;

import java.util.List;
import com.skyway.resource.domain.VpsInstance;

/**
 * VPS 实例 数据层
 *
 * @author ruoyi
 */
public interface VpsInstanceMapper {

    /**
     * 分页查询列表（支持 keyword、categoryId、nodeId、status）
     *
     * @param instance 查询条件
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
    VpsInstance selectById(Long id);

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
