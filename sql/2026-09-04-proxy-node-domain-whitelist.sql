-- Proxy node per-domain access policy. Apply once to existing databases.
ALTER TABLE `res_proxy_node`
  ADD COLUMN `domain_policy_json` text COMMENT '域名访问策略JSON(null=不限制，mode=whitelist/blacklist)' AFTER `config_json`;
