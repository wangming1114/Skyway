-- 节点端口流量统计（按 node_id 存，删除节点再重加则新 node_id 总量为 0）

-- 1、流量明细表（每周期增量）
drop table if exists res_proxy_node_traffic;
create table res_proxy_node_traffic (
  id         bigint(20)   not null auto_increment comment '主键',
  node_id    bigint(20)   not null                comment '节点ID(res_proxy_node.id)',
  stat_time  datetime     not null                comment '统计时间点',
  rx_delta   bigint(20)   default 0               comment '本周期下行增量(字节)',
  tx_delta   bigint(20)   default 0               comment '本周期上行增量(字节)',
  primary key (id),
  key idx_node_id_stat_time (node_id, stat_time)
) engine=innodb auto_increment=1 comment='代理节点流量明细';

-- 2、快照表（上次计数器，用于算 delta）
drop table if exists res_proxy_node_traffic_snapshot;
create table res_proxy_node_traffic_snapshot (
  node_id    bigint(20)   not null                comment '节点ID(res_proxy_node.id)',
  last_rx    bigint(20)   default 0               comment '上次下行累计字节',
  last_tx    bigint(20)   default 0               comment '上次上行累计字节',
  updated_at datetime                             comment '更新时间',
  primary key (node_id)
) engine=innodb comment='代理节点流量快照';
