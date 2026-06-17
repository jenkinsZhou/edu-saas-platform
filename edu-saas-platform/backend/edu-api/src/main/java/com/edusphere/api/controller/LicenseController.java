package com.edusphere.api.controller;

import com.edusphere.api.license.LicenseInfo;
import com.edusphere.api.license.LicenseService;
import com.edusphere.common.api.ApiResult;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 授权管理接口。注意：路径 {@code /api/system/license/**} 已在拦截器放行，
 * 以保证未授权/只读状态下仍可查看机器码并上传授权完成激活。
 */
@RestController
@RequestMapping("/api/system/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping
    @RequirePermission("system:license:view")
    public ApiResult<LicenseInfo> current() {
        return ApiResult.ok(licenseService.currentInfo());
    }

    /** 机器码（部署指纹），供客户报备给厂商以签发绑定授权。 */
    @GetMapping("/fingerprint")
    @RequirePermission("system:license:view")
    public ApiResult<Map<String, Object>> fingerprint() {
        LicenseInfo info = licenseService.currentInfo();
        return ApiResult.ok(Map.of(
                "machineFingerprint", info.machineFingerprint(),
                "status", info.status().name()
        ));
    }

    @PostMapping
    @RequirePermission("system:license:manage")
    public ApiResult<LicenseInfo> activate(@RequestBody @Valid ActivateRequest request) {
        return ApiResult.ok(licenseService.activate(request.licenseText(), SecurityContext.accountId()));
    }

    /** 重新从数据库/文件加载授权（例如运维替换了授权文件后）。 */
    @PostMapping("/reload")
    @RequirePermission("system:license:manage")
    public ApiResult<LicenseInfo> reload() {
        licenseService.reload();
        return ApiResult.ok(licenseService.currentInfo());
    }

    public record ActivateRequest(@NotBlank String licenseText) {
    }
}
