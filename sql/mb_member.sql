-- ----------------------------
-- 会员中心模块（表前缀 mb_）
-- ----------------------------

-- 1、mb_customer（客户表，独立于 sys_user）
drop table if exists mb_customer;
create table mb_customer (
  id                bigint(20)     not null auto_increment  comment '主键(customer_id)',
  username          varchar(64)     not null                 comment '用户名',
  password          varchar(128)    default ''               comment '密码',
  email             varchar(128)    not null                 comment '邮箱',
  phone             varchar(20)     default null             comment '手机号',
  wechat            varchar(64)     default null             comment '微信号',
  qq                varchar(20)     default null             comment 'QQ号',
  avatar            varchar(255)    default null             comment '头像URL',
  status            char(1)        default '0'              comment '状态(0=正常 1=停用)',
  register_time     datetime       default null             comment '注册时间',
  last_login_at     datetime       default null             comment '最近登录时间',
  last_login_ip     varchar(64)    default null             comment '最近登录IP',
  remark            varchar(500)   default null             comment '备注',
  create_by         varchar(64)    default ''               comment '创建者',
  create_time       datetime                                comment '创建时间',
  update_by         varchar(64)    default ''               comment '更新者',
  update_time       datetime                                comment '更新时间',
  primary key (id),
  unique key uk_username (username),
  key idx_status (status)
) engine=innodb auto_increment=1 comment='会员客户表';

-- 2、res_proxy_node 增加 customer_id（关联 mb_customer.id）
-- 已有 res_proxy_node 表时执行（若已添加过可跳过）：
alter table res_proxy_node add column customer_id bigint(20) default null comment '归属客户ID(mb_customer.id)' after instance_id;
alter table res_proxy_node add key idx_customer_id (customer_id);

-- 3、菜单：用户中心（一级）
insert into sys_menu values(210, '用户中心', 0, 6, 'member', null, null, '', 1, 0, 'M', '0', '0', '', 'peoples', 'admin', sysdate(), '', null, '用户中心目录');

-- 4、菜单：客户管理（二级）
insert into sys_menu values(211, '客户管理', 210, 1, 'customer', 'member/customer/index', null, '', 1, 0, 'C', '0', '0', 'member:customer:list', 'user', 'admin', sysdate(), '', null, '客户管理菜单');

-- 5、按钮权限
insert into sys_menu values(212, '客户查询', 211, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(213, '客户新增', 211, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(214, '客户修改', 211, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(215, '客户删除', 211, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(216, '重置密码', 211, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:resetPwd', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(217, '客户导出', 211, 6, '', null, null, '', 1, 0, 'F', '0', '0', 'member:customer:export', '#', 'admin', sysdate(), '', null, '');

-- 6、如果已有旧版 mb_customer 需要迁移字段，可用以下增量 ALTER（可选）：
-- alter table mb_customer drop column nickname;
-- alter table mb_customer drop column role;
-- alter table mb_customer add column email varchar(128) not null comment '邮箱' after password;
-- alter table mb_customer add column phone varchar(20) default null comment '手机号' after email;
-- alter table mb_customer add column wechat varchar(64) default null comment '微信号' after phone;
-- alter table mb_customer add column qq varchar(20) default null comment 'QQ号' after wechat;
-- alter table mb_customer add column avatar varchar(255) default null comment '头像URL' after qq;
-- alter table mb_customer add column register_time datetime default null comment '注册时间' after status;
