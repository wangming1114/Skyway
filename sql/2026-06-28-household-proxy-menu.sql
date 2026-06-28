-- 在“资源管理”下增加家宽代理网站入口，和 VPS 管理、代理节点同级。
-- 适用于已部署数据库；新初始化库已同步更新 sql/skyway.sql。

UPDATE `sys_menu`
SET
  `menu_name` = '代理节点',
  `parent_id` = 200,
  `order_num` = 2,
  `path` = 'proxyNode',
  `component` = 'resource/vps/proxyNode/index',
  `query` = NULL,
  `route_name` = '',
  `is_frame` = 1,
  `is_cache` = 0,
  `menu_type` = 'C',
  `visible` = '0',
  `status` = '0',
  `perms` = 'resource:vps:list',
  `icon` = 'link',
  `update_by` = 'admin',
  `update_time` = NOW(),
  `remark` = '代理节点列表菜单'
WHERE `menu_id` = 207;

DELETE FROM `sys_role_menu` WHERE `menu_id` = 208;
DELETE FROM `sys_menu` WHERE `menu_id` = 208;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 209, '家宽代理', 200, 3, 'householdProxy', 'resource/vps/householdProxy/index', NULL, '', 1, 0, 'C', '0', '0', 'resource:vps:list', 'link', 'admin', NOW(), '', NULL, '家宽代理网站内嵌菜单'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 209);

UPDATE `sys_menu`
SET
  `menu_name` = '家宽代理',
  `parent_id` = 200,
  `order_num` = 3,
  `path` = 'householdProxy',
  `component` = 'resource/vps/householdProxy/index',
  `query` = NULL,
  `route_name` = '',
  `is_frame` = 1,
  `is_cache` = 0,
  `menu_type` = 'C',
  `visible` = '0',
  `status` = '0',
  `perms` = 'resource:vps:list',
  `icon` = 'link',
  `update_by` = 'admin',
  `update_time` = NOW(),
  `remark` = '家宽代理网站内嵌菜单'
WHERE `menu_id` = 209;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 209
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 209);
