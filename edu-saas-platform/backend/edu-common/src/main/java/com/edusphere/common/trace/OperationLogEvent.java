package com.edusphere.common.trace;

public record OperationLogEvent(
        Long tenantId,
        Long accountId,
        String username,
        String module,
        String action,
        String targetType,
        Long targetId,
        Boolean success,
        String requestId,
        String detail
) {
}
