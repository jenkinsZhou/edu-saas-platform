package com.edusphere.api.license;

import com.edusphere.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;

/**
 * 授权拦截：
 * <ul>
 *   <li>功能模块开关：访问未授权模块的接口直接 403；</li>
 *   <li>宽限期后只读：授权失效时拦截一切写操作（POST/PUT/DELETE/PATCH），返回 402。</li>
 * </ul>
 * 抛出的 {@link BizException} 由 {@code ApiExceptionHandler} 统一格式化。
 */
@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final LicenseService licenseService;
    private final LicenseProperties properties;

    public LicenseEnforcementInterceptor(LicenseService licenseService, LicenseProperties properties) {
        this.licenseService = licenseService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!licenseService.enforcementEnabled()) {
            return true;
        }
        String path = request.getRequestURI();

        // 使用 402（而非 403）：避免前端 http 拦截器对 403 触发登出跳转，语义上也对应“需授权”
        String feature = matchFeature(path);
        if (feature != null && !licenseService.isFeatureEnabled(feature)) {
            throw new BizException(402, "当前授权未包含该功能模块（" + feature + "），如需开通请联系厂商升级授权");
        }

        if (WRITE_METHODS.contains(request.getMethod().toUpperCase()) && !licenseService.writable()) {
            throw new BizException(402, licenseService.currentInfo().message());
        }
        return true;
    }

    private String matchFeature(String path) {
        for (Map.Entry<String, String> entry : properties.getFeaturePaths().entrySet()) {
            String prefix = entry.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }
}
