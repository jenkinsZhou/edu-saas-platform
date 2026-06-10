-- 调课转班、通知系统、续费提醒功能

-- 转班申请表
CREATE TABLE IF NOT EXISTS class_transfer_request (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    from_class_group_id BIGINT NULL COMMENT '原班级（转班时必填）',
    to_class_group_id BIGINT NULL COMMENT '目标班级（转班时必填）',
    from_lesson_session_id BIGINT NULL COMMENT '原课时（调课时必填）',
    to_lesson_session_id BIGINT NULL COMMENT '目标课时（调课时必填）',
    transfer_type VARCHAR(32) NOT NULL COMMENT 'CLASS_TRANSFER/LESSON_TRANSFER',
    reason VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED',
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    approval_remark VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_transfer_student (tenant_id, student_id, deleted, status),
    KEY idx_transfer_status (tenant_id, status, created_at)
) COMMENT '转班调课申请表';

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    notification_type VARCHAR(64) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_type VARCHAR(32) NOT NULL COMMENT 'STUDENT/TEACHER/ACCOUNT',
    target_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/SENT/FAILED',
    channel VARCHAR(100) NOT NULL COMMENT 'SMS/WECHAT/EMAIL，逗号分隔',
    sent_at DATETIME NULL,
    error_message VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_notification_target (tenant_id, target_type, target_id, deleted),
    KEY idx_notification_status (tenant_id, status, created_at),
    KEY idx_notification_type (tenant_id, notification_type, deleted)
) COMMENT '通知记录表';
