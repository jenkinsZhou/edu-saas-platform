package com.edusphere.common.api;

import com.edusphere.common.trace.RequestTraceContext;

public record ApiResult<T>(int code, String message, T data, String requestId) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, RequestTraceContext.requestIdOrNull());
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(0, "ok", null, RequestTraceContext.requestIdOrNull());
    }

    public static ApiResult<Void> fail(int code, String message) {
        return new ApiResult<>(code, message, null, RequestTraceContext.requestIdOrNull());
    }
}
