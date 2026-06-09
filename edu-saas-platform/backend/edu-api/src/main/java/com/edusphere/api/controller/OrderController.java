package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.common.exception.BizException;
import com.edusphere.common.trace.OperationLogEvent;
import com.edusphere.common.trace.RequestTraceContext;
import com.edusphere.course.domain.Campus;
import com.edusphere.course.domain.ClassGroup;
import com.edusphere.course.domain.ClassEnrollment;
import com.edusphere.course.domain.CourseProduct;
import com.edusphere.course.domain.Student;
import com.edusphere.course.mapper.CampusMapper;
import com.edusphere.course.mapper.ClassEnrollmentMapper;
import com.edusphere.course.mapper.ClassGroupMapper;
import com.edusphere.course.mapper.CourseProductMapper;
import com.edusphere.course.mapper.StudentMapper;
import com.edusphere.order.domain.EnrollmentOrder;
import com.edusphere.order.domain.EnrollmentOrderItem;
import com.edusphere.order.domain.PaymentOrder;
import com.edusphere.order.domain.PaymentRecord;
import com.edusphere.order.domain.RefundOrder;
import com.edusphere.order.domain.RefundRecord;
import com.edusphere.order.mapper.EnrollmentOrderItemMapper;
import com.edusphere.order.mapper.EnrollmentOrderMapper;
import com.edusphere.order.mapper.PaymentOrderMapper;
import com.edusphere.order.mapper.PaymentRecordMapper;
import com.edusphere.order.mapper.RefundOrderMapper;
import com.edusphere.order.mapper.RefundRecordMapper;
import com.edusphere.api.service.payment.PaymentCallbackVerifier;
import com.edusphere.api.service.payment.PaymentChannel;
import com.edusphere.api.service.payment.PaymentChannelRegistry;
import com.edusphere.api.service.payment.PaymentStateMachine;
import com.edusphere.security.context.DataScopeSupport;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import com.edusphere.system.domain.Account;
import com.edusphere.system.mapper.AccountMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PAYMENT_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter REFUND_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EnrollmentOrderMapper enrollmentOrderMapper;
    private final EnrollmentOrderItemMapper enrollmentOrderItemMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final AccountMapper accountMapper;
    private final ClassGroupMapper classGroupMapper;
    private final CourseProductMapper courseProductMapper;
    private final StudentMapper studentMapper;
    private final CampusMapper campusMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisSupportService redisSupportService;
    private final JdbcTemplate jdbcTemplate;
    private final PaymentStateMachine paymentStateMachine;
    private final PaymentCallbackVerifier paymentCallbackVerifier;
    private final PaymentChannelRegistry paymentChannelRegistry;
    private final ObjectMapper objectMapper;

    public OrderController(
            EnrollmentOrderMapper enrollmentOrderMapper,
            EnrollmentOrderItemMapper enrollmentOrderItemMapper,
            PaymentOrderMapper paymentOrderMapper,
            PaymentRecordMapper paymentRecordMapper,
            RefundOrderMapper refundOrderMapper,
            RefundRecordMapper refundRecordMapper,
            AccountMapper accountMapper,
            ClassGroupMapper classGroupMapper,
            CourseProductMapper courseProductMapper,
            StudentMapper studentMapper,
            CampusMapper campusMapper,
            ClassEnrollmentMapper classEnrollmentMapper,
            ApplicationEventPublisher eventPublisher,
            RedisSupportService redisSupportService,
            JdbcTemplate jdbcTemplate,
            PaymentStateMachine paymentStateMachine,
            PaymentCallbackVerifier paymentCallbackVerifier,
            PaymentChannelRegistry paymentChannelRegistry,
            ObjectMapper objectMapper
    ) {
        this.enrollmentOrderMapper = enrollmentOrderMapper;
        this.enrollmentOrderItemMapper = enrollmentOrderItemMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundOrderMapper = refundOrderMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.accountMapper = accountMapper;
        this.classGroupMapper = classGroupMapper;
        this.courseProductMapper = courseProductMapper;
        this.studentMapper = studentMapper;
        this.campusMapper = campusMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
        this.eventPublisher = eventPublisher;
        this.redisSupportService = redisSupportService;
        this.jdbcTemplate = jdbcTemplate;
        this.paymentStateMachine = paymentStateMachine;
        this.paymentCallbackVerifier = paymentCallbackVerifier;
        this.paymentChannelRegistry = paymentChannelRegistry;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @RequirePermission("order:order:view")
    public ApiResult<PageResult<Map<String, Object>>> orders(@RequestParam(required = false) String orderStatus,
                                                            @RequestParam(required = false) String payStatus,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "20") long pageSize) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<EnrollmentOrder> wrapper = new LambdaQueryWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getDeleted, false)
                .orderByDesc(EnrollmentOrder::getCreatedAt);
        DataScopeSupport.applyCampusScope(wrapper, EnrollmentOrder::getCampusId, EnrollmentOrder::getCreatedBy);
        if (hasText(orderStatus)) {
            wrapper.eq(EnrollmentOrder::getOrderStatus, orderStatus);
        }
        if (hasText(payStatus)) {
            wrapper.eq(EnrollmentOrder::getPayStatus, payStatus);
        }
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(EnrollmentOrder::getOrderNo, keyword)
                    .or()
                    .like(EnrollmentOrder::getStudentName, keyword)
                    .or()
                    .like(EnrollmentOrder::getCustomerName, keyword));
        }
        page = Math.max(1, page);
        pageSize = Math.min(Math.max(1, pageSize), 100);
        Page<EnrollmentOrder> orderPage = enrollmentOrderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<EnrollmentOrder> orders = orderPage.getRecords();
        Map<Long, List<EnrollmentOrderItem>> itemsByOrderId = loadItemsByOrderIds(tenantId, orders.stream().map(EnrollmentOrder::getId).toList());
        Map<Long, ClassGroup> classGroupMap = loadClassGroups(orders.stream().map(EnrollmentOrder::getClassGroupId).toList());
        Map<Long, CourseProduct> productMap = loadProducts(orders.stream().map(EnrollmentOrder::getCourseProductId).toList());
        Map<Long, Campus> campusMap = loadCampuses(orders.stream().map(EnrollmentOrder::getCampusId).filter(Objects::nonNull).toList());
        List<Map<String, Object>> records = orders.stream().map(order -> toOrderSummary(order, itemsByOrderId.get(order.getId()), classGroupMap, productMap, campusMap)).toList();
        Map<String, Object> summary = buildOrderSummary(tenantId, orderStatus, payStatus, keyword, orders);
        return ApiResult.ok(PageResult.of(records, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize(), summary));
    }

    @GetMapping("/{id}")
    @RequirePermission("order:order:view")
    public ApiResult<Map<String, Object>> orderDetail(@PathVariable Long id) {
        EnrollmentOrder order = loadOrderWithTenant(SecurityContext.tenantId(), id);
        Map<Long, ClassGroup> classGroupMap = loadClassGroups(List.of(order.getClassGroupId()));
        Map<Long, CourseProduct> productMap = loadProducts(List.of(order.getCourseProductId()));
        Map<Long, Student> studentMap = loadStudents(List.of(order.getStudentId()));
        Map<Long, Campus> campusMap = loadCampuses(List.of(order.getCampusId()));
        List<EnrollmentOrderItem> items = enrollmentOrderItemMapper.selectList(new LambdaQueryWrapper<EnrollmentOrderItem>()
                .eq(EnrollmentOrderItem::getTenantId, order.getTenantId())
                .eq(EnrollmentOrderItem::getOrderId, order.getId())
                .eq(EnrollmentOrderItem::getDeleted, false)
                .orderByAsc(EnrollmentOrderItem::getSortNo));
        Map<String, Object> payload = toOrderSummary(order, items, classGroupMap, productMap, campusMap);
        Student student = studentMap.get(order.getStudentId());
        payload.put("studentPhone", student == null ? "" : nvl(student.getPhone()));
        payload.put("studentGuardianName", student == null ? "" : nvl(student.getGuardianName()));
        payload.put("studentGuardianPhone", student == null ? "" : nvl(student.getGuardianPhone()));
        payload.put("items", items.stream().map(this::toItemPayload).toList());
        payload.put("paymentOrders", loadPaymentOrders(tenantId(), order.getId()).stream().map(this::toPaymentOrderPayload).toList());
        payload.put("payments", loadPayments(tenantId(), order.getId()).stream().map(this::toPaymentPayload).toList());
        payload.put("refundOrders", loadRefundOrders(tenantId(), order.getId()).stream().map(this::toRefundOrderPayload).toList());
        payload.put("refunds", loadRefunds(tenantId(), order.getId()).stream().map(this::toRefundPayload).toList());
        return ApiResult.ok(payload);
    }

    @GetMapping("/{id}/payments")
    @RequirePermission("order:payment:view")
    public ApiResult<List<Map<String, Object>>> payments(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        loadOrderWithTenant(tenantId, id);
        return ApiResult.ok(loadPayments(tenantId, id).stream().map(this::toPaymentPayload).toList());
    }

    @GetMapping("/{id}/payment-orders")
    @RequirePermission("order:payment:view")
    public ApiResult<List<Map<String, Object>>> paymentOrders(@PathVariable Long id,
                                                              @RequestParam(required = false) String status) {
        Long tenantId = SecurityContext.tenantId();
        loadOrderWithTenant(tenantId, id);
        List<PaymentOrder> orders = loadPaymentOrders(tenantId, id).stream()
                .filter(order -> !hasText(status) || Objects.equals(order.getStatus(), status))
                .toList();
        return ApiResult.ok(orders.stream().map(this::toPaymentOrderPayload).toList());
    }

    @PostMapping("/{id}/payments")
    @RequirePermission("order:payment:create")
    @Transactional
    public ApiResult<Long> createPayment(@PathVariable Long id,
                                         @RequestBody @Valid PaymentCreateRequest request,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long tenantId = SecurityContext.tenantId();
        enforceWriteRateLimit(tenantId, "payment");
        enforceIdempotency(tenantId, "payment", id, idempotencyKey);
        EnrollmentOrder order = loadOrderForUpdateWithTenant(tenantId, id);
        if (Objects.equals(order.getOrderStatus(), "CANCELLED")) {
            throw new BizException(400, "订单已取消，不能收款");
        }
        BigDecimal amount = defaultAmount(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "收款金额必须大于 0");
        }
        if (amount.compareTo(defaultAmount(order.getOutstandingAmount())) > 0) {
            throw new BizException(400, "收款金额不能大于待收金额");
        }

        PaymentChannel channel = paymentChannelRegistry.require("OFFLINE");
        String paymentNo = createPaymentNo(tenantId);
        PaymentOrder paymentOrder = createPaymentOrder(order, paymentNo, channel.channelCode(), request.paymentMethod(), amount, "SUCCESS", channel.successfulPaymentReason(), request.receivedAt(), request.remark());

        PaymentRecord payment = new PaymentRecord();
        payment.setTenantId(tenantId);
        payment.setOrderId(order.getId());
        payment.setPaymentNo(paymentNo);
        payment.setPaymentType("RECEIPT");
        payment.setPaymentMethod(hasText(request.paymentMethod()) ? request.paymentMethod() : "CASH");
        payment.setAmount(amount);
        payment.setChannelTradeNo(request.channelTradeNo());
        payment.setReceivedAt(request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt());
        payment.setRemark(request.remark());
        payment.setCreatedBy(SecurityContext.accountId());
        paymentRecordMapper.insert(payment);

        refreshOrderPaymentStatus(tenantId, order.getId());
        recordOrderAudit(order.getId(), "PAYMENT_CREATED", order.getPayStatus(), null, amount, "paymentNo=" + payment.getPaymentNo() + ", paymentOrderId=" + paymentOrder.getId());
        recordOperation("ORDER", "CREATE_PAYMENT", "PAYMENT_RECORD", payment.getId(), "orderNo=" + order.getOrderNo() + ", amount=" + amount);
        return ApiResult.ok(payment.getId());
    }

    @GetMapping("/{id}/refunds")
    @RequirePermission("order:refund:view")
    public ApiResult<List<Map<String, Object>>> refunds(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        loadOrderWithTenant(tenantId, id);
        return ApiResult.ok(loadRefunds(tenantId, id).stream().map(this::toRefundPayload).toList());
    }

    @GetMapping("/{id}/refund-orders")
    @RequirePermission("order:refund:view")
    public ApiResult<List<Map<String, Object>>> refundOrders(@PathVariable Long id,
                                                             @RequestParam(required = false) String status) {
        Long tenantId = SecurityContext.tenantId();
        loadOrderWithTenant(tenantId, id);
        List<RefundOrder> orders = loadRefundOrders(tenantId, id).stream()
                .filter(order -> !hasText(status) || Objects.equals(order.getStatus(), status))
                .toList();
        return ApiResult.ok(orders.stream().map(this::toRefundOrderPayload).toList());
    }

    @GetMapping("/reconciliation/pending")
    @RequirePermission("order:payment:view")
    public ApiResult<Map<String, Object>> pendingReconciliation(@RequestParam(defaultValue = "50") int limit) {
        Long tenantId = SecurityContext.tenantId();
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> paymentOrders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getTenantId, tenantId)
                        .eq(PaymentOrder::getDeleted, false)
                        .in(PaymentOrder::getStatus, List.of("INIT", "PAYING"))
                        .orderByAsc(PaymentOrder::getNextCheckAt)
                        .last("limit " + normalizedLimit))
                .stream().map(this::toPaymentOrderPayload).toList();
        List<Map<String, Object>> refundOrders = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                        .eq(RefundOrder::getTenantId, tenantId)
                        .eq(RefundOrder::getDeleted, false)
                        .in(RefundOrder::getStatus, List.of("INIT", "REFUNDING"))
                        .orderByAsc(RefundOrder::getNextCheckAt)
                        .last("limit " + normalizedLimit))
                .stream().map(this::toRefundOrderPayload).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentOrders", paymentOrders);
        payload.put("refundOrders", refundOrders);
        payload.put("count", paymentOrders.size() + refundOrders.size());
        return ApiResult.ok(payload);
    }

    @GetMapping("/{id}/audits")
    @RequirePermission("order:order:view")
    public ApiResult<List<Map<String, Object>>> audits(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        loadOrderWithTenant(tenantId, id);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                """
                select event_type, from_status, to_status, amount, detail, operator_username, request_id, created_at
                from order_audit_log
                where tenant_id = ? and order_id = ?
                order by created_at desc
                """,
                tenantId,
                id
        ).stream().map(row -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventType", row.get("event_type"));
            payload.put("fromStatus", row.get("from_status"));
            payload.put("toStatus", row.get("to_status"));
            payload.put("amount", row.get("amount"));
            payload.put("detail", row.get("detail"));
            payload.put("operatorUsername", row.get("operator_username"));
            payload.put("requestId", row.get("request_id"));
            payload.put("createdAt", row.get("created_at"));
            return payload;
        }).toList();
        return ApiResult.ok(records);
    }

    @PostMapping("/{id}/refunds")
    @RequirePermission("order:refund:create")
    @Transactional
    public ApiResult<Long> createRefund(@PathVariable Long id,
                                        @RequestBody @Valid RefundCreateRequest request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long tenantId = SecurityContext.tenantId();
        enforceWriteRateLimit(tenantId, "refund");
        enforceIdempotency(tenantId, "refund", id, idempotencyKey);
        EnrollmentOrder order = loadOrderForUpdateWithTenant(tenantId, id);
        BigDecimal amount = defaultAmount(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "退款金额必须大于 0");
        }
        BigDecimal refundableAmount = defaultAmount(order.getPaidAmount()).subtract(defaultAmount(order.getRefundedAmount()));
        if (amount.compareTo(refundableAmount) > 0) {
            throw new BizException(400, "退款金额不能大于可退金额");
        }

        PaymentChannel channel = paymentChannelRegistry.require("OFFLINE");
        String refundNo = createRefundNo(tenantId);
        RefundOrder refundOrder = createRefundOrder(order, refundNo, null, hasText(request.refundMethod()) ? request.refundMethod() : channel.defaultPaymentMethod(), request.channelTradeNo(), amount, "SUCCESS", channel.successfulRefundReason(), request.refundedAt(), request.refundReason());

        RefundRecord refund = new RefundRecord();
        refund.setTenantId(tenantId);
        refund.setOrderId(order.getId());
        refund.setRefundNo(refundNo);
        refund.setRefundType("MANUAL");
        refund.setRefundMethod(hasText(request.refundMethod()) ? request.refundMethod() : "CASH");
        refund.setAmount(amount);
        refund.setChannelTradeNo(request.channelTradeNo());
        refund.setRefundReason(request.refundReason());
        refund.setRefundedAt(request.refundedAt() == null ? LocalDateTime.now() : request.refundedAt());
        refund.setCreatedBy(SecurityContext.accountId());
        refundRecordMapper.insert(refund);

        refreshOrderPaymentStatus(tenantId, order.getId());
        recordOrderAudit(order.getId(), "REFUND_CREATED", order.getPayStatus(), null, amount, "refundNo=" + refund.getRefundNo() + ", refundOrderId=" + refundOrder.getId());
        recordOperation("ORDER", "CREATE_REFUND", "REFUND_RECORD", refund.getId(), "orderNo=" + order.getOrderNo() + ", amount=" + amount);
        return ApiResult.ok(refund.getId());
    }

    @PostMapping("/enrollment")
    @RequirePermission("order:order:create")
    @Transactional
    public ApiResult<Long> createEnrollmentOrder(@RequestBody @Valid EnrollmentOrderCreateRequest request,
                                                 @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long tenantId = SecurityContext.tenantId();
        enforceWriteRateLimit(tenantId, "enrollment-order");
        enforceIdempotency(tenantId, "enrollment-order", null, idempotencyKey);
        ClassGroup classGroup = loadClassGroupWithTenant(tenantId, request.classGroupId());
        CourseProduct courseProduct = loadProductWithTenant(tenantId, classGroup.getCourseProductId());
        Student student = loadStudentWithTenant(tenantId, request.studentId());

        BigDecimal totalAmount = normalizeAmount(courseProduct.getListPrice());
        BigDecimal discountAmount = normalizeAmount(request.discountAmount());
        if (discountAmount.compareTo(totalAmount) > 0) {
            throw new BizException(400, "优惠金额不能大于应收金额");
        }
        BigDecimal payableAmount = totalAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        String orderNo = createOrderNo(tenantId);

        EnrollmentOrder order = new EnrollmentOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(orderNo);
        order.setOrderType("ENROLLMENT");
        order.setOrderStatus("CREATED");
        order.setPayStatus("UNPAID");
        order.setSourceChannel(hasText(request.sourceChannel()) ? request.sourceChannel() : "MANUAL");
        order.setCustomerName(hasText(request.customerName()) ? request.customerName() : firstNonBlank(student.getGuardianName(), student.getName()));
        order.setCustomerPhone(hasText(request.customerPhone()) ? request.customerPhone() : firstNonBlank(student.getGuardianPhone(), student.getPhone()));
        order.setStudentId(student.getId());
        order.setStudentName(student.getName());
        order.setClassGroupId(classGroup.getId());
        order.setCourseProductId(courseProduct.getId());
        order.setCampusId(classGroup.getCampusId());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundedAmount(BigDecimal.ZERO);
        order.setOutstandingAmount(payableAmount);
        order.setRemark(request.remark());
        order.setCreatedBy(SecurityContext.accountId());
        enrollmentOrderMapper.insert(order);

        EnrollmentOrderItem item = new EnrollmentOrderItem();
        item.setTenantId(tenantId);
        item.setOrderId(order.getId());
        item.setItemType("ENROLLMENT");
        item.setItemName(courseProduct.getName() + " - " + classGroup.getName());
        item.setCourseProductId(courseProduct.getId());
        item.setClassGroupId(classGroup.getId());
        item.setStudentId(student.getId());
        item.setQuantity(1);
        item.setUnitPrice(totalAmount);
        item.setDiscountAmount(discountAmount);
        item.setAmount(payableAmount);
        item.setSnapshotJson(buildSnapshotJson(courseProduct, classGroup, student));
        item.setSortNo(1);
        item.setCreatedBy(SecurityContext.accountId());
        enrollmentOrderItemMapper.insert(item);
        recordOrderAudit(order.getId(), "ORDER_CREATED", null, order.getOrderStatus(), payableAmount, "orderNo=" + orderNo);
        recordOperation("ORDER", "CREATE_ORDER", "ENROLLMENT_ORDER", order.getId(), "orderNo=" + orderNo + ", student=" + student.getName() + ", amount=" + payableAmount);
        return ApiResult.ok(order.getId());
    }

    @PostMapping("/callbacks/payment")
    @Transactional
    public ApiResult<Void> handlePaymentCallback(@RequestHeader("X-Callback-Secret") String secret,
                                                  @RequestBody @Valid PaymentCallbackRequest request) {
        paymentCallbackVerifier.verifySharedSecret(secret);
        Long tenantId = request.tenantId();

        // 优先使用数据库唯一约束来保证幂等性，Redis作为性能优化
        if (paymentCallbackExists(tenantId, request.callbackNo(), request.channelCode(), request.channelTradeNo())) {
            return ApiResult.ok();
        }

        // Redis缓存检查（可选的性能优化层）
        String callbackKey = "paycb:" + tenantId + ":" + request.callbackNo();
        if (!redisSupportService.reserveOnce(callbackKey, Duration.ofDays(2))) {
            return ApiResult.ok();
        }

        EnrollmentOrder order = loadOrderForUpdateSystem(tenantId, request.orderId());
        BigDecimal amount = defaultAmount(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "回调金额必须大于 0");
        }
        if (request.callbackStatus() == null || request.callbackStatus().isBlank()) {
            throw new BizException(400, "回调状态不能为空");
        }
        if (!"SUCCESS".equalsIgnoreCase(request.callbackStatus())) {
            upsertCallbackPaymentOrder(order, request, "FAILED", "回调状态：" + request.callbackStatus());
            recordPaymentCallback(request, null, "IGNORED_STATUS_" + request.callbackStatus());
            recordOrderAudit(tenantId, order.getId(), "PAYMENT_CALLBACK_IGNORED", order.getPayStatus(), null, amount, "callbackNo=" + request.callbackNo());
            return ApiResult.ok();
        }

        PaymentOrder paymentOrder = upsertCallbackPaymentOrder(order, request, "SUCCESS", "支付回调成功");
        PaymentRecord payment = findPaymentRecord(tenantId, request.paymentNo(), request.channelTradeNo());
        if (payment == null) {
            payment = new PaymentRecord();
            payment.setTenantId(tenantId);
            payment.setOrderId(order.getId());
            payment.setPaymentNo(request.paymentNo());
            payment.setPaymentType("CHANNEL");
            payment.setPaymentMethod(hasText(request.paymentMethod()) ? request.paymentMethod() : request.channelCode());
            payment.setAmount(amount);
            payment.setChannelTradeNo(request.channelTradeNo());
            payment.setReceivedAt(request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt());
            payment.setRemark(request.remark());
            payment.setCreatedBy(SecurityContext.current().map(p -> p.accountId()).orElse(null));
            paymentRecordMapper.insert(payment);
        }

        refreshOrderPaymentStatus(tenantId, order.getId());
        recordPaymentCallback(request, payment.getId(), "SUCCESS");
        recordOrderAudit(tenantId, order.getId(), "PAYMENT_CALLBACK", order.getPayStatus(), null, amount, "callbackNo=" + request.callbackNo() + ", paymentOrderId=" + paymentOrder.getId());
        return ApiResult.ok();
    }

    @PutMapping("/{id}/confirm")
    @RequirePermission("order:order:confirm")
    @Transactional
    public ApiResult<Void> confirmOrder(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        enforceWriteRateLimit(tenantId, "confirm-order");
        EnrollmentOrder order = loadOrderForUpdateWithTenant(tenantId, id);
        if (Objects.equals(order.getOrderStatus(), "CANCELLED")) {
            throw new BizException(400, "订单已取消，不能确认");
        }
        if (!Objects.equals(order.getOrderStatus(), "CONFIRMED")) {
            ensureClassCapacity(tenantId, order.getClassGroupId(), order.getStudentId());
            upsertEnrollment(tenantId, order);
            enrollmentOrderMapper.update(null, new LambdaUpdateWrapper<EnrollmentOrder>()
                    .eq(EnrollmentOrder::getTenantId, tenantId)
                    .eq(EnrollmentOrder::getId, id)
                    .eq(EnrollmentOrder::getDeleted, false)
                    .set(EnrollmentOrder::getOrderStatus, "CONFIRMED")
                    .set(EnrollmentOrder::getConfirmedAt, LocalDateTime.now())
                    .set(EnrollmentOrder::getUpdatedBy, SecurityContext.accountId()));
            recordOrderAudit(id, "ORDER_CONFIRMED", order.getOrderStatus(), "CONFIRMED", null, "orderNo=" + order.getOrderNo());
            recordOperation("ORDER", "CONFIRM_ORDER", "ENROLLMENT_ORDER", id, "orderNo=" + order.getOrderNo());
        }
        return ApiResult.ok();
    }

    @PutMapping("/{id}/cancel")
    @RequirePermission("order:order:cancel")
    @Transactional
    public ApiResult<Void> cancelOrder(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        enforceWriteRateLimit(tenantId, "cancel-order");
        EnrollmentOrder order = loadOrderForUpdateWithTenant(tenantId, id);
        if (Objects.equals(order.getOrderStatus(), "CANCELLED")) {
            return ApiResult.ok();
        }
        BigDecimal netPaidAmount = defaultAmount(order.getPaidAmount()).subtract(defaultAmount(order.getRefundedAmount()));
        if (netPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException(409, "订单存在未退净收款，请先完成退款再取消");
        }
        if (Objects.equals(order.getOrderStatus(), "CONFIRMED")) {
            classEnrollmentMapper.update(null, new LambdaUpdateWrapper<ClassEnrollment>()
                    .eq(ClassEnrollment::getTenantId, tenantId)
                    .eq(ClassEnrollment::getClassGroupId, order.getClassGroupId())
                    .eq(ClassEnrollment::getStudentId, order.getStudentId())
                    .eq(ClassEnrollment::getDeleted, false)
                    .set(ClassEnrollment::getEnrollStatus, "CANCELLED")
                    .set(ClassEnrollment::getRemark, appendCancelRemark(order.getRemark())));
        }
        enrollmentOrderMapper.update(null, new LambdaUpdateWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getId, id)
                .eq(EnrollmentOrder::getDeleted, false)
                .set(EnrollmentOrder::getOrderStatus, "CANCELLED")
                .set(EnrollmentOrder::getCancelledAt, LocalDateTime.now())
                .set(EnrollmentOrder::getUpdatedBy, SecurityContext.accountId()));
        recordOrderAudit(id, "ORDER_CANCELLED", order.getOrderStatus(), "CANCELLED", null, "orderNo=" + order.getOrderNo());
        recordOperation("ORDER", "CANCEL_ORDER", "ENROLLMENT_ORDER", id, "orderNo=" + order.getOrderNo());
        return ApiResult.ok();
    }

    private Map<String, Object> toOrderSummary(EnrollmentOrder order,
                                              List<EnrollmentOrderItem> items,
                                              Map<Long, ClassGroup> classGroupMap,
                                              Map<Long, CourseProduct> productMap,
                                              Map<Long, Campus> campusMap) {
        ClassGroup classGroup = classGroupMap.get(order.getClassGroupId());
        CourseProduct product = productMap.get(order.getCourseProductId());
        Campus campus = order.getCampusId() == null ? null : campusMap.get(order.getCampusId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("orderType", order.getOrderType());
        payload.put("orderStatus", order.getOrderStatus());
        payload.put("payStatus", order.getPayStatus());
        payload.put("sourceChannel", nvl(order.getSourceChannel()));
        payload.put("customerName", nvl(order.getCustomerName()));
        payload.put("customerPhone", nvl(order.getCustomerPhone()));
        payload.put("studentId", order.getStudentId());
        payload.put("studentName", nvl(order.getStudentName()));
        payload.put("classGroupId", order.getClassGroupId());
        payload.put("classGroupName", classGroup == null ? "" : classGroup.getName());
        payload.put("courseProductId", order.getCourseProductId());
        payload.put("courseProductName", product == null ? "" : product.getName());
        payload.put("campusId", order.getCampusId() == null ? 0L : order.getCampusId());
        payload.put("campusName", campus == null ? "" : campus.getName());
        payload.put("totalAmount", money(order.getTotalAmount()));
        payload.put("discountAmount", money(order.getDiscountAmount()));
        payload.put("payableAmount", money(order.getPayableAmount()));
        payload.put("paidAmount", money(order.getPaidAmount()));
        payload.put("refundedAmount", money(order.getRefundedAmount()));
        payload.put("netPaidAmount", money(defaultAmount(order.getPaidAmount()).subtract(defaultAmount(order.getRefundedAmount()))));
        payload.put("outstandingAmount", money(order.getOutstandingAmount()));
        payload.put("remark", nvl(order.getRemark()));
        payload.put("confirmedAt", order.getConfirmedAt() == null ? "" : order.getConfirmedAt().toString());
        payload.put("cancelledAt", order.getCancelledAt() == null ? "" : order.getCancelledAt().toString());
        payload.put("itemCount", items == null ? 0 : items.size());
        payload.put("createdAt", order.getCreatedAt() == null ? "" : order.getCreatedAt().toString());
        return payload;
    }

    private Map<String, Object> toItemPayload(EnrollmentOrderItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", item.getId());
        payload.put("itemType", item.getItemType());
        payload.put("itemName", item.getItemName());
        payload.put("courseProductId", item.getCourseProductId() == null ? 0L : item.getCourseProductId());
        payload.put("classGroupId", item.getClassGroupId() == null ? 0L : item.getClassGroupId());
        payload.put("studentId", item.getStudentId() == null ? 0L : item.getStudentId());
        payload.put("quantity", item.getQuantity() == null ? 0 : item.getQuantity());
        payload.put("unitPrice", money(item.getUnitPrice()));
        payload.put("discountAmount", money(item.getDiscountAmount()));
        payload.put("amount", money(item.getAmount()));
        payload.put("snapshotJson", nvl(item.getSnapshotJson()));
        return payload;
    }

    private Map<String, Object> toPaymentPayload(PaymentRecord payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", payment.getId());
        payload.put("paymentNo", payment.getPaymentNo());
        payload.put("paymentType", payment.getPaymentType());
        payload.put("paymentMethod", payment.getPaymentMethod());
        payload.put("amount", money(payment.getAmount()));
        payload.put("channelTradeNo", nvl(payment.getChannelTradeNo()));
        payload.put("receivedAt", payment.getReceivedAt() == null ? "" : payment.getReceivedAt().toString());
        payload.put("remark", nvl(payment.getRemark()));
        return payload;
    }

    private Map<String, Object> toPaymentOrderPayload(PaymentOrder paymentOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", paymentOrder.getId());
        payload.put("paymentNo", paymentOrder.getPaymentNo());
        payload.put("channelCode", paymentOrder.getChannelCode());
        payload.put("channelTradeNo", nvl(paymentOrder.getChannelTradeNo()));
        payload.put("paymentMethod", paymentOrder.getPaymentMethod());
        payload.put("amount", money(paymentOrder.getAmount()));
        payload.put("status", paymentOrder.getStatus());
        payload.put("statusReason", nvl(paymentOrder.getStatusReason()));
        payload.put("requestedAt", paymentOrder.getRequestedAt() == null ? "" : paymentOrder.getRequestedAt().toString());
        payload.put("paidAt", paymentOrder.getPaidAt() == null ? "" : paymentOrder.getPaidAt().toString());
        payload.put("closedAt", paymentOrder.getClosedAt() == null ? "" : paymentOrder.getClosedAt().toString());
        payload.put("checkCount", paymentOrder.getCheckCount() == null ? 0 : paymentOrder.getCheckCount());
        payload.put("remark", nvl(paymentOrder.getRemark()));
        return payload;
    }

    private Map<String, Object> toRefundPayload(RefundRecord refund) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", refund.getId());
        payload.put("refundNo", refund.getRefundNo());
        payload.put("refundType", refund.getRefundType());
        payload.put("refundMethod", refund.getRefundMethod());
        payload.put("amount", money(refund.getAmount()));
        payload.put("channelTradeNo", nvl(refund.getChannelTradeNo()));
        payload.put("refundReason", nvl(refund.getRefundReason()));
        payload.put("refundedAt", refund.getRefundedAt() == null ? "" : refund.getRefundedAt().toString());
        return payload;
    }

    private Map<String, Object> toRefundOrderPayload(RefundOrder refundOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", refundOrder.getId());
        payload.put("refundNo", refundOrder.getRefundNo());
        payload.put("paymentOrderId", refundOrder.getPaymentOrderId() == null ? 0L : refundOrder.getPaymentOrderId());
        payload.put("channelCode", refundOrder.getChannelCode());
        payload.put("channelRefundNo", nvl(refundOrder.getChannelRefundNo()));
        payload.put("refundMethod", refundOrder.getRefundMethod());
        payload.put("amount", money(refundOrder.getAmount()));
        payload.put("status", refundOrder.getStatus());
        payload.put("statusReason", nvl(refundOrder.getStatusReason()));
        payload.put("requestedAt", refundOrder.getRequestedAt() == null ? "" : refundOrder.getRequestedAt().toString());
        payload.put("refundedAt", refundOrder.getRefundedAt() == null ? "" : refundOrder.getRefundedAt().toString());
        payload.put("closedAt", refundOrder.getClosedAt() == null ? "" : refundOrder.getClosedAt().toString());
        payload.put("checkCount", refundOrder.getCheckCount() == null ? 0 : refundOrder.getCheckCount());
        payload.put("remark", nvl(refundOrder.getRemark()));
        return payload;
    }

    private Map<String, Object> buildOrderSummary(Long tenantId, String orderStatus, String payStatus, String keyword, List<EnrollmentOrder> orders) {
        long orderCount = enrollmentOrderMapper.selectCount(orderSummaryWrapper(tenantId, orderStatus, payStatus, keyword, null, null));
        long confirmedCount = enrollmentOrderMapper.selectCount(orderSummaryWrapper(tenantId, orderStatus, payStatus, keyword, "CONFIRMED", null));
        long unpaidCount = enrollmentOrderMapper.selectCount(orderSummaryWrapper(tenantId, orderStatus, payStatus, keyword, null, "UNPAID"));
        BigDecimal pagePayable = orders.stream()
                .map(EnrollmentOrder::getPayableAmount)
                .map(this::defaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("orderCount", orderCount);
        summary.put("confirmedCount", confirmedCount);
        summary.put("unpaidCount", unpaidCount);
        summary.put("totalPayable", pagePayable.toPlainString());
        return summary;
    }

    private LambdaQueryWrapper<EnrollmentOrder> orderSummaryWrapper(
            Long tenantId,
            String orderStatus,
            String payStatus,
            String keyword,
            String summaryOrderStatus,
            String summaryPayStatus
    ) {
        LambdaQueryWrapper<EnrollmentOrder> wrapper = new LambdaQueryWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getDeleted, false);
        DataScopeSupport.applyCampusScope(wrapper, EnrollmentOrder::getCampusId, EnrollmentOrder::getCreatedBy);
        if (hasText(orderStatus)) {
            wrapper.eq(EnrollmentOrder::getOrderStatus, orderStatus);
        }
        if (hasText(payStatus)) {
            wrapper.eq(EnrollmentOrder::getPayStatus, payStatus);
        }
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(EnrollmentOrder::getOrderNo, keyword)
                    .or()
                    .like(EnrollmentOrder::getStudentName, keyword)
                    .or()
                    .like(EnrollmentOrder::getCustomerName, keyword));
        }
        if (hasText(summaryOrderStatus)) {
            wrapper.eq(EnrollmentOrder::getOrderStatus, summaryOrderStatus);
        }
        if (hasText(summaryPayStatus)) {
            wrapper.eq(EnrollmentOrder::getPayStatus, summaryPayStatus);
        }
        return wrapper;
    }

    private EnrollmentOrder loadOrderWithTenant(Long tenantId, Long id) {
        EnrollmentOrder order = enrollmentOrderMapper.selectOne(new LambdaQueryWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getId, id)
                .eq(EnrollmentOrder::getDeleted, false));
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        DataScopeSupport.requireCampusAccess(order.getCampusId(), order.getCreatedBy());
        return order;
    }

    private EnrollmentOrder loadOrderForUpdateWithTenant(Long tenantId, Long id) {
        EnrollmentOrder order = enrollmentOrderMapper.selectOne(new LambdaQueryWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getId, id)
                .eq(EnrollmentOrder::getDeleted, false)
                .last("for update"));
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        DataScopeSupport.requireCampusAccess(order.getCampusId(), order.getCreatedBy());
        return order;
    }

    private EnrollmentOrder loadOrderForUpdateSystem(Long tenantId, Long id) {
        EnrollmentOrder order = enrollmentOrderMapper.selectOne(new LambdaQueryWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getId, id)
                .eq(EnrollmentOrder::getDeleted, false)
                .last("for update"));
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    private void upsertEnrollment(Long tenantId, EnrollmentOrder order) {
        ClassEnrollment existing = classEnrollmentMapper.selectOne(new LambdaQueryWrapper<ClassEnrollment>()
                .eq(ClassEnrollment::getTenantId, tenantId)
                .eq(ClassEnrollment::getClassGroupId, order.getClassGroupId())
                .eq(ClassEnrollment::getStudentId, order.getStudentId())
                .eq(ClassEnrollment::getDeleted, false));
        if (existing == null) {
            ClassEnrollment enrollment = new ClassEnrollment();
            enrollment.setTenantId(tenantId);
            enrollment.setClassGroupId(order.getClassGroupId());
            enrollment.setStudentId(order.getStudentId());
            enrollment.setEnrollStatus("ACTIVE");
            enrollment.setEnrollDate(LocalDate.now());
            enrollment.setRemark("订单确认：" + order.getOrderNo());
            enrollment.setCreatedBy(SecurityContext.accountId());
            classEnrollmentMapper.insert(enrollment);
        } else {
            classEnrollmentMapper.update(null, new LambdaUpdateWrapper<ClassEnrollment>()
                    .eq(ClassEnrollment::getTenantId, tenantId)
                    .eq(ClassEnrollment::getId, existing.getId())
                    .eq(ClassEnrollment::getDeleted, false)
                    .set(ClassEnrollment::getEnrollStatus, "ACTIVE")
                    .set(ClassEnrollment::getEnrollDate, LocalDate.now())
                    .set(ClassEnrollment::getRemark, "订单确认：" + order.getOrderNo())
                    .set(ClassEnrollment::getUpdatedBy, SecurityContext.accountId()));
        }
    }

    private void ensureClassCapacity(Long tenantId, Long classGroupId, Long studentId) {
        // 使用悲观锁锁定班级记录，确保容量检查和插入操作的原子性
        ClassGroup classGroup = loadClassGroupForUpdateWithTenant(tenantId, classGroupId);

        // 检查学生是否已在该班级中
        ClassEnrollment existing = classEnrollmentMapper.selectOne(new LambdaQueryWrapper<ClassEnrollment>()
                .eq(ClassEnrollment::getTenantId, tenantId)
                .eq(ClassEnrollment::getClassGroupId, classGroupId)
                .eq(ClassEnrollment::getStudentId, studentId)
                .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                .eq(ClassEnrollment::getDeleted, false));
        if (existing != null) {
            return;
        }

        // 在持有班级行锁的情况下检查容量
        if (classGroup.getCapacity() == null || classGroup.getCapacity() <= 0) {
            return;
        }

        Long activeCount = classEnrollmentMapper.selectCount(new LambdaQueryWrapper<ClassEnrollment>()
                .eq(ClassEnrollment::getTenantId, tenantId)
                .eq(ClassEnrollment::getClassGroupId, classGroupId)
                .eq(ClassEnrollment::getEnrollStatus, "ACTIVE")
                .eq(ClassEnrollment::getDeleted, false));
        if (activeCount != null && activeCount >= classGroup.getCapacity()) {
            throw new BizException(409, "班级容量已满");
        }
    }

    private void refreshOrderPaymentStatus(Long tenantId, Long orderId) {
        EnrollmentOrder order = loadOrderForUpdateSystem(tenantId, orderId);
        BigDecimal paidAmount = loadPayments(tenantId, orderId).stream()
                .map(PaymentRecord::getAmount)
                .map(this::defaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundedAmount = loadRefunds(tenantId, orderId).stream()
                .map(RefundRecord::getAmount)
                .map(this::defaultAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundedAmount.compareTo(paidAmount) > 0) {
            throw new BizException(400, "累计退款不能大于累计收款");
        }
        BigDecimal payableAmount = defaultAmount(order.getPayableAmount());
        if (paidAmount.compareTo(payableAmount) > 0) {
            throw new BizException(400, "累计收款不能大于应收金额");
        }
        BigDecimal netPaidAmount = paidAmount.subtract(refundedAmount);
        BigDecimal outstandingAmount = payableAmount.subtract(netPaidAmount);
        String payStatus = netPaidAmount.compareTo(BigDecimal.ZERO) == 0
                ? "UNPAID"
                : outstandingAmount.compareTo(BigDecimal.ZERO) == 0 ? "PAID" : "PART_PAID";
        if (refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            payStatus = netPaidAmount.compareTo(BigDecimal.ZERO) == 0 ? "REFUNDED" : "PART_REFUNDED";
        }
        enrollmentOrderMapper.update(null, new LambdaUpdateWrapper<EnrollmentOrder>()
                .eq(EnrollmentOrder::getTenantId, tenantId)
                .eq(EnrollmentOrder::getId, orderId)
                .eq(EnrollmentOrder::getDeleted, false)
                .set(EnrollmentOrder::getPaidAmount, paidAmount)
                .set(EnrollmentOrder::getRefundedAmount, refundedAmount)
                .set(EnrollmentOrder::getOutstandingAmount, outstandingAmount)
                .set(EnrollmentOrder::getPayStatus, payStatus));
    }

    private PaymentOrder createPaymentOrder(EnrollmentOrder order,
                                            String paymentNo,
                                            String channelCode,
                                            String paymentMethod,
                                            BigDecimal amount,
                                            String status,
                                            String statusReason,
                                            LocalDateTime paidAt,
                                            String remark) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setTenantId(order.getTenantId());
        paymentOrder.setOrderId(order.getId());
        paymentOrder.setPaymentNo(paymentNo);
        paymentOrder.setChannelCode(hasText(channelCode) ? channelCode : "OFFLINE");
        paymentOrder.setPaymentMethod(hasText(paymentMethod) ? paymentMethod : "CASH");
        paymentOrder.setAmount(amount);
        paymentOrder.setStatus(status);
        paymentOrder.setStatusReason(statusReason);
        paymentOrder.setRequestedAt(LocalDateTime.now());
        paymentOrder.setPaidAt("SUCCESS".equals(status) ? (paidAt == null ? LocalDateTime.now() : paidAt) : null);
        paymentOrder.setClosedAt(paymentStateMachine.isTerminalPaymentStatus(status) ? LocalDateTime.now() : null);
        paymentOrder.setNextCheckAt(paymentStateMachine.isTerminalPaymentStatus(status) ? null : LocalDateTime.now().plusMinutes(5));
        paymentOrder.setCheckCount(0);
        paymentOrder.setRemark(remark);
        paymentOrder.setCreatedBy(SecurityContext.current().map(p -> p.accountId()).orElse(null));
        paymentOrderMapper.insert(paymentOrder);
        return paymentOrder;
    }

    private PaymentOrder upsertCallbackPaymentOrder(EnrollmentOrder order,
                                                    PaymentCallbackRequest request,
                                                    String status,
                                                    String statusReason) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTenantId, request.tenantId())
                .eq(PaymentOrder::getPaymentNo, request.paymentNo())
                .eq(PaymentOrder::getDeleted, false)
                .last("for update"));
        LocalDateTime eventTime = request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt();
        if (paymentOrder == null) {
            paymentOrder = new PaymentOrder();
            paymentOrder.setTenantId(request.tenantId());
            paymentOrder.setOrderId(order.getId());
            paymentOrder.setPaymentNo(request.paymentNo());
            paymentOrder.setChannelCode(request.channelCode());
            paymentOrder.setChannelTradeNo(request.channelTradeNo());
            paymentOrder.setPaymentMethod(hasText(request.paymentMethod()) ? request.paymentMethod() : request.channelCode());
            paymentOrder.setAmount(defaultAmount(request.amount()));
            paymentOrder.setStatus(status);
            paymentOrder.setStatusReason(statusReason);
            paymentOrder.setRequestedAt(eventTime);
            paymentOrder.setPaidAt("SUCCESS".equals(status) ? eventTime : null);
            paymentOrder.setClosedAt(paymentStateMachine.isTerminalPaymentStatus(status) ? LocalDateTime.now() : null);
            paymentOrder.setNextCheckAt(paymentStateMachine.isTerminalPaymentStatus(status) ? null : LocalDateTime.now().plusMinutes(5));
            paymentOrder.setCheckCount(0);
            paymentOrder.setRemark(request.remark());
            paymentOrder.setCreatedBy(SecurityContext.current().map(p -> p.accountId()).orElse(null));
            paymentOrderMapper.insert(paymentOrder);
            return paymentOrder;
        }
        paymentStateMachine.requirePaymentTransition(paymentOrder.getStatus(), status);
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTenantId, request.tenantId())
                .eq(PaymentOrder::getId, paymentOrder.getId())
                .eq(PaymentOrder::getDeleted, false)
                .set(PaymentOrder::getStatus, status)
                .set(PaymentOrder::getStatusReason, statusReason)
                .set(PaymentOrder::getChannelTradeNo, request.channelTradeNo())
                .set(PaymentOrder::getPaidAt, "SUCCESS".equals(status) ? eventTime : paymentOrder.getPaidAt())
                .set(PaymentOrder::getClosedAt, paymentStateMachine.isTerminalPaymentStatus(status) ? LocalDateTime.now() : null)
                .set(PaymentOrder::getNextCheckAt, paymentStateMachine.isTerminalPaymentStatus(status) ? null : LocalDateTime.now().plusMinutes(5))
                .set(PaymentOrder::getUpdatedBy, SecurityContext.current().map(p -> p.accountId()).orElse(null)));
        paymentOrder.setStatus(status);
        return paymentOrder;
    }

    private RefundOrder createRefundOrder(EnrollmentOrder order,
                                          String refundNo,
                                          Long paymentOrderId,
                                          String refundMethod,
                                          String channelRefundNo,
                                          BigDecimal amount,
                                          String status,
                                          String statusReason,
                                          LocalDateTime refundedAt,
                                          String remark) {
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setTenantId(order.getTenantId());
        refundOrder.setOrderId(order.getId());
        refundOrder.setRefundNo(refundNo);
        refundOrder.setPaymentOrderId(paymentOrderId);
        refundOrder.setChannelCode("OFFLINE");
        refundOrder.setChannelRefundNo(channelRefundNo);
        refundOrder.setRefundMethod(hasText(refundMethod) ? refundMethod : "CASH");
        refundOrder.setAmount(amount);
        refundOrder.setStatus(status);
        refundOrder.setStatusReason(statusReason);
        refundOrder.setRequestedAt(LocalDateTime.now());
        refundOrder.setRefundedAt("SUCCESS".equals(status) ? (refundedAt == null ? LocalDateTime.now() : refundedAt) : null);
        refundOrder.setClosedAt(paymentStateMachine.isTerminalRefundStatus(status) ? LocalDateTime.now() : null);
        refundOrder.setNextCheckAt(paymentStateMachine.isTerminalRefundStatus(status) ? null : LocalDateTime.now().plusMinutes(5));
        refundOrder.setCheckCount(0);
        refundOrder.setRemark(remark);
        refundOrder.setCreatedBy(SecurityContext.current().map(p -> p.accountId()).orElse(null));
        refundOrderMapper.insert(refundOrder);
        return refundOrder;
    }

    private PaymentRecord findPaymentRecord(Long tenantId, String paymentNo, String channelTradeNo) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getTenantId, tenantId)
                .eq(PaymentRecord::getDeleted, false)
                .and(q -> q.eq(PaymentRecord::getPaymentNo, paymentNo)
                        .or(hasText(channelTradeNo), x -> x.eq(PaymentRecord::getChannelTradeNo, channelTradeNo)))
                .last("limit 1");
        return paymentRecordMapper.selectOne(wrapper);
    }

    private String createOrderNo(Long tenantId) {
        return "E" + ORDER_NO_FORMAT.format(LocalDateTime.now()) + String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }

    private String createPaymentNo(Long tenantId) {
        return "P" + PAYMENT_NO_FORMAT.format(LocalDateTime.now()) + String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }

    private String createRefundNo(Long tenantId) {
        return "R" + REFUND_NO_FORMAT.format(LocalDateTime.now()) + String.format("%04d", SECURE_RANDOM.nextInt(10000));
    }

    private Map<Long, List<EnrollmentOrderItem>> loadItemsByOrderIds(Long tenantId, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<EnrollmentOrderItem> items = enrollmentOrderItemMapper.selectList(new LambdaQueryWrapper<EnrollmentOrderItem>()
                .eq(EnrollmentOrderItem::getTenantId, tenantId)
                .eq(EnrollmentOrderItem::getDeleted, false)
                .in(EnrollmentOrderItem::getOrderId, orderIds)
                .orderByAsc(EnrollmentOrderItem::getSortNo));
        return items.stream().collect(Collectors.groupingBy(EnrollmentOrderItem::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<PaymentRecord> loadPayments(Long tenantId, Long orderId) {
        return paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getTenantId, tenantId)
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getDeleted, false)
                .orderByDesc(PaymentRecord::getReceivedAt));
    }

    private List<PaymentOrder> loadPaymentOrders(Long tenantId, Long orderId) {
        return paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTenantId, tenantId)
                .eq(PaymentOrder::getOrderId, orderId)
                .eq(PaymentOrder::getDeleted, false)
                .orderByDesc(PaymentOrder::getCreatedAt));
    }

    private List<RefundRecord> loadRefunds(Long tenantId, Long orderId) {
        return refundRecordMapper.selectList(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getTenantId, tenantId)
                .eq(RefundRecord::getOrderId, orderId)
                .eq(RefundRecord::getDeleted, false)
                .orderByDesc(RefundRecord::getRefundedAt));
    }

    private List<RefundOrder> loadRefundOrders(Long tenantId, Long orderId) {
        return refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getTenantId, tenantId)
                .eq(RefundOrder::getOrderId, orderId)
                .eq(RefundOrder::getDeleted, false)
                .orderByDesc(RefundOrder::getCreatedAt));
    }

    private Map<Long, ClassGroup> loadClassGroups(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<ClassGroup> wrapper = new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getDeleted, false)
                .in(ClassGroup::getId, ids);
        DataScopeSupport.applyCampusScope(wrapper, ClassGroup::getCampusId, ClassGroup::getCreatedBy);
        List<ClassGroup> classGroups = classGroupMapper.selectList(wrapper);
        return classGroups.stream().collect(Collectors.toMap(ClassGroup::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, CourseProduct> loadProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        List<CourseProduct> products = courseProductMapper.selectList(new LambdaQueryWrapper<CourseProduct>()
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false)
                .in(CourseProduct::getId, ids));
        return products.stream().collect(Collectors.toMap(CourseProduct::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Student> loadStudents(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getDeleted, false)
                .in(Student::getId, ids);
        DataScopeSupport.applyOwnerScope(wrapper, Student::getCreatedBy);
        List<Student> students = studentMapper.selectList(wrapper);
        return students.stream().collect(Collectors.toMap(Student::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, Campus> loadCampuses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        List<Campus> campuses = campusMapper.selectList(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getTenantId, tenantId)
                .eq(Campus::getDeleted, false)
                .in(Campus::getId, ids));
        return campuses.stream().collect(Collectors.toMap(Campus::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private ClassGroup loadClassGroupWithTenant(Long tenantId, Long id) {
        ClassGroup classGroup = classGroupMapper.selectOne(new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getId, id)
                .eq(ClassGroup::getDeleted, false));
        if (classGroup == null) {
            throw new BizException(404, "班级不存在");
        }
        DataScopeSupport.requireCampusAccess(classGroup.getCampusId(), classGroup.getCreatedBy());
        return classGroup;
    }

    private ClassGroup loadClassGroupForUpdateWithTenant(Long tenantId, Long id) {
        ClassGroup classGroup = classGroupMapper.selectOne(new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getId, id)
                .eq(ClassGroup::getDeleted, false)
                .last("for update"));
        if (classGroup == null) {
            throw new BizException(404, "班级不存在");
        }
        DataScopeSupport.requireCampusAccess(classGroup.getCampusId(), classGroup.getCreatedBy());
        return classGroup;
    }

    private CourseProduct loadProductWithTenant(Long tenantId, Long id) {
        CourseProduct product = courseProductMapper.selectOne(new LambdaQueryWrapper<CourseProduct>()
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getId, id)
                .eq(CourseProduct::getDeleted, false));
        if (product == null) {
            throw new BizException(404, "课程不存在");
        }
        return product;
    }

    private Student loadStudentWithTenant(Long tenantId, Long id) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getId, id)
                .eq(Student::getDeleted, false);
        DataScopeSupport.applyOwnerScope(wrapper, Student::getCreatedBy);
        Student student = studentMapper.selectOne(wrapper);
        if (student == null) {
            throw new BizException(404, "学员不存在");
        }
        return student;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildSnapshotJson(CourseProduct product, ClassGroup classGroup, Student student) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("courseProductId", product.getId());
            snapshot.put("courseProductName", product.getName());
            snapshot.put("classGroupId", classGroup.getId());
            snapshot.put("classGroupName", classGroup.getName());
            snapshot.put("studentId", student.getId());
            snapshot.put("studentName", student.getName());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "序列化快照数据失败");
        }
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : hasText(second) ? second : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void enforceIdempotency(Long tenantId, String action, Long resourceId, String idempotencyKey) {
        if (!hasText(idempotencyKey)) {
            return;
        }
        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > 128) {
            throw new BizException(400, "幂等键过长");
        }
        String redisKey = "idem:" + tenantId + ":" + action + ":" + (resourceId == null ? "new" : resourceId) + ":" + normalizedKey;
        if (!redisSupportService.reserveOnce(redisKey, Duration.ofMinutes(10))) {
            throw new BizException(409, "请求正在处理或已提交，请勿重复操作");
        }
    }

    private void enforceWriteRateLimit(Long tenantId, String action) {
        String rateKey = "rl:order:" + tenantId + ":" + SecurityContext.accountId() + ":" + action;
        if (!redisSupportService.allow(rateKey, 30, Duration.ofMinutes(1))) {
            throw new BizException(429, "操作过于频繁，请稍后再试");
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String money(BigDecimal value) {
        return defaultAmount(value).toPlainString();
    }

    private String appendCancelRemark(String remark) {
        return hasText(remark) ? remark + " | 订单已取消" : "订单已取消";
    }

    private void recordOperation(String module, String action, String targetType, Long targetId, String detail) {
        Long accountId = SecurityContext.accountId();
        eventPublisher.publishEvent(new OperationLogEvent(
                SecurityContext.tenantId(),
                accountId,
                resolveUsername(accountId),
                module,
                action,
                targetType,
                targetId,
                true,
                RequestTraceContext.requestIdOrNull(),
                detail
        ));
    }

    private void recordOrderAudit(Long orderId, String eventType, String fromStatus, String toStatus, BigDecimal amount, String detail) {
        recordOrderAudit(SecurityContext.tenantId(), orderId, eventType, fromStatus, toStatus, amount, detail);
    }

    private void recordOrderAudit(Long tenantId, Long orderId, String eventType, String fromStatus, String toStatus, BigDecimal amount, String detail) {
        Long accountId = SecurityContext.current().map(p -> p.accountId()).orElse(null);
        jdbcTemplate.update(
                """
                insert into order_audit_log
                (id, tenant_id, order_id, event_type, from_status, to_status, amount, detail,
                 operator_account_id, operator_username, request_id)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                System.currentTimeMillis() * 1000 + Math.floorMod(System.nanoTime(), 1000),
                tenantId,
                orderId,
                eventType,
                fromStatus,
                toStatus,
                amount,
                detail,
                accountId,
                resolveUsername(accountId),
                RequestTraceContext.requestIdOrNull()
        );
    }

    private boolean paymentCallbackExists(Long tenantId, String callbackNo, String channelCode, String channelTradeNo) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from payment_callback_log
                where tenant_id = ?
                  and deleted = 0
                  and (callback_no = ? or (channel_code = ? and channel_trade_no = ?))
                """,
                Integer.class,
                tenantId,
                callbackNo,
                channelCode,
                channelTradeNo
        );
        return count != null && count > 0;
    }

    private void recordPaymentCallback(PaymentCallbackRequest request, Long paymentRecordId, String processResult) {
        jdbcTemplate.update(
                """
                insert into payment_callback_log
                (id, tenant_id, order_id, payment_record_id, callback_no, channel_code, channel_trade_no,
                 callback_status, raw_payload_json, process_result, received_at, processed_at,
                 operator_account_id, operator_username, request_id)
                values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as json), ?, ?, ?, ?, ?, ?)
                """,
                System.currentTimeMillis() * 1000 + Math.floorMod(System.nanoTime(), 1000),
                request.tenantId(),
                request.orderId(),
                paymentRecordId,
                request.callbackNo(),
                request.channelCode(),
                request.channelTradeNo(),
                request.callbackStatus(),
                request.rawPayloadJson(),
                processResult,
                request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt(),
                LocalDateTime.now(),
                SecurityContext.current().map(p -> p.accountId()).orElse(null),
                SecurityContext.current().map(p -> resolveUsername(p.accountId())).orElse("payment-callback"),
                RequestTraceContext.requestIdOrNull()
        );
    }

    private String resolveUsername(Long accountId) {
        if (accountId == null) {
            return "";
        }
        Account account = accountMapper.selectById(accountId);
        return account == null ? "" : account.getUsername();
    }

    private Long tenantId() {
        return SecurityContext.tenantId();
    }

    public record EnrollmentOrderCreateRequest(
            @NotNull Long classGroupId,
            @NotNull Long studentId,
            String customerName,
            String customerPhone,
            String sourceChannel,
            BigDecimal discountAmount,
            String remark
    ) {
    }

    public record PaymentCreateRequest(
            @NotNull BigDecimal amount,
            String paymentMethod,
            String channelTradeNo,
            LocalDateTime receivedAt,
            String remark
    ) {
    }

    public record RefundCreateRequest(
            @NotNull BigDecimal amount,
            String refundMethod,
            String channelTradeNo,
            LocalDateTime refundedAt,
            String refundReason
    ) {
    }

    public record PaymentCallbackRequest(
            @NotNull Long tenantId,
            @NotNull Long orderId,
            @NotBlank String callbackNo,
            @NotBlank String channelCode,
            @NotBlank String paymentNo,
            @NotBlank String channelTradeNo,
            @NotBlank String callbackStatus,
            @NotNull BigDecimal amount,
            String paymentMethod,
            LocalDateTime receivedAt,
            String rawPayloadJson,
            String remark
    ) {
    }
}
