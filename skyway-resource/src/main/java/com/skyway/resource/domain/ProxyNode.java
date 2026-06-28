package com.skyway.resource.domain;

import java.util.Date;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.skyway.common.core.domain.BaseEntity;

/**
 * 代理节点表 res_proxy_node
 *
 * @author ruoyi
 */
public class ProxyNode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** VPS实例ID */
    private Long instanceId;

    /** 归属客户ID(mb_customer.id) */
    private Long customerId;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型(VLESS-REALITY等) */
    private String nodeType;

    /** 地址(IP/域名) */
    private String address;

    /** 端口 */
    private Integer port;

    /** 完整分享链接 */
    private String url;

    /** 协议配置参数(JSON) */
    private String configJson;

    /** 有效期(null=永久有效) */
    private Date expireTime;

    /** 自定义用户ID(可空) */
    private String customId;

    /** 状态(0=正常 1=停用) */
    private String status;

    /** 当前端口限速规则 */
    private ProxyNodeRateLimit rateLimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotNull(message = "VPS实例ID不能为空")
    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    @Size(min = 0, max = 100, message = "节点名称长度不能超过100个字符")
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @NotBlank(message = "节点类型不能为空")
    @Size(min = 0, max = 60, message = "节点类型长度不能超过60个字符")
    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    @NotBlank(message = "地址不能为空")
    @Size(min = 0, max = 255, message = "地址长度不能超过255个字符")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口范围为1-65535")
    @Max(value = 65535, message = "端口范围为1-65535")
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Size(min = 0, max = 2000, message = "分享链接长度不能超过2000个字符")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    @Size(min = 0, max = 64, message = "custom_id长度不能超过64个字符")
    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ProxyNodeRateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(ProxyNodeRateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("instanceId", getInstanceId())
                .append("customerId", getCustomerId())
                .append("nodeName", getNodeName())
                .append("nodeType", getNodeType())
                .append("address", getAddress())
                .append("port", getPort())
                .append("url", getUrl())
                .append("expireTime", getExpireTime())
                .append("customId", getCustomId())
                .append("status", getStatus())
                .append("rateLimit", getRateLimit())
                .append("remark", getRemark())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
