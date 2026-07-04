-- 客户订阅临时访问链接
create table if not exists mb_customer_temp_share (
  id bigint(20) not null auto_increment comment '主键',
  customer_id bigint(20) not null comment '客户ID',
  token varchar(64) not null comment '公开访问令牌',
  access_password varchar(128) not null comment '访问密码BCrypt哈希',
  expire_time datetime not null comment '过期时间',
  status char(1) default '0' comment '状态（0正常 1作废）',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (id),
  unique key uk_mb_customer_temp_share_token (token),
  key idx_mb_customer_temp_share_customer (customer_id),
  key idx_mb_customer_temp_share_expire (expire_time)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='客户订阅临时访问链接';
