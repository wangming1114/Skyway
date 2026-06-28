-- Homepage dashboard query indexes.
-- Apply once to existing databases after the dashboard traffic/expiry modules are enabled.

ALTER TABLE `res_proxy_node_traffic`
  ADD INDEX `idx_traffic_stat_node` (`stat_time`, `node_id`);

ALTER TABLE `res_proxy_node`
  ADD INDEX `idx_proxy_node_expire_status` (`expire_time`, `status`);

ALTER TABLE `res_proxy_node_rate_limit`
  ADD INDEX `idx_rate_limit_status` (`status`);
