-- 节点流量采集定时任务（需先存在 sys_job 表，Skyway 初始化脚本已建）
-- 执行后可在「系统管理 -> 定时任务」中查看、修改 cron 或启停

insert into sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark)
values (4, '节点流量采集', 'DEFAULT', 'proxyNodeTrafficTask.collect', '0 */5 * * * ?', '3', '1', '0', 'admin', sysdate(), '', null, '节点端口流量累计采集（每5分钟）')
on duplicate key update invoke_target = values(invoke_target), cron_expression = values(cron_expression), remark = values(remark);
