-- 性能优化：添加缺失的复合索引

-- 优化考勤查询
ALTER TABLE attendance_record
    ADD INDEX IF NOT EXISTS idx_attendance_lesson (tenant_id, lesson_session_id, deleted),
    ADD INDEX IF NOT EXISTS idx_attendance_student_time (tenant_id, student_id, check_in_at, deleted);

-- 优化课消查询
ALTER TABLE course_consumption
    ADD INDEX IF NOT EXISTS idx_consumption_enrollment (tenant_id, enrollment_id, deleted),
    ADD INDEX IF NOT EXISTS idx_consumption_lesson (tenant_id, lesson_session_id, deleted);

-- 优化排课冲突检测（最关键）
ALTER TABLE lesson_session
    ADD INDEX IF NOT EXISTS idx_lesson_teacher_time_range (tenant_id, teacher_id, deleted, planned_start_at, planned_end_at),
    ADD INDEX IF NOT EXISTS idx_lesson_classroom_time_range (tenant_id, classroom_id, deleted, planned_start_at, planned_end_at),
    ADD INDEX IF NOT EXISTS idx_lesson_class_time_range (tenant_id, class_group_id, deleted, planned_start_at, planned_end_at);

-- 优化报名查询
ALTER TABLE class_enrollment
    ADD INDEX IF NOT EXISTS idx_enrollment_student_status (tenant_id, student_id, enroll_status, deleted);

-- 优化订单查询
ALTER TABLE enrollment_order
    ADD INDEX IF NOT EXISTS idx_order_student (tenant_id, student_id, deleted, created_at),
    ADD INDEX IF NOT EXISTS idx_order_pay_status (tenant_id, pay_status, deleted, created_at);

-- 优化通知查询
ALTER TABLE notification
    ADD INDEX IF NOT EXISTS idx_notification_sent (tenant_id, status, sent_at);

-- 优化转班申请查询
ALTER TABLE class_transfer_request
    ADD INDEX IF NOT EXISTS idx_transfer_approval (tenant_id, status, approved_at);

-- 优化合同到期查询
ALTER TABLE contract
    ADD INDEX IF NOT EXISTS idx_contract_expiry_alert (tenant_id, status, end_date, deleted);
