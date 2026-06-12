package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import com.edusphere.system.domain.Notification;
import com.edusphere.system.mapper.NotificationMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final JdbcTemplate jdbcTemplate;

    public NotificationController(NotificationMapper notificationMapper, JdbcTemplate jdbcTemplate) {
        this.notificationMapper = notificationMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @RequirePermission("system:notification:view")
    public ApiResult<PageResult<Notification>> listNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notificationType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getTenantId, tenantId)
                .eq(Notification::getDeleted, false);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Notification::getStatus, status);
        }
        if (notificationType != null && !notificationType.isBlank()) {
            wrapper.eq(Notification::getNotificationType, notificationType);
        }

        wrapper.orderByDesc(Notification::getCreatedAt);
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/send")
    @RequirePermission("system:notification:send")
    public ApiResult<Long> sendNotification(@RequestBody @Valid SendNotificationRequest request) {
        Long tenantId = SecurityContext.tenantId();

        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setNotificationType(request.notificationType());
        notification.setTitle(request.title());
        notification.setContent(request.content());
        notification.setTargetType(request.targetType());
        notification.setTargetId(request.targetId());
        notification.setStatus("PENDING");
        notification.setChannel(request.channel());
        notification.setCreatedBy(SecurityContext.accountId());
        notificationMapper.insert(notification);

        return ApiResult.ok(notification.getId());
    }

    @GetMapping("/renewal-alert")
    @RequirePermission("system:notification:view")
    public ApiResult<List<Map<String, Object>>> getRenewalAlert(@RequestParam(defaultValue = "3") int daysThreshold) {
        Long tenantId = SecurityContext.tenantId();

        String sql = """
            SELECT
                ce.id as enrollment_id,
                ce.student_id,
                s.name as student_name,
                s.phone as student_phone,
                ce.class_group_id,
                cg.name as class_name,
                COALESCE(cp.total_lessons, 0) as total_sessions,
                COALESCE(SUM(cc.consumed_count), 0) as consumed_sessions,
                (COALESCE(cp.total_lessons, 0) - COALESCE(SUM(cc.consumed_count), 0)) as remaining_sessions
            FROM class_enrollment ce
            INNER JOIN student s ON s.id = ce.student_id AND s.deleted = 0
            INNER JOIN class_group cg ON cg.id = ce.class_group_id AND cg.deleted = 0
            LEFT JOIN course_product cp ON cp.id = cg.course_product_id AND cp.deleted = 0
            LEFT JOIN course_consumption cc ON cc.enrollment_id = ce.id AND cc.deleted = 0
            WHERE ce.tenant_id = ? AND ce.deleted = 0 AND ce.enroll_status = 'ACTIVE'
            GROUP BY ce.id, ce.student_id, s.name, s.phone, ce.class_group_id, cg.name, cp.total_lessons
            HAVING remaining_sessions <= ?
            ORDER BY remaining_sessions ASC
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("enrollmentId", rs.getLong("enrollment_id"));
                    map.put("studentId", rs.getLong("student_id"));
                    map.put("studentName", rs.getString("student_name"));
                    map.put("studentPhone", rs.getString("student_phone"));
                    map.put("classGroupId", rs.getLong("class_group_id"));
                    map.put("className", rs.getString("class_name"));
                    map.put("totalSessions", rs.getInt("total_sessions"));
                    map.put("consumedSessions", rs.getInt("consumed_sessions"));
                    map.put("remainingSessions", rs.getInt("remaining_sessions"));
                    return map;
                },
                tenantId, daysThreshold);

        return ApiResult.ok(results);
    }

    @PostMapping("/batch-renewal-reminder")
    @RequirePermission("system:notification:send")
    public ApiResult<Integer> sendBatchRenewalReminder(@RequestParam(defaultValue = "3") int daysThreshold) {
        Long tenantId = SecurityContext.tenantId();

        List<Map<String, Object>> students = getRenewalAlert(daysThreshold).data();

        int count = 0;
        for (Map<String, Object> student : students) {
            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setNotificationType("RENEWAL_REMINDER");
            notification.setTitle("课时即将用完提醒");
            notification.setContent(String.format("尊敬的家长，您孩子在%s的课程剩余%d节课，请及时续费",
                    student.get("className"), student.get("remainingSessions")));
            notification.setTargetType("STUDENT");
            notification.setTargetId((Long) student.get("studentId"));
            notification.setStatus("PENDING");
            notification.setChannel("SMS,WECHAT");
            notification.setCreatedBy(SecurityContext.accountId());
            notificationMapper.insert(notification);
            count++;
        }

        return ApiResult.ok(count);
    }

    public record SendNotificationRequest(
            @NotBlank String notificationType,
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String targetType,
            @NotNull Long targetId,
            @NotBlank String channel
    ) {}
}
