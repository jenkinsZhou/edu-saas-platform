CREATE DATABASE IF NOT EXISTS edu_saas
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE edu_saas;

CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    plan_code VARCHAR(64) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tenant_code (code)
);

CREATE TABLE IF NOT EXISTS tenant_theme (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    primary_color VARCHAR(32) NOT NULL,
    accent_color VARCHAR(32) NOT NULL,
    logo_url VARCHAR(500) NULL,
    layout VARCHAR(32) NOT NULL,
    custom_css_vars_json JSON NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_tenant_theme_tenant (tenant_id)
);

CREATE TABLE IF NOT EXISTS account (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_login_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_account_username (tenant_id, username),
    UNIQUE KEY uk_account_phone (tenant_id, phone),
    KEY idx_account_tenant_status (tenant_id, status),
    KEY idx_account_list (tenant_id, deleted, created_at)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    real_name VARCHAR(80) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    gender VARCHAR(16) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_user_account (tenant_id, account_id)
);

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(80) NOT NULL,
    data_scope VARCHAR(32) NOT NULL,
    system_builtin TINYINT(1) NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (tenant_id, code),
    KEY idx_role_list (tenant_id, deleted, data_scope, id),
    KEY idx_role_builtin (tenant_id, deleted, system_builtin)
);

CREATE TABLE IF NOT EXISTS menu_permission (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(32) NOT NULL,
    route_path VARCHAR(255) NULL,
    permission_code VARCHAR(128) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_menu_parent (tenant_id, parent_id),
    KEY idx_menu_tree (tenant_id, deleted, parent_id, sort_no),
    KEY idx_menu_list (tenant_id, deleted, type, sort_no),
    KEY idx_menu_permission_code (tenant_id, permission_code)
);

CREATE TABLE IF NOT EXISTS account_role (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_role (tenant_id, account_id, role_id)
);

CREATE TABLE IF NOT EXISTS account_campus (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_campus (tenant_id, account_id, campus_id),
    KEY idx_account_campus_account (tenant_id, account_id)
);

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (tenant_id, role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    username VARCHAR(64) NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id BIGINT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    request_id VARCHAR(80) NULL,
    detail VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_log_tenant_time (tenant_id, created_at),
    KEY idx_operation_log_module_time (tenant_id, module, action, created_at),
    KEY idx_operation_log_username_time (tenant_id, username, created_at),
    KEY idx_operation_log_request (request_id)
);

CREATE TABLE IF NOT EXISTS campus (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    address VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_campus_code (tenant_id, code),
    KEY idx_campus_list (tenant_id, deleted, status, id)
);

CREATE TABLE IF NOT EXISTS course_product (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    delivery_mode VARCHAR(32) NOT NULL,
    billing_mode VARCHAR(32) NOT NULL,
    total_lessons INT NULL,
    list_price DECIMAL(12,2) NULL,
    extension_template_code VARCHAR(80) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_course_product_tenant_status (tenant_id, status),
    KEY idx_course_product_category (tenant_id, category_code),
    KEY idx_course_product_list (tenant_id, deleted, status, created_at),
    KEY idx_course_product_owner (tenant_id, created_by, deleted, created_at)
);

CREATE TABLE IF NOT EXISTS class_group (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    course_product_id BIGINT NOT NULL,
    campus_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    head_teacher_id BIGINT NULL,
    capacity INT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_class_course (tenant_id, course_product_id),
    KEY idx_class_campus_status (tenant_id, campus_id, status),
    KEY idx_class_list (tenant_id, deleted, status, created_at),
    KEY idx_class_campus_list (tenant_id, campus_id, deleted, status, created_at)
);

CREATE TABLE IF NOT EXISTS lesson_session (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    teacher_id BIGINT NULL,
    classroom_id BIGINT NULL,
    online_room_url VARCHAR(500) NULL,
    planned_start_at DATETIME NOT NULL,
    planned_end_at DATETIME NOT NULL,
    actual_start_at DATETIME NULL,
    actual_end_at DATETIME NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_lesson_class_time (tenant_id, class_group_id, planned_start_at),
    KEY idx_lesson_teacher_time (tenant_id, teacher_id, planned_start_at),
    KEY idx_lesson_list (tenant_id, deleted, status, planned_start_at),
    KEY idx_lesson_owner (tenant_id, created_by, deleted, planned_start_at)
);

CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(32) NULL,
    guardian_name VARCHAR(80) NULL,
    guardian_phone VARCHAR(32) NULL,
    source VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_student_tenant_status (tenant_id, status),
    KEY idx_student_phone (tenant_id, phone),
    KEY idx_student_guardian_phone (tenant_id, guardian_phone),
    KEY idx_student_list (tenant_id, deleted, status, created_at),
    KEY idx_student_owner (tenant_id, created_by, deleted, created_at)
);

CREATE TABLE IF NOT EXISTS class_enrollment (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    enroll_status VARCHAR(32) NOT NULL,
    enroll_date DATE NULL,
    remark VARCHAR(255) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_class_enrollment (tenant_id, class_group_id, student_id),
    KEY idx_enrollment_student (tenant_id, student_id),
    KEY idx_enrollment_class_status (tenant_id, class_group_id, deleted, enroll_status),
    KEY idx_enrollment_list (tenant_id, deleted, created_at)
);

