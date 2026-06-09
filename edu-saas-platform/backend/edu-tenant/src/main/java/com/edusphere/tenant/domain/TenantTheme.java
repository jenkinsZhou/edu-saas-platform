package com.edusphere.tenant.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

@TableName("tenant_theme")
public class TenantTheme extends BaseEntity {

    private String name;
    @TableField("primary_color")
    private String primaryColor;
    @TableField("accent_color")
    private String accentColor;
    @TableField("logo_url")
    private String logoUrl;
    private String layout;
    @TableField("custom_css_vars_json")
    private String customCssVarsJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getCustomCssVarsJson() {
        return customCssVarsJson;
    }

    public void setCustomCssVarsJson(String customCssVarsJson) {
        this.customCssVarsJson = customCssVarsJson;
    }
}
