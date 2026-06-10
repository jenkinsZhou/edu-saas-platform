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
            List<LessonSession> teacherLessons = lessonSessionMapper.selectList(
                    new LambdaQueryWrapper<LessonSession>()
                            .eq(LessonSession::getTenantId, tenantId)
                            .eq(LessonSession::getTeacherId, request.teacherId())
                            .eq(LessonSession::getDeleted, false)
                            .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
            );

            for (LessonSession lesson : teacherLessons) {
                if (isTimeConflict(lesson.getPlannedStartAt(), lesson.getPlannedEndAt(),
                        request.startTime(), request.endTime())) {
                    conflicts.add(String.format("教师时间冲突：已安排课程 %s ~ %s",
                            lesson.getPlannedStartAt(), lesson.getPlannedEndAt()));
                }
            }
        }

        if (request.classroomId() != null) {
            List<LessonSession> classroomLessons = lessonSessionMapper.selectList(
                    new LambdaQueryWrapper<LessonSession>()
                            .eq(LessonSession::getTenantId, tenantId)
                            .eq(LessonSession::getClassroomId, request.classroomId())
                            .eq(LessonSession::getDeleted, false)
                            .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
            );

            for (LessonSession lesson : classroomLessons) {
                if (isTimeConflict(lesson.getPlannedStartAt(), lesson.getPlannedEndAt(),
                        request.startTime(), request.endTime())) {
                    conflicts.add(String.format("教室时间冲突：已被占用 %s ~ %s",
                            lesson.getPlannedStartAt(), lesson.getPlannedEndAt()));
                }
            }
        }

        if (request.classGroupId() != null) {
            List<ClassEnrollment> enrollments = classEnrollmentMapper.selectList(
                    new LambdaQueryWrapper<ClassEnrollment>()
                            .eq(ClassEnrollment::getTenantId, tenantId)
                            .eq(ClassEnrollment::getClassGroupId, request.classGroupId())
                            .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                            .eq(ClassEnrollment::getDeleted, false)
            );

            for (ClassEnrollment enrollment : enrollments) {
                List<LessonSession> studentLessons = lessonSessionMapper.selectList(
                        new LambdaQueryWrapper<LessonSession>()
                                .eq(LessonSession::getTenantId, tenantId)
                                .eq(LessonSession::getDeleted, false)
                                .ne(request.excludeLessonId() != null, LessonSession::getId, request.excludeLessonId())
                                .in(LessonSession::getClassGroupId,
                                        classEnrollmentMapper.selectList(
                                                new LambdaQueryWrapper<ClassEnrollment>()
                                                        .eq(ClassEnrollment::getTenantId, tenantId)
                                                        .eq(ClassEnrollment::getStudentId, enrollment.getStudentId())
                                                        .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                                                        .eq(ClassEnrollment::getDeleted, false)
                                        ).stream().map(ClassEnrollment::getClassGroupId).toList()
                                )
                );

                for (LessonSession lesson : studentLessons) {
                    if (isTimeConflict(lesson.getPlannedStartAt(), lesson.getPlannedEndAt(),
                            request.startTime(), request.endTime())) {
                        conflicts.add(String.format("学员时间冲突：学员ID %d 已有课程 %s ~ %s",
                                enrollment.getStudentId(), lesson.getPlannedStartAt(), lesson.getPlannedEndAt()));
                        break;
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
