-- Manual VPS port rate-limit upgrade script.
-- Apply this to existing databases. Fresh installs can use skyway.sql directly.

CREATE TABLE IF NOT EXISTS `res_proxy_node_rate_limit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance_id` bigint(20) NOT NULL COMMENT 'VPS实例ID',
  `proxy_node_id` bigint(20) NOT NULL COMMENT '代理节点ID',
  `port` int(11) NOT NULL COMMENT '端口号',
  `download_mbps` int(11) NOT NULL COMMENT '下载限速Mbps',
  `upload_mbps` int(11) NOT NULL COMMENT '上传限速Mbps',
  `expire_time` datetime DEFAULT NULL COMMENT '限速到期时间，NULL=永久',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态 active/removed/expired/failed',
  `last_apply_result` varchar(2000) DEFAULT NULL COMMENT '最近一次远端应用结果',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_proxy_node_status` (`proxy_node_id`, `status`) USING BTREE,
  KEY `idx_instance_status` (`instance_id`, `status`) USING BTREE,
  KEY `idx_expire_status` (`expire_time`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='代理节点端口限速表';

INSERT INTO `sys_job`
  (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT
  '端口限速到期清理',
  'DEFAULT',
  'proxyNodeRateLimitExpireTask.processExpired',
  '0 */1 * * * ?',
  '3',
  '1',
  '0',
  'admin',
  NOW(),
  '',
  NULL,
  '到期限速规则自动 SSH 删除远端端口限速并重载 TC 规则（每分钟）'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_job`
  WHERE `invoke_target` = 'proxyNodeRateLimitExpireTask.processExpired'
);
