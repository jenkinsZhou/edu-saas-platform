package com.edusphere.security.context;

import java.util.List;
import java.util.Set;

public record LoginPrincipal(
        Long accountId,
        Long userId,
        Long tenantId,
        Set<String> roles,
        Set<String> permissions,
        DataScope dataScope,
        List<Long> campusIds
) {
}
