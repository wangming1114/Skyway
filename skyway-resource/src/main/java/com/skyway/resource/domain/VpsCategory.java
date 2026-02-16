package com.skyway.resource.domain;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.skyway.common.core.domain.BaseEntity;

/**
 * 资源分类与节点表 res_category（分类与节点合并，type 区分）
 *
 * @author ruoyi
 */
public class VpsCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 父ID（0为根） */
    private Long parentId;

    /** 名称 */
    private String name;

    /** 排序 */
    private Integer orderNum;

    /** 类型（1=分类 2=节点） */
    private String type;

    /** 子节点 */
    private List<VpsCategory> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @NotBlank(message = "名称不能为空")
    @Size(min = 0, max = 100, message = "名称长度不能超过100个字符")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull(message = "排序不能为空")
    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<VpsCategory> getChildren() {
        return children;
    }

    public void setChildren(List<VpsCategory> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("parentId", getParentId())
                .append("name", getName())
                .append("orderNum", getOrderNum())
                .append("type", getType())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
