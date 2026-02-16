package com.skyway.member.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyway.common.annotation.Excel;
import com.skyway.common.core.domain.BaseEntity;

/**
 * 会员客户表 mb_customer
 *
 * @author ruoyi
 */
public class MbCustomer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键(customer_id) */
    @Excel(name = "客户ID")
    private Long id;

    /** 用户名 */
    @Excel(name = "用户名")
    private String username;

    /** 密码 */
    private String password;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 微信号 */
    @Excel(name = "微信号")
    private String wechat;

    /** QQ号 */
    @Excel(name = "QQ号")
    private String qq;

    /** 头像URL */
    private String avatar;

    /** 状态(0=正常 1=停用) */
    @Excel(name = "状态", readConverterExp = "0=启用,1=禁用")
    private String status;

    /** 注册时间 */
    @Excel(name = "注册时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date registerTime;

    /** 最近登录时间 */
    @Excel(name = "最近登录时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginAt;

    /** 最近登录IP */
    @Excel(name = "最近登录IP")
    private String lastLoginIp;

    /** 关联节点数（查询列表时填充，非表字段） */
    private Long nodeBindCount;

    /** 关键字（仅查询：用户名/手机号模糊，非表字段） */
    private String keyword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /** 密码：允许反序列化（前端提交），禁止序列化（不返回给前端） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public Long getNodeBindCount() {
        return nodeBindCount;
    }

    public void setNodeBindCount(Long nodeBindCount) {
        this.nodeBindCount = nodeBindCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
