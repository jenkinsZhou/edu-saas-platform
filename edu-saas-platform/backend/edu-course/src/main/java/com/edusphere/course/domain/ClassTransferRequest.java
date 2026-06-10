package com.edusphere.course.domain;

import com.edusphere.common.domain.BaseEntity;

import java.time.LocalDateTime;

public class ClassTransferRequest extends BaseEntity {
    private Long tenantId;
    private Long studentId;
    private Long fromClassGroupId;
    private Long toClassGroupId;
    private Long fromLessonSessionId;
    private Long toLessonSessionId;
    private String transferType;
    private String reason;
    private String status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approvalRemark;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getFromClassGroupId() {
        return fromClassGroupId;
    }

    public void setFromClassGroupId(Long fromClassGroupId) {
        this.fromClassGroupId = fromClassGroupId;
    }

    public Long getToClassGroupId() {
        return toClassGroupId;
    }

    public void setToClassGroupId(Long toClassGroupId) {
        this.toClassGroupId = toClassGroupId;
    }

    public Long getFromLessonSessionId() {
        return fromLessonSessionId;
    }

    public void setFromLessonSessionId(Long fromLessonSessionId) {
        this.fromLessonSessionId = fromLessonSessionId;
    }

    public Long getToLessonSessionId() {
        return toLessonSessionId;
    }

    public void setToLessonSessionId(Long toLessonSessionId) {
        this.toLessonSessionId = toLessonSessionId;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }
}
