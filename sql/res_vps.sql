-- ----------------------------
-- 资源管理 - VPS 管理（按需执行）
-- 表前缀 res_；分类与节点合并为 res_category，通过 type 区分
-- ----------------------------

-- 1、res_category（分类与节点合并表）
drop table if exists res_category;
create table res_category (
  id              bigint(20)      not null auto_increment    comment '主键',
  parent_id       bigint(20)     default 0                   comment '父ID（0为根；分类建树，节点一般为0）',
  name            varchar(100)   default ''                   comment '名称',
  order_num       int(4)         default 0                   comment '排序',
  type            char(1)        default '1'                  comment '类型（1=分类 2=节点）',
  create_time     datetime                                   comment '创建时间',
  update_time     datetime                                   comment '更新时间',
  primary key (id)
) engine=innodb auto_increment=1 comment = '资源分类与节点表';

-- 2、res_instance（VPS 实例）
drop table if exists res_instance;
create table res_instance (
  id              bigint(20)     not null auto_increment    comment '主键',
  name            varchar(100)   default ''                   comment 'VPS名称',
  category_id     bigint(20)     default null                comment '所属分类(res_category.type=1)',
  node_id         bigint(20)     default null                comment '所属节点(res_category.type=2)',
  ip              varchar(50)    default null                comment 'IP',
  ssh_port        int            default 22                  comment 'SSH端口',
  ssh_username    varchar(64)   default null                comment 'SSH登录账号',
  ssh_password    varchar(255)  default null                comment 'SSH登录密码',
  cpu             varchar(50)   default null                comment 'CPU规格',
  memory          varchar(50)   default null                comment '内存规格',
  disk            varchar(50)   default null                comment '磁盘规格',
  status          varchar(20)   default 'running'          comment '状态(res_instance_status)',
  traffic_limit   bigint(20)    default null               comment '流量限制(字节，NULL/0=不限制)',
  renewal_amount  varchar(100)  default null               comment '续费金额(如10/月、100/年)',
  expire_time     datetime      default null                comment '到期时间',
  remark          varchar(500)  default null                comment '备注',
  create_time     datetime                                   comment '创建时间',
  update_time     datetime                                   comment '更新时间',
  primary key (id)
) engine=innodb auto_increment=1 comment = 'VPS实例表';

-- 3、字典类型
insert into sys_dict_type values(11, '资源分类类型', 'res_category_type', '0', 'admin', sysdate(), '', null, '分类/节点');
insert into sys_dict_type values(12, 'VPS实例状态', 'res_instance_status', '0', 'admin', sysdate(), '', null, '运行中/已停止/异常');

-- 4、字典数据 res_category_type
insert into sys_dict_data values(30, 1, '分类', '1', 'res_category_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '左侧树分类');
insert into sys_dict_data values(31, 2, '节点', '2', 'res_category_type', '', 'default', 'N', '0', 'admin', sysdate(), '', null, '所属节点');

-- 5、字典数据 res_instance_status
insert into sys_dict_data values(32, 1, '运行中', 'running', 'res_instance_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(33, 2, '已停止', 'stopped', 'res_instance_status', '', 'info', 'N', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(34, 3, '异常', 'abnormal', 'res_instance_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '');

-- 6、菜单：资源管理（一级）
insert into sys_menu values(200, '资源管理', 0, 5, 'resource', null, null, '', 1, 0, 'M', '0', '0', '', 'tree', 'admin', sysdate(), '', null, '资源管理目录');

-- 7、菜单：VPS 管理（二级）
insert into sys_menu values(201, 'VPS管理', 200, 1, 'vps', 'resource/vps/index', null, '', 1, 0, 'C', '0', '0', 'resource:vps:list', 'server', 'admin', sysdate(), '', null, 'VPS管理菜单');

-- 8、按钮权限
insert into sys_menu values(202, 'VPS查询', 201, 1, '', null, null, '', 1, 0, 'F', '0', '0', 'resource:vps:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(203, 'VPS新增', 201, 2, '', null, null, '', 1, 0, 'F', '0', '0', 'resource:vps:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(204, 'VPS修改', 201, 3, '', null, null, '', 1, 0, 'F', '0', '0', 'resource:vps:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(205, 'VPS删除', 201, 4, '', null, null, '', 1, 0, 'F', '0', '0', 'resource:vps:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values(206, 'VPS导出', 201, 5, '', null, null, '', 1, 0, 'F', '0', '0', 'resource:vps:export', '#', 'admin', sysdate(), '', null, '');

-- 已有 res_instance 表时执行：补充 SSH 字段
-- ALTER TABLE res_instance
--   ADD COLUMN ssh_port int DEFAULT 22 COMMENT 'SSH端口',
--   ADD COLUMN ssh_username varchar(64) DEFAULT NULL COMMENT 'SSH登录账号',
--   ADD COLUMN ssh_password varchar(255) DEFAULT NULL COMMENT 'SSH登录密码';

-- 已有 res_instance 表时执行：流量限制、续费、到期时间
-- ALTER TABLE res_instance
--   ADD COLUMN traffic_limit bigint(20) DEFAULT NULL COMMENT '流量限制(字节，NULL/0=不限制)',
--   ADD COLUMN renewal_amount varchar(100) DEFAULT NULL COMMENT '续费金额(如10/月、100/年)',
--   ADD COLUMN expire_time datetime DEFAULT NULL COMMENT '到期时间';

-- 9、res_proxy_node（代理节点表）
drop table if exists res_proxy_node;
create table res_proxy_node (
  id              bigint(20)     not null auto_increment  comment '主键',
  instance_id     bigint(20)     not null                 comment 'VPS实例ID(res_instance.id)',
  node_name       varchar(100)   default ''               comment '节点名称/备注',
  node_type       varchar(60)    not null                 comment '节点类型(VLESS-REALITY等)',
  address         varchar(255)   not null                 comment '地址(IP/域名)',
  port            int            not null                 comment '端口',
  url             varchar(2000)  default null             comment '完整分享链接',
  config_json     text           default null             comment '协议配置参数(JSON)',
  expire_time     datetime       default null             comment '有效期(null=永久有效)',
  custom_id       varchar(64)    default null             comment '自定义用户ID(可空)',
  status          char(1)        default '0'              comment '状态(0=正常 1=停用)',
  remark          varchar(500)   default null             comment '备注',
  create_by       varchar(64)    default ''               comment '创建者',
  create_time     datetime                                comment '创建时间',
  update_by       varchar(64)    default ''               comment '更新者',
  update_time     datetime                                comment '更新时间',
  primary key (id),
  key idx_instance_id (instance_id)
) engine=innodb auto_increment=1 comment='代理节点表';

-- 已有 res_proxy_node 表时执行：增加 custom_id 字段
-- ALTER TABLE res_proxy_node ADD COLUMN custom_id varchar(64) DEFAULT NULL COMMENT '自定义用户ID(可空)';

-- 10、字典类型：代理节点状态
insert into sys_dict_type values(13, '代理节点状态', 'res_proxy_node_status', '0', 'admin', sysdate(), '', null, '正常/停用');

-- 11、字典数据 res_proxy_node_status
insert into sys_dict_data values(35, 1, '正常', '0', 'res_proxy_node_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(36, 2, '停用', '1', 'res_proxy_node_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '');

-- 12、菜单：代理节点（二级，与 VPS 管理同级）
insert into sys_menu values(207, '代理节点', 200, 2, 'proxyNode', 'resource/vps/proxyNode/index', null, '', 1, 0, 'C', '0', '0', 'resource:vps:list', 'link', 'admin', sysdate(), '', null, '代理节点列表菜单');
