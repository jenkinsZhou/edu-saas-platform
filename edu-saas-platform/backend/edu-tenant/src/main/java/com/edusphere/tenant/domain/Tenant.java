package com.edusphere.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

@TableName("tenant")
public class Tenant extends BaseEntity {

    /**
     * tenant 是租户根表，自身没有 tenant_id 列，遮蔽父类字段避免 SQL 引用不存在的列
     */
    @TableField(exist = false)
    private Long tenantId;

    private String name;
    private String code;
    private String status;
    private String planCode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }
}
