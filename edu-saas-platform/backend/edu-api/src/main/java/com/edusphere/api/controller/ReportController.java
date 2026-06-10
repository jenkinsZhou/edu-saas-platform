package com.edusphere.api.controller;

import com.edusphere.common.api.ApiResult;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final JdbcTemplate jdbcTemplate;

    public ReportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/revenue")
    @RequirePermission("report:revenue:view")
    public ApiResult<Map<String, Object>> getRevenueReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Long tenantId = SecurityContext.tenantId();
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        String sql = """
            SELECT
                COUNT(DISTINCT eo.id) as total_orders,
                SUM(eo.payable_amount) as total_revenue,
                SUM(eo.paid_amount) as collected_amount,
                SUM(eo.refunded_amount) as refunded_amount,
                SUM(eo.paid_amount - eo.refunded_amount) as net_revenue
            FROM enrollment_order eo
            WHERE eo.tenant_id = ? AND eo.deleted = 0
                AND DATE(eo.created_at) BETWEEN ? AND ?
            """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, tenantId, startDate, endDate);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return ApiResult.ok(result);
    }

    @GetMapping("/revenue/daily")
    @RequirePermission("report:revenue:view")
    public ApiResult<List<Map<String, Object>>> getDailyRevenue(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Long tenantId = SecurityContext.tenantId();
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        String sql = """
            SELECT
                DATE(eo.created_at) as date,
                COUNT(DISTINCT eo.id) as order_count,
                SUM(eo.paid_amount) as daily_revenue,
                SUM(eo.refunded_amount) as daily_refund
            FROM enrollment_order eo
            WHERE eo.tenant_id = ? AND eo.deleted = 0
                AND DATE(eo.created_at) BETWEEN ? AND ?
            GROUP BY DATE(eo.created_at)
            ORDER BY date DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tenantId, startDate, endDate);
        return ApiResult.ok(results);
    }

    @GetMapping("/student")
    @RequirePermission("report:student:view")
    public ApiResult<Map<String, Object>> getStudentReport() {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                COUNT(DISTINCT s.id) as total_students,
                COUNT(DISTINCT CASE WHEN ce.enroll_status = 'ACTIVE' THEN s.id END) as active_students,
                COUNT(DISTINCT CASE WHEN ce.enroll_status = 'COMPLETED' THEN s.id END) as completed_students,
                COUNT(DISTINCT CASE WHEN ce.enroll_status = 'DROPPED' THEN s.id END) as dropped_students
            FROM student s
            LEFT JOIN class_enrollment ce ON ce.student_id = s.id AND ce.deleted = 0
            WHERE s.tenant_id = ? AND s.deleted = 0
            """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, tenantId);
        return ApiResult.ok(result);
    }

    @GetMapping("/student/enrollment")
    @RequirePermission("report:student:view")
    public ApiResult<List<Map<String, Object>>> getStudentEnrollmentStats() {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                cg.id as class_group_id,
                cg.name as class_name,
                COUNT(DISTINCT ce.student_id) as enrolled_count,
                cg.capacity,
                (cg.capacity - COUNT(DISTINCT ce.student_id)) as remaining_capacity
            FROM class_group cg
            LEFT JOIN class_enrollment ce ON ce.class_group_id = cg.id
                AND ce.enroll_status = 'ACTIVE' AND ce.deleted = 0
            WHERE cg.tenant_id = ? AND cg.deleted = 0
            GROUP BY cg.id, cg.name, cg.capacity
            ORDER BY enrolled_count DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tenantId);
        return ApiResult.ok(results);
    }

    @GetMapping("/teacher")
    @RequirePermission("report:teacher:view")
    public ApiResult<List<Map<String, Object>>> getTeacherStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Long tenantId = SecurityContext.tenantId();
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        String sql = """
            SELECT
                t.id as teacher_id,
                t.name as teacher_name,
                COUNT(DISTINCT ls.id) as total_lessons,
                COUNT(DISTINCT ls.class_group_id) as class_count,
                COUNT(DISTINCT ar.id) as attendance_count
            FROM teacher t
            LEFT JOIN lesson_session ls ON ls.teacher_id = t.id AND ls.deleted = 0
                AND DATE(ls.planned_start_at) BETWEEN ? AND ?
            LEFT JOIN attendance_record ar ON ar.lesson_session_id = ls.id AND ar.deleted = 0
            WHERE t.tenant_id = ? AND t.deleted = 0 AND t.status = 'ACTIVE'
            GROUP BY t.id, t.name
            ORDER BY total_lessons DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, startDate, endDate, tenantId);
        return ApiResult.ok(results);
    }

    @GetMapping("/attendance")
    @RequirePermission("report:attendance:view")
    public ApiResult<Map<String, Object>> getAttendanceReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Long tenantId = SecurityContext.tenantId();
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        String sql = """
            SELECT
                COUNT(ar.id) as total_records,
                COUNT(CASE WHEN ar.attendance_status = 'PRESENT' THEN 1 END) as present_count,
                COUNT(CASE WHEN ar.attendance_status = 'ABSENT' THEN 1 END) as absent_count,
                COUNT(CASE WHEN ar.attendance_status = 'LATE' THEN 1 END) as late_count,
                ROUND(COUNT(CASE WHEN ar.attendance_status = 'PRESENT' THEN 1 END) * 100.0 / NULLIF(COUNT(ar.id), 0), 2) as attendance_rate
            FROM attendance_record ar
            WHERE ar.tenant_id = ? AND ar.deleted = 0
                AND DATE(ar.check_in_at) BETWEEN ? AND ?
            """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql, tenantId, startDate, endDate);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return ApiResult.ok(result);
    }

    @GetMapping("/course-consumption")
    @RequirePermission("report:consumption:view")
    public ApiResult<List<Map<String, Object>>> getCourseConsumptionReport() {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                cg.id as class_group_id,
                cg.name as class_name,
                COUNT(DISTINCT ce.student_id) as student_count,
                SUM(ce.total_sessions) as total_sessions,
                SUM(COALESCE(cc.consumed, 0)) as consumed_sessions,
                SUM(ce.total_sessions - COALESCE(cc.consumed, 0)) as remaining_sessions
            FROM class_group cg
            INNER JOIN class_enrollment ce ON ce.class_group_id = cg.id
                AND ce.enroll_status = 'ACTIVE' AND ce.deleted = 0
            LEFT JOIN (
                SELECT enrollment_id, SUM(consumed_count) as consumed
                FROM course_consumption
                WHERE deleted = 0
                GROUP BY enrollment_id
            ) cc ON cc.enrollment_id = ce.id
            WHERE cg.tenant_id = ? AND cg.deleted = 0
            GROUP BY cg.id, cg.name
            ORDER BY student_count DESC
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tenantId);
        return ApiResult.ok(results);
    }

    @GetMapping("/dashboard")
    @RequirePermission("report:dashboard:view")
    public ApiResult<Map<String, Object>> getDashboard() {
        Long tenantId = SecurityContext.tenantId();

        Map<String, Object> dashboard = new LinkedHashMap<>();

        String revenueSql = "SELECT SUM(paid_amount - refunded_amount) as revenue FROM enrollment_order WHERE tenant_id = ? AND deleted = 0 AND DATE(created_at) >= ?";
        BigDecimal monthRevenue = jdbcTemplate.queryForObject(revenueSql, BigDecimal.class, tenantId, LocalDate.now().withDayOfMonth(1));
        dashboard.put("monthRevenue", monthRevenue != null ? monthRevenue : BigDecimal.ZERO);

        String studentSql = "SELECT COUNT(DISTINCT s.id) FROM student s INNER JOIN class_enrollment ce ON ce.student_id = s.id AND ce.enroll_status = 'ACTIVE' WHERE s.tenant_id = ? AND s.deleted = 0 AND ce.deleted = 0";
        Integer activeStudents = jdbcTemplate.queryForObject(studentSql, Integer.class, tenantId);
        dashboard.put("activeStudents", activeStudents != null ? activeStudents : 0);

        String teacherSql = "SELECT COUNT(*) FROM teacher WHERE tenant_id = ? AND deleted = 0 AND status = 'ACTIVE'";
        Integer activeTeachers = jdbcTemplate.queryForObject(teacherSql, Integer.class, tenantId);
        dashboard.put("activeTeachers", activeTeachers != null ? activeTeachers : 0);

        String attendanceSql = "SELECT ROUND(COUNT(CASE WHEN attendance_status = 'PRESENT' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 2) FROM attendance_record WHERE tenant_id = ? AND deleted = 0 AND DATE(check_in_at) >= ?";
        BigDecimal attendanceRate = jdbcTemplate.queryForObject(attendanceSql, BigDecimal.class, tenantId, LocalDate.now().withDayOfMonth(1));
        dashboard.put("monthAttendanceRate", attendanceRate != null ? attendanceRate : BigDecimal.ZERO);

        return ApiResult.ok(dashboard);
    }
}
