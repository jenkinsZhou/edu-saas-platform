package com.edusphere.common.trace;

import java.util.Optional;

public final class RequestTraceContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static Optional<String> requestId() {
        return Optional.ofNullable(REQUEST_ID.get());
    }

    public static String requestIdOrNull() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
