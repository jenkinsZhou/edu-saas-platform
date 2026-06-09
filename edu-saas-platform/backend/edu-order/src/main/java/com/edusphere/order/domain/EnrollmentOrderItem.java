package com.edusphere.order.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

import java.math.BigDecimal;

@TableName("enrollment_order_item")
public class EnrollmentOrderItem extends BaseEntity {

    @TableField("order_id")
    private Long orderId;
    @TableField("item_type")
    private String itemType;
    @TableField("item_name")
    private String itemName;
    @TableField("course_product_id")
    private Long courseProductId;
    @TableField("class_group_id")
    private Long classGroupId;
    @TableField("student_id")
    private Long studentId;
    private Integer quantity;
    @TableField("unit_price")
    private BigDecimal unitPrice;
    @TableField("discount_amount")
    private BigDecimal discountAmount;
    private BigDecimal amount;
    @TableField("snapshot_json")
    private String snapshotJson;
    @TableField("sort_no")
    private Integer sortNo;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getCourseProductId() {
        return courseProductId;
    }

    public void setCourseProductId(Long courseProductId) {
        this.courseProductId = courseProductId;
    }

    public Long getClassGroupId() {
        return classGroupId;
    }

    public void setClassGroupId(Long classGroupId) {
        this.classGroupId = classGroupId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
