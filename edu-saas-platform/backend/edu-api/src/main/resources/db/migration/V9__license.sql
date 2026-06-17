-- 私有化部署授权（License）存储。每个部署仅一条记录（单机构本地化交付）。
-- 与 tenant 表一样不带 tenant_id：授权是“整套部署”维度，而非租户维度。
CREATE TABLE IF NOT EXISTS system_license (
    id           BIGINT       NOT NULL PRIMARY KEY,
    license_text LONGTEXT      NOT NULL COMMENT '签名后的授权字符串：base64url(payload).base64url(signature)',
    license_id   VARCHAR(64)  NULL COMMENT '授权编号（payload.licenseId，便于排查）',
    customer     VARCHAR(128) NULL COMMENT '被授权客户名称',
    expires_at   DATE         NULL COMMENT '到期日期（冗余，便于查询）',
    uploaded_by  BIGINT       NULL,
    uploaded_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '部署授权许可';
