package com.edusphere.security.context;

import java.util.Optional;

public final class SecurityContext {

    private static final ThreadLocal<LoginPrincipal> HOLDER = new ThreadLocal<>();

    private SecurityContext() {
    }

    public static void set(LoginPrincipal principal) {
        HOLDER.set(principal);
    }

    public static Optional<LoginPrincipal> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static Long tenantId() {
        return current()
                .map(LoginPrincipal::tenantId)
                .orElseThrow(() -> new IllegalStateException("No login principal in current request"));
    }

    public static Long accountId() {
        return current()
                .map(LoginPrincipal::accountId)
                .orElseThrow(() -> new IllegalStateException("No login principal in current request"));
    }

    public static void clear() {
        HOLDER.remove();
    }
}
