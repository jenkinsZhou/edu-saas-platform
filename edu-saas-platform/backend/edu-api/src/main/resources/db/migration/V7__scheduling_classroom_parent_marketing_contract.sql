-- 排课系统、教室、家长、营销、合同功能

-- 教室表
CREATE TABLE IF NOT EXISTS classroom (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campus_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    room_no VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    equipment VARCHAR(500) NULL COMMENT '设备清单，逗号分隔',
    status VARCHAR(32) NOT NULL COMMENT 'AVAILABLE/OCCUPIED/MAINTENANCE',
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_classroom_room (tenant_id, room_no, deleted),
    KEY idx_classroom_status (tenant_id, status, deleted)
) COMMENT '教室资源表';

-- 学员家长关联表
CREATE TABLE IF NOT EXISTS student_parent (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    wechat_id VARCHAR(100) NULL,
    relationship VARCHAR(32) NULL COMMENT 'FATHER/MOTHER/GUARDIAN',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主要联系人',
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_parent_student (tenant_id, student_id, deleted),
    KEY idx_parent_phone (tenant_id, phone)
) COMMENT '学员家长表';

-- 优惠券表
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL COMMENT 'FIXED/PERCENT',
    discount_amount DECIMAL(10,2) NULL,
    discount_percent INT NULL,
    min_order_amount DECIMAL(10,2) NULL,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    total_quantity INT NOT NULL,
    used_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL COMMENT 'ACTIVE/INACTIVE/EXPIRED',
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_coupon_status (tenant_id, status, deleted),
    KEY idx_coupon_valid (tenant_id, valid_from, valid_to)
) COMMENT '优惠券表';

-- 合同表
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    contract_no VARCHAR(64) NOT NULL,
    student_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    contract_type VARCHAR(32) NOT NULL COMMENT 'ENROLLMENT/SERVICE',
    contract_amount DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/ACTIVE/EXPIRED/TERMINATED',
    contract_url VARCHAR(500) NULL COMMENT '合同模板URL',
    signed_url VARCHAR(500) NULL COMMENT '已签署合同URL',
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_contract_no (tenant_id, contract_no, deleted),
    KEY idx_contract_student (tenant_id, student_id, deleted),
    KEY idx_contract_expiry (tenant_id, end_date, status, deleted)
) COMMENT '合同表';
