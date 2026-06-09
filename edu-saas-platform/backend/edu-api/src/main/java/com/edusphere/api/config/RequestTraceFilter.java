package com.edusphere.api.config;

import com.edusphere.common.trace.RequestTraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final long slowRequestThresholdMs;
    private final boolean accessLogEnabled;

    public RequestTraceFilter(
            @Value("${edu.observability.slow-request-threshold-ms:1000}") long slowRequestThresholdMs,
            @Value("${edu.observability.access-log-enabled:true}") boolean accessLogEnabled
    ) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
        this.accessLogEnabled = accessLogEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.currentTimeMillis();
        RequestTraceContext.setRequestId(requestId);
        MDC.put(RequestTraceContext.REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = System.currentTimeMillis() - startedAt;
            MDC.put("costMs", String.valueOf(costMs));
            writeAccessLog(request, response, costMs);
            MDC.remove("costMs");
            MDC.remove(RequestTraceContext.REQUEST_ID_MDC_KEY);
            RequestTraceContext.clear();
        }
    }

    private void writeAccessLog(HttpServletRequest request, HttpServletResponse response, long costMs) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null || query.isBlank() ? uri : uri + "?" + query;
        String remoteAddr = clientIp(request);
        if (costMs >= slowRequestThresholdMs || status >= 500) {
            log.warn("api_access method={} path={} status={} costMs={} clientIp={}", method, path, status, costMs, remoteAddr);
            return;
        }
        if (accessLogEnabled) {
            log.info("api_access method={} path={} status={} costMs={} clientIp={}", method, path, status, costMs, remoteAddr);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(RequestTraceContext.REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId) && requestId.length() <= 64) {
            return requestId;
        }
        return "req-" + Instant.now().toEpochMilli() + "-" + Long.toUnsignedString(RANDOM.nextLong(), 36);
    }
}
