package com.edusphere.course.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edusphere.common.domain.BaseEntity;

import java.time.LocalDateTime;

@TableName("lesson_session")
public class LessonSession extends BaseEntity {

    @TableField("class_group_id")
    private Long classGroupId;
    @TableField("teacher_id")
    private Long teacherId;
    @TableField("classroom_id")
    private Long classroomId;
    @TableField("online_room_url")
    private String onlineRoomUrl;
    @TableField("planned_start_at")
    private LocalDateTime plannedStartAt;
    @TableField("planned_end_at")
    private LocalDateTime plannedEndAt;
    @TableField("actual_start_at")
    private LocalDateTime actualStartAt;
    @TableField("actual_end_at")
    private LocalDateTime actualEndAt;
    private String status;

    public Long getClassGroupId() {
        return classGroupId;
    }

    public void setClassGroupId(Long classGroupId) {
        this.classGroupId = classGroupId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Long classroomId) {
        this.classroomId = classroomId;
    }

    public String getOnlineRoomUrl() {
        return onlineRoomUrl;
    }

    public void setOnlineRoomUrl(String onlineRoomUrl) {
        this.onlineRoomUrl = onlineRoomUrl;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(LocalDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(LocalDateTime plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public LocalDateTime getActualStartAt() {
        return actualStartAt;
    }

    public void setActualStartAt(LocalDateTime actualStartAt) {
        this.actualStartAt = actualStartAt;
    }

    public LocalDateTime getActualEndAt() {
        return actualEndAt;
    }

    public void setActualEndAt(LocalDateTime actualEndAt) {
        this.actualEndAt = actualEndAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
