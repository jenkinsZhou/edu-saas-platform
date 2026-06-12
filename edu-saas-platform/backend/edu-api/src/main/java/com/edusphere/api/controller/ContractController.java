package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.order.domain.Contract;
import com.edusphere.order.mapper.ContractMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import com.edusphere.system.domain.Notification;
import com.edusphere.system.mapper.NotificationMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private static final DateTimeFormatter CONTRACT_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ContractMapper contractMapper;
    private final NotificationMapper notificationMapper;
    private final JdbcTemplate jdbcTemplate;

    public ContractController(ContractMapper contractMapper,
                              NotificationMapper notificationMapper,
                              JdbcTemplate jdbcTemplate) {
        this.contractMapper = contractMapper;
        this.notificationMapper = notificationMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @RequirePermission("order:contract:view")
    public ApiResult<PageResult<Contract>> listContracts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .eq(Contract::getTenantId, tenantId)
                .eq(Contract::getDeleted, false);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Contract::getStatus, status);
        }

        wrapper.orderByDesc(Contract::getCreatedAt);
        Page<Contract> result = contractMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping
    @RequirePermission("order:contract:create")
    public ApiResult<Long> createContract(@RequestBody @Valid CreateContractRequest request) {
        Long tenantId = SecurityContext.tenantId();

        Contract contract = new Contract();
        contract.setTenantId(tenantId);
        contract.setContractNo(generateContractNo(tenantId));
        contract.setStudentId(request.studentId());
        contract.setOrderId(request.orderId());
        contract.setContractType(request.contractType());
        contract.setContractAmount(request.contractAmount());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setStatus("PENDING");
        contract.setCreatedBy(SecurityContext.accountId());
        contractMapper.insert(contract);
        return ApiResult.ok(contract.getId());
    }

    @GetMapping("/expiring")
    @RequirePermission("order:contract:view")
    public ApiResult<List<Map<String, Object>>> getExpiringContracts(@RequestParam(defaultValue = "30") int daysThreshold) {
        Long tenantId = SecurityContext.tenantId();
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);

        String sql = """
            SELECT
                c.id as contract_id,
                c.contract_no,
                c.student_id,
                s.name as student_name,
                s.phone as student_phone,
                c.end_date,
                DATEDIFF(c.end_date, CURDATE()) as days_remaining
            FROM contract c
            INNER JOIN student s ON s.id = c.student_id AND s.deleted = 0
            WHERE c.tenant_id = ? AND c.deleted = 0
                AND c.status = 'ACTIVE'
                AND c.end_date <= ?
                AND c.end_date >= CURDATE()
            ORDER BY c.end_date ASC
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("contractId", rs.getLong("contract_id"));
                    map.put("contractNo", rs.getString("contract_no"));
                    map.put("studentId", rs.getLong("student_id"));
                    map.put("studentName", rs.getString("student_name"));
                    map.put("studentPhone", rs.getString("student_phone"));
                    map.put("endDate", rs.getDate("end_date").toLocalDate());
                    map.put("daysRemaining", rs.getInt("days_remaining"));
                    return map;
                },
                tenantId, thresholdDate);

        return ApiResult.ok(results);
    }

    @PostMapping("/batch-expiry-reminder")
    @RequirePermission("order:contract:notify")
    public ApiResult<Integer> sendBatchExpiryReminder(@RequestParam(defaultValue = "30") int daysThreshold) {
        Long tenantId = SecurityContext.tenantId();
        List<Map<String, Object>> contracts = getExpiringContracts(daysThreshold).data();

        int count = 0;
        for (Map<String, Object> contract : contracts) {
            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setNotificationType("CONTRACT_EXPIRY_REMINDER");
            notification.setTitle("合同即将到期提醒");
            notification.setContent(String.format("您的合同（%s）将在%d天后到期，请及时续签",
                    contract.get("contractNo"), contract.get("daysRemaining")));
            notification.setTargetType("STUDENT");
            notification.setTargetId((Long) contract.get("studentId"));
            notification.setStatus("PENDING");
            notification.setChannel("SMS,WECHAT");
            notification.setCreatedBy(SecurityContext.accountId());
            notificationMapper.insert(notification);
            count++;
        }

        return ApiResult.ok(count);
    }

    private String generateContractNo(Long tenantId) {
        String datePart = LocalDate.now().format(CONTRACT_NO_FORMAT);
        byte[] randomBytes = new byte[4];
        RANDOM.nextBytes(randomBytes);
        long randomPart = Math.abs(((randomBytes[0] & 0xFF) << 24) |
                ((randomBytes[1] & 0xFF) << 16) |
                ((randomBytes[2] & 0xFF) << 8) |
                (randomBytes[3] & 0xFF));
        return String.format("CT%s%010d", datePart, randomPart % 10000000000L);
    }

    public record CreateContractRequest(
            @NotNull Long studentId,
            Long orderId,
            @NotBlank String contractType,
            @NotNull BigDecimal contractAmount,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}
}
