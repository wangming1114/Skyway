-- 节点到期停用与邮件通知定时任务（需先存在 sys_job 表，Skyway 初始化脚本已建）
-- 执行后可在「系统管理 -> 定时任务」中查看、修改 cron 或启停
-- 每小时整点执行：查询已到期且状态为正常的节点，按列表「停止」流程停用并发送管理员/客户通知邮件

insert into sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark)
values (6, '节点到期停用与通知', 'DEFAULT', 'proxyNodeExpireTask.processExpired', '0 0 * * * ?', '3', '1', '0', 'admin', sysdate(), '', null, '到期节点自动停用并邮件通知管理员与客户（每小时）')
on duplicate key update invoke_target = values(invoke_target), cron_expression = values(cron_expression), remark = values(remark);
