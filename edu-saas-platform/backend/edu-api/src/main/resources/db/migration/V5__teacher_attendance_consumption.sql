-- 教师管理、考勤打卡、课时消耗功能

-- 教师表
CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) NULL,
    gender VARCHAR(16) NULL,
    subjects VARCHAR(200) NULL COMMENT '授课科目，逗号分隔',
    title VARCHAR(64) NULL COMMENT '职称',
    status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/INACTIVE/RESIGNED',
    avatar_url VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_teacher_employee (tenant_id, employee_no, deleted),
    KEY idx_teacher_list (tenant_id, deleted, status, created_at),
    KEY idx_teacher_phone (tenant_id, phone)
) COMMENT '教师表';

-- 课时消耗表
CREATE TABLE IF NOT EXISTS course_consumption (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    lesson_session_id BIGINT NULL,
    enrollment_id BIGINT NOT NULL COMMENT '关联的班级报名记录',
    consumed_count INT NOT NULL DEFAULT 1 COMMENT '消耗课时数',
    consumption_type VARCHAR(32) NOT NULL COMMENT 'NORMAL/MAKEUP/TRIAL',
    remark VARCHAR(200) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_consumption_student (tenant_id, student_id, deleted, created_at),
    KEY idx_consumption_class (tenant_id, class_group_id, deleted, created_at),
    KEY idx_consumption_enrollment (tenant_id, enrollment_id, deleted)
) COMMENT '课时消耗记录';

-- 为 lesson_session 表添加教师字段（如果不存在）
ALTER TABLE lesson_session
    ADD COLUMN IF NOT EXISTS teacher_id BIGINT NULL COMMENT '授课教师' AFTER class_group_id,
    ADD INDEX IF NOT EXISTS idx_lesson_teacher (tenant_id, teacher_id, deleted);
