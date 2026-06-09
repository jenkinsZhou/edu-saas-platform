package com.edusphere.course.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

import java.time.LocalDateTime;

@TableName("attendance_record")
public class AttendanceRecord extends BaseEntity {

    @TableField("lesson_session_id")
    private Long lessonSessionId;
    @TableField("student_id")
    private Long studentId;
    private String status;
    @TableField("checked_at")
    private LocalDateTime checkedAt;
    private String remark;

    public Long getLessonSessionId() {
        return lessonSessionId;
    }

    public void setLessonSessionId(Long lessonSessionId) {
        this.lessonSessionId = lessonSessionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
