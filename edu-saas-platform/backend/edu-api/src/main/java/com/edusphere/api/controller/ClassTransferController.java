package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.ClassEnrollment;
import com.edusphere.course.domain.ClassTransferRequest;
import com.edusphere.course.mapper.ClassEnrollmentMapper;
import com.edusphere.course.mapper.ClassTransferRequestMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import com.edusphere.system.domain.Notification;
import com.edusphere.system.mapper.NotificationMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/class-transfer")
public class ClassTransferController {

    private final ClassTransferRequestMapper transferRequestMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;
    private final NotificationMapper notificationMapper;

    public ClassTransferController(ClassTransferRequestMapper transferRequestMapper,
                                   ClassEnrollmentMapper classEnrollmentMapper,
                                   NotificationMapper notificationMapper) {
        this.transferRequestMapper = transferRequestMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    @RequirePermission("course:transfer:view")
    public ApiResult<PageResult<ClassTransferRequest>> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<ClassTransferRequest> wrapper = new LambdaQueryWrapper<ClassTransferRequest>()
                .eq(ClassTransferRequest::getTenantId, tenantId)
                .eq(ClassTransferRequest::getDeleted, false);

        if (status != null && !status.isBlank()) {
            wrapper.eq(ClassTransferRequest::getStatus, status);
        }

        wrapper.orderByDesc(ClassTransferRequest::getCreatedAt);
        Page<ClassTransferRequest> result = transferRequestMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping("/request")
    @RequirePermission("course:transfer:create")
    public ApiResult<Long> createTransferRequest(@RequestBody @Valid CreateTransferRequest request) {
        Long tenantId = SecurityContext.tenantId();

        ClassTransferRequest transfer = new ClassTransferRequest();
        transfer.setTenantId(tenantId);
        transfer.setStudentId(request.studentId());
        transfer.setFromClassGroupId(request.fromClassGroupId());
        transfer.setToClassGroupId(request.toClassGroupId());
        transfer.setFromLessonSessionId(request.fromLessonSessionId());
        transfer.setToLessonSessionId(request.toLessonSessionId());
        transfer.setTransferType(request.transferType());
        transfer.setReason(request.reason());
        transfer.setStatus("PENDING");
        transfer.setCreatedBy(SecurityContext.accountId());
        transferRequestMapper.insert(transfer);

        return ApiResult.ok(transfer.getId());
    }

    @PutMapping("/{id}/approve")
    @Transactional
    @RequirePermission("course:transfer:approve")
    public ApiResult<Void> approveTransfer(@PathVariable Long id, @RequestBody @Valid ApprovalRequest request) {
        Long tenantId = SecurityContext.tenantId();
        ClassTransferRequest transfer = transferRequestMapper.selectOne(new LambdaQueryWrapper<ClassTransferRequest>()
                .eq(ClassTransferRequest::getTenantId, tenantId)
                .eq(ClassTransferRequest::getId, id)
                .eq(ClassTransferRequest::getDeleted, false));

        if (transfer == null) {
            throw new BizException(404, "转班申请不存在");
        }
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new BizException(400, "申请已处理");
        }

        transfer.setStatus(request.approved() ? "APPROVED" : "REJECTED");
        transfer.setApprovedBy(SecurityContext.accountId());
        transfer.setApprovedAt(LocalDateTime.now());
        transfer.setApprovalRemark(request.remark());
        transferRequestMapper.updateById(transfer);

        if (request.approved()) {
            if ("CLASS_TRANSFER".equals(transfer.getTransferType())) {
                classEnrollmentMapper.update(null, new LambdaUpdateWrapper<ClassEnrollment>()
                        .eq(ClassEnrollment::getTenantId, tenantId)
                        .eq(ClassEnrollment::getStudentId, transfer.getStudentId())
                        .eq(ClassEnrollment::getClassGroupId, transfer.getFromClassGroupId())
                        .eq(ClassEnrollment::getDeleted, false)
                        .set(ClassEnrollment::getEnrollStatus, "TRANSFERRED"));

                ClassEnrollment newEnrollment = new ClassEnrollment();
                newEnrollment.setTenantId(tenantId);
                newEnrollment.setStudentId(transfer.getStudentId());
                newEnrollment.setClassGroupId(transfer.getToClassGroupId());
                newEnrollment.setEnrollStatus("ACTIVE");
                newEnrollment.setCreatedBy(SecurityContext.accountId());
                classEnrollmentMapper.insert(newEnrollment);
            }

            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setNotificationType("CLASS_TRANSFER_APPROVED");
            notification.setTitle("转班申请已通过");
            notification.setContent("您的转班申请已通过审批");
            notification.setTargetType("STUDENT");
            notification.setTargetId(transfer.getStudentId());
            notification.setStatus("PENDING");
            notification.setChannel("SMS,WECHAT");
            notification.setCreatedBy(SecurityContext.accountId());
            notificationMapper.insert(notification);
        }

        return ApiResult.ok();
    }

    public record CreateTransferRequest(
            @NotNull Long studentId,
            Long fromClassGroupId,
            Long toClassGroupId,
            Long fromLessonSessionId,
            Long toLessonSessionId,
            @NotBlank String transferType,
            String reason
    ) {}

    public record ApprovalRequest(
            @NotNull Boolean approved,
            String remark
    ) {}
}
