package com.edusphere.security.permission;

import com.edusphere.common.exception.BizException;
import com.edusphere.security.context.SecurityContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.edusphere.security.permission.RequirePermission) || @within(com.edusphere.security.permission.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission permission = resolvePermission(joinPoint);
        if (permission == null) {
            return joinPoint.proceed();
        }
        boolean allowed = SecurityContext.current()
                .map(principal -> principal.permissions().contains(permission.value()))
                .orElse(false);
        if (!allowed) {
            throw new BizException(403, "没有操作权限：" + permission.value());
        }
        return joinPoint.proceed();
    }

    private RequirePermission resolvePermission(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequirePermission permission = AnnotationUtils.findAnnotation(method, RequirePermission.class);
        if (permission != null) {
            return permission;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequirePermission.class);
    }
}
