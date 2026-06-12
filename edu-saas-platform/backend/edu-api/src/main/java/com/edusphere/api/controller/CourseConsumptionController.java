package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.CourseConsumption;
import com.edusphere.course.domain.ClassEnrollment;
import com.edusphere.course.mapper.CourseConsumptionMapper;
import com.edusphere.course.mapper.ClassEnrollmentMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-consumption")
public class CourseConsumptionController {

    private final CourseConsumptionMapper courseConsumptionMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;
    private final JdbcTemplate jdbcTemplate;

    public CourseConsumptionController(CourseConsumptionMapper courseConsumptionMapper,
                                       ClassEnrollmentMapper classEnrollmentMapper,
                                       JdbcTemplate jdbcTemplate) {
        this.courseConsumptionMapper = courseConsumptionMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @RequirePermission("course:consumption:view")
    public ApiResult<PageResult<CourseConsumption>> listConsumption(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<CourseConsumption> wrapper = new LambdaQueryWrapper<CourseConsumption>()
                .eq(CourseConsumption::getTenantId, tenantId)
                .eq(CourseConsumption::getDeleted, false);

        if (studentId != null) {
            wrapper.eq(CourseConsumption::getStudentId, studentId);
        }
        if (classGroupId != null) {
            wrapper.eq(CourseConsumption::getClassGroupId, classGroupId);
        }

        wrapper.orderByDesc(CourseConsumption::getCreatedAt);
        Page<CourseConsumption> result = courseConsumptionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/summary")
    @RequirePermission("course:consumption:view")
    public ApiResult<List<Map<String, Object>>> getConsumptionSummary(@RequestParam Long studentId) {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                ce.id as enrollment_id,
                ce.class_group_id,
                ce.student_id,
                COALESCE(cp.total_lessons, 0) as total_sessions,
                COALESCE(SUM(cc.consumed_count), 0) as consumed_sessions,
                (COALESCE(cp.total_lessons, 0) - COALESCE(SUM(cc.consumed_count), 0)) as remaining_sessions
            FROM class_enrollment ce
            INNER JOIN class_group cg ON cg.id = ce.class_group_id AND cg.deleted = 0
            LEFT JOIN course_product cp ON cp.id = cg.course_product_id AND cp.deleted = 0
            LEFT JOIN course_consumption cc ON cc.enrollment_id = ce.id AND cc.deleted = 0
            WHERE ce.tenant_id = ? AND ce.student_id = ? AND ce.deleted = 0 AND ce.enroll_status = 'ACTIVE'
            GROUP BY ce.id, ce.class_group_id, ce.student_id, cp.total_lessons
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("enrollmentId", rs.getLong("enrollment_id"));
                    map.put("classGroupId", rs.getLong("class_group_id"));
                    map.put("studentId", rs.getLong("student_id"));
                    map.put("totalSessions", rs.getInt("total_sessions"));
                    map.put("consumedSessions", rs.getInt("consumed_sessions"));
                    map.put("remainingSessions", rs.getInt("remaining_sessions"));
                    return map;
                },
                tenantId, studentId);

        return ApiResult.ok(results);
    }

    @GetMapping("/alert")
    @RequirePermission("course:consumption:view")
    public ApiResult<List<Map<String, Object>>> getConsumptionAlert(@RequestParam(defaultValue = "3") int threshold) {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                ce.id as enrollment_id,
                ce.class_group_id,
                ce.student_id,
                COALESCE(cp.total_lessons, 0) as total_sessions,
                COALESCE(SUM(cc.consumed_count), 0) as consumed_sessions,
                (COALESCE(cp.total_lessons, 0) - COALESCE(SUM(cc.consumed_count), 0)) as remaining_sessions
            FROM class_enrollment ce
            INNER JOIN class_group cg ON cg.id = ce.class_group_id AND cg.deleted = 0
            LEFT JOIN course_product cp ON cp.id = cg.course_product_id AND cp.deleted = 0
            LEFT JOIN course_consumption cc ON cc.enrollment_id = ce.id AND cc.deleted = 0
            WHERE ce.tenant_id = ? AND ce.deleted = 0 AND ce.enroll_status = 'ACTIVE'
            GROUP BY ce.id, ce.class_group_id, ce.student_id, cp.total_lessons
            HAVING remaining_sessions <= ? AND remaining_sessions > 0
            ORDER BY remaining_sessions ASC
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("enrollmentId", rs.getLong("enrollment_id"));
                    map.put("classGroupId", rs.getLong("class_group_id"));
                    map.put("studentId", rs.getLong("student_id"));
                    map.put("totalSessions", rs.getInt("total_sessions"));
                    map.put("consumedSessions", rs.getInt("consumed_sessions"));
                    map.put("remainingSessions", rs.getInt("remaining_sessions"));
                    return map;
                },
                tenantId, threshold);

        return ApiResult.ok(results);
    }
}
