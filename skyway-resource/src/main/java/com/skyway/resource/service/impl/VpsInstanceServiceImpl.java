package com.skyway.resource.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.skyway.common.exception.ServiceException;
import com.skyway.resource.domain.VpsCategory;
import com.skyway.resource.domain.VpsInstance;
import com.skyway.resource.mapper.VpsCategoryMapper;
import com.skyway.resource.mapper.VpsInstanceMapper;
import com.skyway.resource.mapper.ProxyNodeMapper;
import com.skyway.resource.mapper.ProxyNodeTrafficMapper;
import com.skyway.resource.service.IVpsInstanceService;

/**
 * VPS 实例 服务实现
 *
 * @author ruoyi
 */
@Service
public class VpsInstanceServiceImpl implements IVpsInstanceService {

    @Autowired
    private VpsInstanceMapper vpsInstanceMapper;

    @Autowired
    private VpsCategoryMapper vpsCategoryMapper;

    @Autowired
    private ProxyNodeMapper proxyNodeMapper;

    @Autowired
    private ProxyNodeTrafficMapper proxyNodeTrafficMapper;

    @Override
    public void prepareListFilter(VpsInstance instance) {
        if (instance != null && instance.getCategoryId() != null) {
            List<Long> categoryIds = collectDescendantIds(instance.getCategoryId());
            instance.setCategoryIds(categoryIds);
            instance.setCategoryId(null);
        }
    }

    @Override
    public List<VpsInstance> selectList(VpsInstance instance) {
        prepareListFilter(instance);
        List<VpsInstance> list = vpsInstanceMapper.selectList(instance);
        if (list != null && !list.isEmpty()) {
            List<Long> instanceIds = list.stream()
                    .map(VpsInstance::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            Map<Long, Long> trafficByInstanceId = new HashMap<>();
            if (!instanceIds.isEmpty()) {
                List<Map<String, Object>> sums = proxyNodeTrafficMapper.selectSumByInstanceIds(instanceIds);
                if (sums != null) {
                    for (Map<String, Object> sum : sums) {
                        Long instanceId = toLongObject(sum.get("instanceId"), sum.get("instanceid"));
                        if (instanceId != null) {
                            long rx = toLong(sum.get("totalRx"), sum.get("totalrx"));
                            long tx = toLong(sum.get("totalTx"), sum.get("totaltx"));
                            trafficByInstanceId.put(instanceId, rx + tx);
                        }
                    }
                }
            }
            for (VpsInstance row : list) {
                if (row.getId() != null) {
                    row.setTotalTrafficBytes(trafficByInstanceId.getOrDefault(row.getId(), 0L));
                }
            }
        }
        return list;
    }

    @Override
    public int count(VpsInstance instance) {
        prepareListFilter(instance);
        return vpsInstanceMapper.count(instance);
    }

    private static long toLong(Object a, Object b) {
        if (a != null) {
            if (a instanceof Number) return ((Number) a).longValue();
            try { return Long.parseLong(String.valueOf(a)); } catch (NumberFormatException ignored) {}
        }
        if (b != null) {
            if (b instanceof Number) return ((Number) b).longValue();
            try { return Long.parseLong(String.valueOf(b)); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    private static Long toLongObject(Object a, Object b) {
        if (a != null) {
            if (a instanceof Number) return ((Number) a).longValue();
            try { return Long.parseLong(String.valueOf(a)); } catch (NumberFormatException ignored) {}
        }
        if (b != null) {
            if (b instanceof Number) return ((Number) b).longValue();
            try { return Long.parseLong(String.valueOf(b)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** 收集某分类及其所有子孙分类ID（兼容 MySQL 5.7，在内存中根据树结构计算） */
    private List<Long> collectDescendantIds(Long rootId) {
        List<VpsCategory> all = vpsCategoryMapper.selectListByType("1");
        if (all == null || all.isEmpty()) {
            List<Long> single = new ArrayList<>(1);
            single.add(rootId);
            return single;
        }
        java.util.Map<Long, List<Long>> parentToChildren = all.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(VpsCategory::getParentId,
                Collectors.mapping(VpsCategory::getId, Collectors.toList())));
        List<Long> result = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            result.add(id);
            List<Long> children = parentToChildren.get(id);
            if (children != null) {
                for (Long child : children) {
                    queue.offer(child);
                }
            }
        }
        return result;
    }

    @Override
    public VpsInstance getById(Long id) {
        return vpsInstanceMapper.selectById(id);
    }

    @Override
    public int insert(VpsInstance row) {
        return vpsInstanceMapper.insert(row);
    }

    @Override
    public int update(VpsInstance row) {
        return vpsInstanceMapper.update(row);
    }

    @Override
    public int deleteById(Long id) {
        if (proxyNodeMapper.countByInstanceId(id) > 0) {
            throw new ServiceException("该实例下存在代理节点，无法删除，请先移除相关代理节点");
        }
        return vpsInstanceMapper.deleteById(id);
    }
}
