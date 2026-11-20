-- Run once on existing MySQL 5.7+ databases.
ALTER TABLE res_proxy_node
  ADD COLUMN listen_port INT NULL,
  ADD COLUMN ws_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  ADD COLUMN subscription_token CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
  ADD UNIQUE KEY uk_ws_key (ws_key),
  ADD UNIQUE KEY uk_ws_subscription (subscription_token),
  ADD UNIQUE KEY uk_ws_listener (instance_id,listen_port);

CREATE TABLE IF NOT EXISTS res_ws_operation (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  sequence_no BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
  instance_id BIGINT NOT NULL, node_id BIGINT NULL, reserved_port INT NULL,
  UNIQUE KEY uk_ws_reserved_port (instance_id,reserved_port),
  kind VARCHAR(12) NOT NULL, actor VARCHAR(64) NOT NULL,
  request_json TEXT NOT NULL, context_json TEXT NOT NULL,
  state VARCHAR(16) NOT NULL, stage VARCHAR(500) NOT NULL,
  attempts INT NOT NULL DEFAULT 0, next_run DATETIME NOT NULL,
  created_at DATETIME NOT NULL, KEY idx_ws_work (state,next_run),
  KEY idx_ws_node_work (node_id,state), KEY idx_ws_instance_work (instance_id,kind,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS res_ws_domain (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  node_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  instance_id BIGINT NOT NULL, hostname VARCHAR(253) CHARACTER SET ascii NOT NULL,
  zone_id CHAR(32) CHARACTER SET ascii NOT NULL, record_id CHAR(32) CHARACTER SET ascii NULL,
  origin VARCHAR(45) CHARACTER SET ascii NOT NULL, proxied TINYINT NOT NULL,
  state VARCHAR(16) NOT NULL, renew_at DATETIME NULL,
  UNIQUE KEY uk_ws_hostname (hostname), UNIQUE KEY uk_ws_domain_op (operation_id),
  KEY idx_ws_domain_node (node_key), KEY idx_ws_domain_instance (instance_id,state), KEY idx_ws_cert (state,renew_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_config (config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Cloudflare 根域名','skyway.cloudflare.domain','skyway666.sbs','Y','admin',NOW(),'WS-TLS 节点的根域名'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='skyway.cloudflare.domain');
INSERT INTO sys_config (config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Cloudflare Zone ID','skyway.cloudflare.zoneId','','Y','admin',NOW(),'Cloudflare 区域标识'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='skyway.cloudflare.zoneId');
INSERT INTO sys_config (config_name,config_key,config_value,config_type,create_by,create_time,remark)
SELECT 'Cloudflare API Token','skyway.cloudflare.apiToken','','Y','admin',NOW(),'只写，权限限本区域 DNS 编辑及区域读取'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key='skyway.cloudflare.apiToken');
