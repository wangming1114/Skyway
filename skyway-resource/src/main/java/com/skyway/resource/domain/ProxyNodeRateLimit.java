package com.skyway.resource.domain;

import java.util.Date;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.skyway.common.core.domain.BaseEntity;

/**
 * 代理节点端口限速表 res_proxy_node_rate_limit
 */
public class ProxyNodeRateLimit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REMOVED = "removed";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_FAILED = "failed";

    private Long id;
    private Long instanceId;
    private Long proxyNodeId;
    private Integer port;
    private Integer downloadMbps;
    private Integer uploadMbps;
    private Date expireTime;
    private String status;
    private String lastApplyResult;

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

    @NotNull(message = "代理节点ID不能为空")
    public Long getProxyNodeId() {
        return proxyNodeId;
    }

    public void setProxyNodeId(Long proxyNodeId) {
        this.proxyNodeId = proxyNodeId;
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

    @NotNull(message = "下载限速不能为空")
    @Min(value = 1, message = "下载限速必须大于0")
    public Integer getDownloadMbps() {
        return downloadMbps;
    }

    public void setDownloadMbps(Integer downloadMbps) {
        this.downloadMbps = downloadMbps;
    }

    @NotNull(message = "上传限速不能为空")
    @Min(value = 1, message = "上传限速必须大于0")
    public Integer getUploadMbps() {
        return uploadMbps;
    }

    public void setUploadMbps(Integer uploadMbps) {
        this.uploadMbps = uploadMbps;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastApplyResult() {
        return lastApplyResult;
    }

    public void setLastApplyResult(String lastApplyResult) {
        this.lastApplyResult = lastApplyResult;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("instanceId", getInstanceId())
                .append("proxyNodeId", getProxyNodeId())
                .append("port", getPort())
                .append("downloadMbps", getDownloadMbps())
                .append("uploadMbps", getUploadMbps())
                .append("expireTime", getExpireTime())
                .append("status", getStatus())
                .append("lastApplyResult", getLastApplyResult())
                .append("remark", getRemark())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
