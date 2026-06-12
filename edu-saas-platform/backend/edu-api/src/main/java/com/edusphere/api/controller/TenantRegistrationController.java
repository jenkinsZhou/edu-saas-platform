package com.edusphere.api.controller;

import com.edusphere.api.service.tenant.TenantProvisioningService;
import com.edusphere.common.api.ApiResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 租户自助入驻（公开接口，无需登录）
 */
@RestController
@RequestMapping("/api/tenant/register")
public class TenantRegistrationController {

    private final TenantProvisioningService provisioningService;

    public TenantRegistrationController(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @PostMapping
    public ApiResult<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request) {
        TenantProvisioningService.RegistrationResult result = provisioningService.register(
                new TenantProvisioningService.RegistrationRequest(
                        request.organizationName(),
                        request.organizationCode(),
                        request.adminUsername(),
                        request.adminPassword(),
                        request.adminPhone(),
                        request.adminEmail(),
                        request.planCode()
                ));
        return ApiResult.ok(Map.of(
                "tenantId", result.tenantId(),
                "organizationCode", result.organizationCode(),
                "adminUsername", result.adminUsername()
        ));
    }

    public record RegisterRequest(
            @NotBlank(message = "机构名称不能为空") String organizationName,
            @NotBlank(message = "机构编码不能为空") String organizationCode,
            @NotBlank(message = "管理员账号不能为空") String adminUsername,
            @NotBlank(message = "密码不能为空") String adminPassword,
            String adminPhone,
            String adminEmail,
            @NotBlank(message = "请选择套餐") String planCode
    ) {
    }
}
