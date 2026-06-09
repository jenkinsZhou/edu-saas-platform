-- 修复支付回调竞态条件问题 (S3)
-- 为 payment_callback_log 添加 callback_no 的唯一约束，防止重复处理

-- 注意：V2__audit_and_callbacks.sql 已经创建了 payment_callback_log 表并包含唯一约束
-- 本迁移文件用于补充说明和确保约束存在

-- 验证唯一约束是否存在
-- uk_payment_callback_no (tenant_id, callback_no) - 已在V2中创建
-- uk_payment_callback_trade (tenant_id, channel_code, channel_trade_no) - 已在V2中创建

-- 如果需要添加额外的约束或索引，可以在此处添加
-- 例如：为 payment_record 表添加约束

-- 确保 payment_record 表的支付流水号唯一性
-- 检查 payment_record 表结构后再添加
