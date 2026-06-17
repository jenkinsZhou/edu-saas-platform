package com.edusphere.api.config;

import com.edusphere.api.license.LicenseEnforcementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册授权拦截器。放行登录/授权管理/健康检查/支付回调等接口，
 * 以保证未授权时仍能登录、查看机器码并上传授权文件完成激活。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LicenseEnforcementInterceptor licenseEnforcementInterceptor;

    public WebMvcConfig(LicenseEnforcementInterceptor licenseEnforcementInterceptor) {
        this.licenseEnforcementInterceptor = licenseEnforcementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseEnforcementInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/system/license/**",
                        "/api/tenant/register",
                        "/api/orders/callbacks/**",
                        "/api/health/**"
                );
    }
}