CREATE TABLE IF NOT EXISTS enrollment_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    order_type VARCHAR(32) NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    pay_status VARCHAR(32) NOT NULL,
    source_channel VARCHAR(64) NULL,
    customer_name VARCHAR(80) NULL,
    customer_phone VARCHAR(32) NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(80) NOT NULL,
    class_group_id BIGINT NOT NULL,
    course_product_id BIGINT NOT NULL,
    campus_id BIGINT NULL,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payable_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    outstanding_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    remark VARCHAR(255) NULL,
    confirmed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_enrollment_order_no (tenant_id, order_no),
    KEY idx_enrollment_order_student (tenant_id, student_id, created_at),
    KEY idx_enrollment_order_class (tenant_id, class_group_id, created_at),
    KEY idx_enrollment_order_status (tenant_id, order_status, pay_status, created_at),
    KEY idx_enrollment_order_list (tenant_id, deleted, created_at),
    KEY idx_enrollment_order_campus_list (tenant_id, campus_id, deleted, created_at),
    KEY idx_enrollment_order_owner (tenant_id, created_by, deleted, created_at)
);

CREATE TABLE IF NOT EXISTS enrollment_order_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    course_product_id BIGINT NULL,
    class_group_id BIGINT NULL,
    student_id BIGINT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    snapshot_json JSON NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_enrollment_order_item_order (tenant_id, order_id, sort_no)
);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_no VARCHAR(64) NOT NULL,
    channel_code VARCHAR(32) NOT NULL,
    channel_trade_no VARCHAR(128) NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(255) NULL,
    requested_at DATETIME NOT NULL,
    paid_at DATETIME NULL,
    closed_at DATETIME NULL,
    last_checked_at DATETIME NULL,
    next_check_at DATETIME NULL,
    check_count INT NOT NULL DEFAULT 0,
    remark VARCHAR(255) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_no (tenant_id, payment_no),
    KEY idx_payment_order_trade (tenant_id, channel_code, channel_trade_no),
    KEY idx_payment_order_status_check (tenant_id, status, next_check_at),
    KEY idx_payment_order_order (tenant_id, order_id, created_at)
);

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_no VARCHAR(64) NOT NULL,
    payment_type VARCHAR(32) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    channel_trade_no VARCHAR(128) NULL,
    received_at DATETIME NOT NULL,
    remark VARCHAR(255) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_record_no (tenant_id, payment_no),
    KEY idx_payment_record_order (tenant_id, order_id, received_at),
    KEY idx_payment_record_method_time (tenant_id, payment_method, received_at),
    KEY idx_payment_record_list (tenant_id, deleted, received_at)
);

CREATE TABLE IF NOT EXISTS refund_order (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    payment_order_id BIGINT NULL,
    channel_code VARCHAR(32) NOT NULL,
    channel_refund_no VARCHAR(128) NULL,
    refund_method VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(255) NULL,
    requested_at DATETIME NOT NULL,
    refunded_at DATETIME NULL,
    closed_at DATETIME NULL,
    last_checked_at DATETIME NULL,
    next_check_at DATETIME NULL,
    check_count INT NOT NULL DEFAULT 0,
    remark VARCHAR(255) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_refund_order_no (tenant_id, refund_no),
    KEY idx_refund_order_channel_no (tenant_id, channel_code, channel_refund_no),
    KEY idx_refund_order_status_check (tenant_id, status, next_check_at),
    KEY idx_refund_order_order (tenant_id, order_id, created_at)
);

CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL,
    refund_type VARCHAR(32) NOT NULL,
    refund_method VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    channel_trade_no VARCHAR(128) NULL,
    refund_reason VARCHAR(255) NULL,
    refunded_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_refund_record_no (tenant_id, refund_no),
    KEY idx_refund_record_order (tenant_id, order_id, refunded_at),
    KEY idx_refund_record_method_time (tenant_id, refund_method, refunded_at),
    KEY idx_refund_record_list (tenant_id, deleted, refunded_at)
);

CREATE TABLE IF NOT EXISTS order_audit_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NULL,
    amount DECIMAL(12,2) NULL,
    detail VARCHAR(500) NULL,
    operator_account_id BIGINT NULL,
    operator_username VARCHAR(64) NULL,
    request_id VARCHAR(80) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_audit_order_time (tenant_id, order_id, created_at),
    KEY idx_order_audit_event_time (tenant_id, event_type, created_at),
    KEY idx_order_audit_request (request_id)
);

CREATE TABLE IF NOT EXISTS payment_callback_log (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_record_id BIGINT NULL,
    callback_no VARCHAR(80) NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    channel_trade_no VARCHAR(128) NULL,
    callback_status VARCHAR(32) NOT NULL,
    raw_payload_json JSON NULL,
    process_result VARCHAR(500) NULL,
    received_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    operator_account_id BIGINT NULL,
    operator_username VARCHAR(64) NULL,
    request_id VARCHAR(80) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_callback_no (tenant_id, callback_no),
    UNIQUE KEY uk_payment_callback_trade (tenant_id, channel_code, channel_trade_no),
    KEY idx_payment_callback_order_time (tenant_id, order_id, received_at),
    KEY idx_payment_callback_request (request_id)
);

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    lesson_session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    checked_at DATETIME NULL,
    remark VARCHAR(255) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_attendance_student_lesson (tenant_id, lesson_session_id, student_id),
    KEY idx_attendance_student (tenant_id, student_id)
);

CREATE TABLE IF NOT EXISTS extension_template (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(100) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_extension_template_code (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS extension_field (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    field_code VARCHAR(80) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_type VARCHAR(32) NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 0,
    options_json JSON NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_extension_field_code (tenant_id, template_id, field_code)
);

CREATE TABLE IF NOT EXISTS business_extension_value (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    field_code VARCHAR(80) NOT NULL,
    field_value JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_extension_value (tenant_id, business_type, business_id, field_code)
);
