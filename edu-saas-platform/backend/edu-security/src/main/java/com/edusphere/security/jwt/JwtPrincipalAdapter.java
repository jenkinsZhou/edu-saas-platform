package com.edusphere.security.jwt;

import com.edusphere.security.context.LoginPrincipal;

import java.util.List;

public final class JwtPrincipalAdapter {

    private JwtPrincipalAdapter() {
    }

    public static JwtClaims from(LoginPrincipal principal) {
        return new JwtClaims(
                principal.accountId(),
                principal.userId(),
                principal.tenantId(),
                List.copyOf(principal.roles()),
                List.copyOf(principal.permissions()),
                principal.dataScope(),
                List.copyOf(principal.campusIds())
        );
    }
}
