-- Proxy node per-domain allowlist. Apply once to existing databases.
ALTER TABLE `res_proxy_node`
  ADD COLUMN `domain_policy_json` text COMMENT '域名白名单策略JSON(null=不限制)' AFTER `config_json`;
