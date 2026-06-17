package com.edusphere.api.license;

/**
 * 授权运行状态。
 */
public enum LicenseStatus {
    /** 有效。 */
    VALID,
    /** 已过期但仍在宽限期内：功能可用，前端应显著提醒尽快续期。 */
    GRACE,
    /** 已过期且超过宽限期：系统进入只读状态。 */
    EXPIRED,
    /** 授权文件签名不合法 / 被篡改 / 绑定了其它部署：只读。 */
    INVALID,
    /** 尚未上传任何授权（如全新交付未激活）：只读，等待激活。 */
    UNLICENSED;

    /** 是否允许写操作（新增/修改/删除）。 */
    public boolean writable() {
        return this == VALID || this == GRACE;
    }
}
