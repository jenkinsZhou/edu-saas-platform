package com.edusphere.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableField("account_id")
    private Long accountId;
    @TableField("real_name")
    private String realName;
    @TableField("avatar_url")
    private String avatarUrl;
    private String gender;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
