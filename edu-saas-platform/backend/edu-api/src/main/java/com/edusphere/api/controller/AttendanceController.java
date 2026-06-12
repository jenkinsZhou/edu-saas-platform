package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.AttendanceRecord;
import com.edusphere.course.domain.CourseConsumption;
import com.edusphere.course.domain.LessonSession;
import com.edusphere.course.domain.ClassEnrollment;
import com.edusphere.course.mapper.AttendanceRecordMapper;
import com.edusphere.course.mapper.CourseConsumptionMapper;
import com.edusphere.course.mapper.LessonSessionMapper;
import com.edusphere.course.mapper.ClassEnrollmentMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LessonSessionMapper lessonSessionMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;
    private final CourseConsumptionMapper courseConsumptionMapper;

    public AttendanceController(AttendanceRecordMapper attendanceRecordMapper,
                                LessonSessionMapper lessonSessionMapper,
                                ClassEnrollmentMapper classEnrollmentMapper,
                                CourseConsumptionMapper courseConsumptionMapper) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.lessonSessionMapper = lessonSessionMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
        this.courseConsumptionMapper = courseConsumptionMapper;
    }

    @GetMapping
    @RequirePermission("course:attendance:view")
    public ApiResult<PageResult<AttendanceRecord>> listAttendance(
            @RequestParam(required = false) Long lessonSessionId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getTenantId, tenantId)
                .eq(AttendanceRecord::getDeleted, false);

        if (lessonSessionId != null) {
            wrapper.eq(AttendanceRecord::getLessonSessionId, lessonSessionId);
        }
        if (studentId != null) {
            wrapper.eq(AttendanceRecord::getStudentId, studentId);
        }

        wrapper.orderByDesc(AttendanceRecord::getCheckedAt);
        Page<AttendanceRecord> result = attendanceRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/check-in")
    @Transactional
    @RequirePermission("course:attendance:create")
    public ApiResult<Long> checkIn(@RequestBody @Valid CheckInRequest request) {
        Long tenantId = SecurityContext.tenantId();

        LessonSession lesson = lessonSessionMapper.selectOne(new LambdaQueryWrapper<LessonSession>()
                .eq(LessonSession::getTenantId, tenantId)
                .eq(LessonSession::getId, request.lessonSessionId())
                .eq(LessonSession::getDeleted, false));
        if (lesson == null) {
            throw new BizException(404, "课时不存在");
        }

        AttendanceRecord existing = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getTenantId, tenantId)
                .eq(AttendanceRecord::getLessonSessionId, request.lessonSessionId())
                .eq(AttendanceRecord::getStudentId, request.studentId())
                .eq(AttendanceRecord::getDeleted, false));
        if (existing != null) {
            throw new BizException(409, "该学员已签到");
        }

        ClassEnrollment enrollment = classEnrollmentMapper.selectOne(new LambdaQueryWrapper<ClassEnrollment>()
                .eq(ClassEnrollment::getTenantId, tenantId)
                .eq(ClassEnrollment::getClassGroupId, lesson.getClassGroupId())
                .eq(ClassEnrollment::getStudentId, request.studentId())
                .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                .eq(ClassEnrollment::getDeleted, false));

        AttendanceRecord attendance = new AttendanceRecord();
        attendance.setTenantId(tenantId);
        attendance.setLessonSessionId(request.lessonSessionId());
        attendance.setStudentId(request.studentId());
        attendance.setStatus("PRESENT");
        attendance.setCheckedAt(LocalDateTime.now());
        attendance.setRemark(request.remark());
        attendance.setCreatedBy(SecurityContext.accountId());
        attendanceRecordMapper.insert(attendance);

        if (enrollment != null && "PRESENT".equals(attendance.getStatus())) {
            CourseConsumption consumption = new CourseConsumption();
            consumption.setTenantId(tenantId);
            consumption.setStudentId(request.studentId());
            consumption.setClassGroupId(lesson.getClassGroupId());
            consumption.setLessonSessionId(request.lessonSessionId());
            consumption.setEnrollmentId(enrollment.getId());
            consumption.setConsumedCount(1);
            consumption.setConsumptionType("NORMAL");
            consumption.setRemark("正常上课消耗");
            consumption.setCreatedBy(SecurityContext.accountId());
            courseConsumptionMapper.insert(consumption);
        }

        return ApiResult.ok(attendance.getId());
    }

    @PutMapping("/{id}/status")
    @RequirePermission("course:attendance:update")
    public ApiResult<Void> updateAttendanceStatus(@PathVariable Long id, @RequestBody @Valid UpdateAttendanceRequest request) {
        Long tenantId = SecurityContext.tenantId();
        AttendanceRecord attendance = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getTenantId, tenantId)
                .eq(AttendanceRecord::getId, id)
                .eq(AttendanceRecord::getDeleted, false));
        if (attendance == null) {
            throw new BizException(404, "考勤记录不存在");
        }

        attendance.setStatus(request.status());
        attendance.setRemark(request.remark());
        attendance.setUpdatedBy(SecurityContext.accountId());
        attendanceRecordMapper.updateById(attendance);
        return ApiResult.ok();
    }

    public record CheckInRequest(
            @NotNull Long lessonSessionId,
            @NotNull Long studentId,
            String remark
    ) {}

    public record UpdateAttendanceRequest(
            @NotNull String status,
            String remark
    ) {}
}
