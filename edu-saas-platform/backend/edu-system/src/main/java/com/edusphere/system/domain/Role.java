package com.edusphere.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;
import com.edusphere.security.context.DataScope;

@TableName("role")
public class Role extends BaseEntity {

    private String name;
    private String code;
    @TableField("data_scope")
    private DataScope dataScope;
    @TableField("system_builtin")
    private Boolean systemBuiltin;

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

    public DataScope getDataScope() {
        return dataScope;
    }

    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
    }

    public Boolean getSystemBuiltin() {
        return systemBuiltin;
    }

    public void setSystemBuiltin(Boolean systemBuiltin) {
        this.systemBuiltin = systemBuiltin;
    }
}
