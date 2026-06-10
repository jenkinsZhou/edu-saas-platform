package com.edusphere.course.domain;

import com.edusphere.common.domain.BaseEntity;

public class CourseConsumption extends BaseEntity {
    private Long tenantId;
    private Long studentId;
    private Long classGroupId;
    private Long lessonSessionId;
    private Long enrollmentId;
    private Integer consumedCount;
    private String consumptionType;
    private String remark;

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

    public Long getClassGroupId() {
        return classGroupId;
    }

    public void setClassGroupId(Long classGroupId) {
        this.classGroupId = classGroupId;
    }

    public Long getLessonSessionId() {
        return lessonSessionId;
    }

    public void setLessonSessionId(Long lessonSessionId) {
        this.lessonSessionId = lessonSessionId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Integer getConsumedCount() {
        return consumedCount;
    }

    public void setConsumedCount(Integer consumedCount) {
        this.consumedCount = consumedCount;
    }

    public String getConsumptionType() {
        return consumptionType;
    }

    public void setConsumptionType(String consumptionType) {
        this.consumptionType = consumptionType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
