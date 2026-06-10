package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.*;
import com.edusphere.course.mapper.*;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {

    private final LessonSessionMapper lessonSessionMapper;
    private final TeacherMapper teacherMapper;
    private final ClassroomMapper classroomMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;

    public SchedulingController(LessonSessionMapper lessonSessionMapper,
                                TeacherMapper teacherMapper,
                                ClassroomMapper classroomMapper,
                                ClassEnrollmentMapper classEnrollmentMapper) {
        this.lessonSessionMapper = lessonSessionMapper;
        this.teacherMapper = teacherMapper;
        this.classroomMapper = classroomMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
    }

    @PostMapping("/check-conflict")
    @RequirePermission("course:schedule:view")
    public ApiResult<ConflictCheckResult> checkConflict(@RequestBody @Valid CheckConflictRequest request) {
        Long tenantId = SecurityContext.tenantId();
        List<String> conflicts = new ArrayList<>();

        if (request.teacherId() != null) {
            long count = lessonSessionMapper.selectCount(
                    new LambdaQueryWrapper<LessonSession>()
                            .eq(LessonSession::getTenantId, tenantId)
                            .eq(LessonSession::getTeacherId, request.teacherId())
                            .eq(LessonSession::getDeleted, false)
                            .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
                            .le(LessonSession::getPlannedStartAt, request.endTime())
                            .ge(LessonSession::getPlannedEndAt, request.startTime())
            );
            if (count > 0) {
                conflicts.add("教师时间冲突：该时间段已有其他课程安排");
            }
        }

        if (request.classroomId() != null) {
            long count = lessonSessionMapper.selectCount(
                    new LambdaQueryWrapper<LessonSession>()
                            .eq(LessonSession::getTenantId, tenantId)
                            .eq(LessonSession::getClassroomId, request.classroomId())
                            .eq(LessonSession::getDeleted, false)
                            .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
                            .le(LessonSession::getPlannedStartAt, request.endTime())
                            .ge(LessonSession::getPlannedEndAt, request.startTime())
            );
            if (count > 0) {
                conflicts.add("教室时间冲突：该时间段教室已被占用");
            }
        }

        if (request.classGroupId() != null) {
            List<Long> studentIds = classEnrollmentMapper.selectList(
                    new LambdaQueryWrapper<ClassEnrollment>()
                            .select(ClassEnrollment::getStudentId)
                            .eq(ClassEnrollment::getTenantId, tenantId)
                            .eq(ClassEnrollment::getClassGroupId, request.classGroupId())
                            .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                            .eq(ClassEnrollment::getDeleted, false)
            ).stream().map(ClassEnrollment::getStudentId).toList();

            if (!studentIds.isEmpty()) {
                List<Long> conflictClassGroups = classEnrollmentMapper.selectList(
                        new LambdaQueryWrapper<ClassEnrollment>()
                                .select(ClassEnrollment::getClassGroupId)
                                .eq(ClassEnrollment::getTenantId, tenantId)
                                .in(ClassEnrollment::getStudentId, studentIds)
                                .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                                .eq(ClassEnrollment::getDeleted, false)
                ).stream().map(ClassEnrollment::getClassGroupId).distinct().toList();

                if (!conflictClassGroups.isEmpty()) {
                    long count = lessonSessionMapper.selectCount(
                            new LambdaQueryWrapper<LessonSession>()
                                    .eq(LessonSession::getTenantId, tenantId)
                                    .in(LessonSession::getClassGroupId, conflictClassGroups)
                                    .eq(LessonSession::getDeleted, false)
                                    .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
                                    .le(LessonSession::getPlannedStartAt, request.endTime())
                                    .ge(LessonSession::getPlannedEndAt, request.startTime())
                    );
                    if (count > 0) {
                        conflicts.add("学员时间冲突：班级内有学员在该时间段已有其他课程");
                    }
                }
            }
        }

        return ApiResult.ok(new ConflictCheckResult(conflicts.isEmpty(), conflicts));
    }

    @PostMapping("/create-lesson")
    @Transactional
    @RequirePermission("course:schedule:create")
    public ApiResult<Long> createLesson(@RequestBody @Valid CreateLessonRequest request) {
        Long tenantId = SecurityContext.tenantId();

        ConflictCheckResult conflictCheck = checkConflict(new CheckConflictRequest(
                request.teacherId(),
                request.classroomId(),
                request.classGroupId(),
                request.startTime(),
                request.endTime(),
                null
        )).getData();

        if (!conflictCheck.hasNoConflict()) {
            throw new BizException(409, "排课冲突：" + String.join("; ", conflictCheck.conflicts()));
        }

        LessonSession lesson = new LessonSession();
        lesson.setTenantId(tenantId);
        lesson.setClassGroupId(request.classGroupId());
        lesson.setTeacherId(request.teacherId());
        lesson.setClassroomId(request.classroomId());
        lesson.setPlannedStartAt(request.startTime());
        lesson.setPlannedEndAt(request.endTime());
        lesson.setStatus("SCHEDULED");
        lesson.setCreatedBy(SecurityContext.accountId());
        lessonSessionMapper.insert(lesson);

        return ApiResult.ok(lesson.getId());
    }

    private boolean isTimeConflict(LocalDateTime start1, LocalDateTime end1,
                                    LocalDateTime start2, LocalDateTime end2) {
        return !(end1.isBefore(start2) || start1.isAfter(end2) ||
                end1.isEqual(start2) || start1.isEqual(end2));
    }

    public record CheckConflictRequest(
            Long teacherId,
            Long classroomId,
            Long classGroupId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            Long excludeLessonId
    ) {}

    public record CreateLessonRequest(
            @NotNull Long classGroupId,
            Long teacherId,
            Long classroomId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {}

    public record ConflictCheckResult(boolean hasNoConflict, List<String> conflicts) {}
}
