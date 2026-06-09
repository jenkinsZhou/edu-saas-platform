package com.edusphere.security.jwt;

import com.edusphere.common.trace.RequestTraceContext;
import com.edusphere.security.context.LoginPrincipal;
import com.edusphere.security.context.TenantContext;
import com.edusphere.security.context.SecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                JwtClaims claims;
                try {
                    claims = jwtService.parseAccessToken(token);
                } catch (Exception ex) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null,\"requestId\":\""
                            + RequestTraceContext.requestIdOrNull() + "\"}");
                    return;
                }
                SecurityContext.set(new LoginPrincipal(
                        claims.accountId(),
                        claims.userId(),
                        claims.tenantId(),
                        new LinkedHashSet<>(claims.roles()),
                        new LinkedHashSet<>(claims.permissions()),
                        claims.dataScope(),
                        claims.campusIds()
                ));
                List<SimpleGrantedAuthority> authorities = claims.permissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        claims.accountId(),
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                TenantContext.set(claims.tenantId());
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            SecurityContext.clear();
            TenantContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
