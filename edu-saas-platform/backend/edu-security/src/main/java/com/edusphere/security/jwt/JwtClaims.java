package com.edusphere.security.jwt;

import com.edusphere.security.context.DataScope;

import java.util.List;

public record JwtClaims(
        Long accountId,
        Long userId,
        Long tenantId,
        List<String> roles,
        List<String> permissions,
        DataScope dataScope,
        List<Long> campusIds
) {
}
