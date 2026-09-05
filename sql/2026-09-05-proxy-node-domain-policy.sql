-- Rename the existing allowlist JSON column semantically; no data rewrite is required.
ALTER TABLE `res_proxy_node`
  MODIFY COLUMN `domain_policy_json` text COMMENT '域名访问策略JSON(null=不限制，mode=whitelist/blacklist)';
