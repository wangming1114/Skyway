-- VPS 实例状态同步定时任务（通过 SSH 探测自动更新状态）
-- 执行后可在「系统管理 -> 定时任务」中查看、修改 cron 或启停

insert into sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark)
values (5, 'VPS状态同步', 'DEFAULT', 'vpsInstanceStatusSyncTask.sync', '0 */10 * * * ?', '3', '1', '0', 'admin', sysdate(), '', null, 'SSH 探测各 VPS 可达性并更新状态（每10分钟）')
on duplicate key update invoke_target = values(invoke_target), cron_expression = values(cron_expression), remark = values(remark);
