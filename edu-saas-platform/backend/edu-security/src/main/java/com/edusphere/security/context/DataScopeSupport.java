package com.edusphere.security.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.edusphere.common.exception.BizException;

import java.util.List;

public final class DataScopeSupport {

    private DataScopeSupport() {
    }

    public static <T> void applyCampusScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> campusField, SFunction<T, Long> createdByField) {
        LoginPrincipal principal = principal();
        DataScope scope = principal.dataScope();
        if (scope == DataScope.ALL || scope == DataScope.TENANT) {
            return;
        }
        if (scope == DataScope.OWNER) {
            wrapper.eq(createdByField, principal.accountId());
            return;
        }
        List<Long> campusIds = principal.campusIds();
        if (campusIds == null || campusIds.isEmpty()) {
            wrapper.eq(createdByField, -1L);
            return;
        }
        wrapper.in(campusField, campusIds);
    }

    public static <T> void applyOwnerScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> createdByField) {
        LoginPrincipal principal = principal();
        if (principal.dataScope() == DataScope.OWNER) {
            wrapper.eq(createdByField, principal.accountId());
        }
    }

    public static boolean canAccessCampus(Long campusId, Long createdBy) {
        LoginPrincipal principal = principal();
        DataScope scope = principal.dataScope();
        if (scope == DataScope.ALL || scope == DataScope.TENANT) {
            return true;
        }
        if (scope == DataScope.OWNER) {
            return createdBy != null && createdBy.equals(principal.accountId());
        }
        return campusId != null && principal.campusIds() != null && principal.campusIds().contains(campusId);
    }

    public static void requireCampusAccess(Long campusId, Long createdBy) {
        if (!canAccessCampus(campusId, createdBy)) {
            throw new BizException(403, "没有数据权限");
        }
    }

    private static LoginPrincipal principal() {
        return SecurityContext.current()
                .orElseThrow(() -> new IllegalStateException("No login principal in current request"));
    }
}
