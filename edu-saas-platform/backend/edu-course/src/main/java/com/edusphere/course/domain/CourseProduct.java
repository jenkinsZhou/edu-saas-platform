package com.edusphere.course.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

import java.math.BigDecimal;

@TableName("course_product")
public class CourseProduct extends BaseEntity {

    private String name;
    @TableField("category_code")
    private String categoryCode;
    @TableField("delivery_mode")
    private String deliveryMode;
    @TableField("billing_mode")
    private String billingMode;
    @TableField("total_lessons")
    private Integer totalLessons;
    @TableField("list_price")
    private BigDecimal listPrice;
    @TableField("extension_template_code")
    private String extensionTemplateCode;
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getBillingMode() {
        return billingMode;
    }

    public void setBillingMode(String billingMode) {
        this.billingMode = billingMode;
    }

    public Integer getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(Integer totalLessons) {
        this.totalLessons = totalLessons;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }

    public String getExtensionTemplateCode() {
        return extensionTemplateCode;
    }

    public void setExtensionTemplateCode(String extensionTemplateCode) {
        this.extensionTemplateCode = extensionTemplateCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
