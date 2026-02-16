package com.skyway.resource.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.common.exception.ServiceException;
import com.skyway.common.utils.StringUtils;
import com.skyway.resource.domain.VpsCategory;
import com.skyway.resource.mapper.VpsCategoryMapper;
import com.skyway.resource.service.IVpsCategoryService;

/**
 * 资源分类与节点 服务实现
 *
 * @author ruoyi
 */
@Service
public class VpsCategoryServiceImpl implements IVpsCategoryService {

    private static final String TYPE_CATEGORY = "1";
    private static final String TYPE_NODE = "2";

    @Autowired
    private VpsCategoryMapper vpsCategoryMapper;

    @Override
    public List<VpsCategory> listCategoryTree() {
        List<VpsCategory> list = vpsCategoryMapper.selectListByType(TYPE_CATEGORY);
        return buildCategoryTree(list);
    }

    @Override
    public List<VpsCategory> listNode() {
        return vpsCategoryMapper.selectListByType(TYPE_NODE);
    }

    @Override
    public VpsCategory getById(Long id) {
        return vpsCategoryMapper.selectById(id);
    }

    @Override
    public int insert(VpsCategory row) {
        if (StringUtils.isEmpty(row.getType())) {
            row.setType(TYPE_CATEGORY);
        }
        if (row.getParentId() == null) {
            row.setParentId(0L);
        }
        if (row.getOrderNum() == null) {
            row.setOrderNum(0);
        }
        return vpsCategoryMapper.insert(row);
    }

    @Override
    public int update(VpsCategory row) {
        return vpsCategoryMapper.update(row);
    }

    @Override
    public int deleteById(Long id) {
        VpsCategory row = vpsCategoryMapper.selectById(id);
        if (row == null) {
            return 0;
        }
        if (TYPE_CATEGORY.equals(row.getType())) {
            if (vpsCategoryMapper.hasChildById(id) > 0) {
                throw new ServiceException("存在子分类，不允许删除");
            }
            if (vpsCategoryMapper.countInstanceByCategoryId(id) > 0) {
                throw new ServiceException("该分类下存在VPS实例，不允许删除");
            }
        } else if (TYPE_NODE.equals(row.getType())) {
            if (vpsCategoryMapper.countInstanceByNodeId(id) > 0) {
                throw new ServiceException("该节点下存在VPS实例，不允许删除");
            }
        }
        return vpsCategoryMapper.deleteById(id);
    }

    /**
     * 构建分类树
     */
    private List<VpsCategory> buildCategoryTree(List<VpsCategory> list) {
        List<VpsCategory> returnList = new ArrayList<>();
        List<Long> tempList = list.stream().map(VpsCategory::getId).collect(Collectors.toList());
        for (VpsCategory item : list) {
            Long parentId = item.getParentId() != null ? item.getParentId() : 0L;
            if (!tempList.contains(parentId)) {
                recursionFn(list, item);
                returnList.add(item);
            }
        }
        if (returnList.isEmpty()) {
            returnList = list;
        }
        return returnList;
    }

    private void recursionFn(List<VpsCategory> list, VpsCategory t) {
        List<VpsCategory> childList = getChildList(list, t);
        t.setChildren(childList);
        for (VpsCategory child : childList) {
            if (getChildList(list, child).size() > 0) {
                recursionFn(list, child);
            }
        }
    }

    private List<VpsCategory> getChildList(List<VpsCategory> list, VpsCategory t) {
        List<VpsCategory> tlist = new ArrayList<>();
        for (VpsCategory n : list) {
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tlist.add(n);
            }
        }
        return tlist;
    }
}
