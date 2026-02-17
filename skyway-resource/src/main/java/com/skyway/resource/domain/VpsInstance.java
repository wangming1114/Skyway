package com.skyway.resource.domain;

import java.util.Date;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.skyway.common.annotation.Excel;
import com.skyway.common.core.domain.BaseEntity;

/**
 * VPS 实例表 res_instance
 *
 * @author ruoyi
 */
public class VpsInstance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** VPS名称 */
    @Excel(name = "VPS名称")
    private String name;

    /** 所属分类 */
    private Long categoryId;

    /** 所属节点 */
    private Long nodeId;

    /** IP */
    @Excel(name = "IP")
    private String ip;

    /** SSH端口 */
    private Integer sshPort;

    /** SSH登录账号 */
    private String sshUsername;

    /** SSH登录密码（不参与Excel导出） */
    private String sshPassword;

    /** CPU规格 */
    @Excel(name = "CPU")
    private String cpu;

    /** 内存规格 */
    @Excel(name = "内存")
    private String memory;

    /** 磁盘规格 */
    @Excel(name = "磁盘")
    private String disk;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "running=运行中,stopped=已停止,abnormal=异常")
    private String status;

    /** 网络类型（res_instance_network_type） */
    @Excel(name = "网络类型")
    private String networkType;

    /** 系统类型（centos, ubuntu, debian, alpine, other） */
    @Excel(name = "系统类型")
    private String osType;

    /** 系统版本（如 7.9, 24.04） */
    @Excel(name = "系统版本")
    private String osVersion;

    /** 带宽（如 50M） */
    @Excel(name = "带宽")
    private String bandwidth;

    /** 流量限制（字节，null 或 0 表示不限制） */
    @Excel(name = "流量限制(字节)")
    private Long trafficLimit;

    /** 续费金额（如 10/月、100/年） */
    @Excel(name = "续费金额")
    private String renewalAmount;

    /** 到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "到期时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 已开通节点数（关联查询，不持久化） */
    private Integer nodeCount;

    /** 累计流量字节数（关联汇总，不持久化） */
    private Long totalTrafficBytes;

    /** 分类名称（关联查询，不持久化） */
    @Excel(name = "分类")
    private String categoryName;

    /** 节点名称（关联查询，不持久化） */
    @Excel(name = "节点")
    private String nodeName;

    /** 关键字搜索（名称/ID/IP，不持久化） */
    private String keyword;

    /** 分类ID列表（查询用：选中节点及其所有子分类，不持久化） */
    private java.util.List<Long> categoryIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotBlank(message = "VPS名称不能为空")
    @Size(min = 0, max = 100, message = "VPS名称长度不能超过100个字符")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Size(min = 0, max = 50, message = "IP长度不能超过50个字符")
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Min(value = 1, message = "SSH端口范围为1-65535")
    @Max(value = 65535, message = "SSH端口范围为1-65535")
    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    @Size(min = 0, max = 64, message = "SSH账号长度不能超过64个字符")
    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    @Size(min = 0, max = 255, message = "SSH密码长度不能超过255个字符")
    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    @Size(min = 0, max = 50, message = "CPU规格长度不能超过50个字符")
    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    @Size(min = 0, max = 50, message = "内存规格长度不能超过50个字符")
    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    @Size(min = 0, max = 50, message = "磁盘规格长度不能超过50个字符")
    public String getDisk() {
        return disk;
    }

    public void setDisk(String disk) {
        this.disk = disk;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Size(min = 0, max = 32, message = "网络类型长度不能超过32个字符")
    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Size(min = 0, max = 32, message = "系统类型长度不能超过32个字符")
    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    @Size(min = 0, max = 64, message = "系统版本长度不能超过64个字符")
    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    @Size(min = 0, max = 32, message = "带宽长度不能超过32个字符")
    public String getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(String bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public java.util.List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(java.util.List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public Long getTrafficLimit() {
        return trafficLimit;
    }

    public void setTrafficLimit(Long trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    public String getRenewalAmount() {
        return renewalAmount;
    }

    public void setRenewalAmount(String renewalAmount) {
        this.renewalAmount = renewalAmount;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Long getTotalTrafficBytes() {
        return totalTrafficBytes;
    }

    public void setTotalTrafficBytes(Long totalTrafficBytes) {
        this.totalTrafficBytes = totalTrafficBytes;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("name", getName())
                .append("categoryId", getCategoryId())
                .append("nodeId", getNodeId())
                .append("ip", getIp())
                .append("sshPort", getSshPort())
                .append("sshUsername", getSshUsername())
                .append("cpu", getCpu())
                .append("memory", getMemory())
                .append("disk", getDisk())
                .append("status", getStatus())
                .append("remark", getRemark())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
