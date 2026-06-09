package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.common.exception.BizException;
import com.edusphere.system.domain.Account;
import com.edusphere.system.domain.Role;
import com.edusphere.system.domain.SysUser;
import com.edusphere.system.mapper.AccountMapper;
import com.edusphere.system.mapper.RoleMapper;
import com.edusphere.system.mapper.SysUserMapper;
import com.edusphere.security.context.LoginPrincipal;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.jwt.JwtClaims;
import com.edusphere.security.jwt.JwtPrincipalAdapter;
import com.edusphere.security.jwt.JwtService;
import com.edusphere.api.service.security.AuthTokenService;
import com.edusphere.api.service.security.PasswordService;
import com.edusphere.api.service.tenant.OrganizationResolver;
import com.edusphere.tenant.domain.Tenant;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final JdbcTemplate jdbcTemplate;
    private final JwtService jwtService;
    private final RedisSupportService redisSupportService;
    private final OrganizationResolver organizationResolver;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;

    public AuthController(
            AccountMapper accountMapper,
            SysUserMapper sysUserMapper,
            RoleMapper roleMapper,
            JdbcTemplate jdbcTemplate,
            JwtService jwtService,
            RedisSupportService redisSupportService,
            OrganizationResolver organizationResolver,
            PasswordService passwordService,
            AuthTokenService authTokenService
    ) {
        this.accountMapper = accountMapper;
        this.sysUserMapper = sysUserMapper;
        this.roleMapper = roleMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.jwtService = jwtService;
        this.redisSupportService = redisSupportService;
        this.organizationResolver = organizationResolver;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        Tenant organization = organizationResolver.resolve(servletRequest, request.organizationCode());
        String rateKey = "rl:login:" + organization.getId() + ":" + servletRequest.getRemoteAddr() + ":" + request.username().toLowerCase();
        if (!redisSupportService.allow(rateKey, 10, Duration.ofMinutes(1))) {
            throw new BizException(429, "登录过于频繁，请稍后再试");
        }
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, organization.getId())
                .eq(Account::getUsername, request.username())
                .eq(Account::getDeleted, false));
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (!passwordService.matches(request.password(), account.getPasswordHash())) {
            throw new BizException(401, "账号或密码错误");
        }
        upgradePasswordIfNeeded(account, request.password());

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, organization.getId())
                .eq(SysUser::getAccountId, account.getId())
                .eq(SysUser::getDeleted, false));

        LoginProfile profile = loadLoginProfile(account, user);

        LoginPrincipal principal = new LoginPrincipal(
                account.getId(),
                profile.userId(),
                account.getTenantId(),
                Set.copyOf(profile.roleCodes()),
                Set.copyOf(profile.permissions()),
                profile.dataScope(),
                profile.campusIds()
        );
        return ApiResult.ok(buildLoginResult(account, organization, profile, principal, authTokenService.issueRefreshToken(account.getTenantId(), account.getId())));
    }

    @PostMapping("/refresh")
    public ApiResult<Map<String, Object>> refresh(@RequestBody @Valid RefreshRequest request) {
        AuthTokenService.RefreshTokenSubject subject = authTokenService.requireRefreshToken(request.refreshToken());
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, subject.tenantId())
                .eq(Account::getId, subject.accountId())
                .eq(Account::getDeleted, false));
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            authTokenService.revokeRefreshToken(request.refreshToken());
            throw new BizException(401, "账号不存在或已停用");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, account.getTenantId())
                .eq(SysUser::getAccountId, account.getId())
                .eq(SysUser::getDeleted, false));
        LoginProfile profile = loadLoginProfile(account, user);
        LoginPrincipal principal = new LoginPrincipal(
                account.getId(),
                profile.userId(),
                account.getTenantId(),
                Set.copyOf(profile.roleCodes()),
                Set.copyOf(profile.permissions()),
                profile.dataScope(),
                profile.campusIds()
        );
        Tenant organization = loadOrganization(account.getTenantId());
        return ApiResult.ok(buildLoginResult(account, organization, profile, principal, request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest servletRequest) {
        String accessToken = resolveBearerToken(servletRequest);
        if (accessToken != null && !accessToken.isBlank()) {
            jwtService.revokeAccessToken(accessToken);
        }
        if (request != null) {
            authTokenService.revokeRefreshToken(request.refreshToken());
        }
        SecurityContext.clear();
        return ApiResult.ok();
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String organizationCode
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    private com.edusphere.security.context.DataScope effectiveDataScope(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return com.edusphere.security.context.DataScope.TENANT;
        }
        if (roles.stream().anyMatch(role -> role.getDataScope() == com.edusphere.security.context.DataScope.ALL)) {
            return com.edusphere.security.context.DataScope.ALL;
        }
        if (roles.stream().anyMatch(role -> role.getDataScope() == com.edusphere.security.context.DataScope.TENANT)) {
            return com.edusphere.security.context.DataScope.TENANT;
        }
        if (roles.stream().anyMatch(role -> role.getDataScope() == com.edusphere.security.context.DataScope.CAMPUS)) {
            return com.edusphere.security.context.DataScope.CAMPUS;
        }
        if (roles.stream().anyMatch(role -> role.getDataScope() == com.edusphere.security.context.DataScope.CUSTOM)) {
            return com.edusphere.security.context.DataScope.CUSTOM;
        }
        return com.edusphere.security.context.DataScope.OWNER;
    }

    private LoginProfile loadLoginProfile(Account account, SysUser user) {
        Long organizationId = account.getTenantId();
        String cacheKey = "auth:profile:" + organizationId + ":" + account.getId();
        return redisSupportService.getOrLoadJson(cacheKey, Duration.ofMinutes(5), LoginProfile.class, () -> {
            List<Long> roleIds = jdbcTemplate.queryForList(
                    "select role_id from account_role where tenant_id = ? and account_id = ?",
                    Long.class,
                    organizationId,
                    account.getId()
            );
            List<Role> roles = roleIds.isEmpty()
                    ? List.of()
                    : roleMapper.selectBatchIds(roleIds);
            List<String> roleCodes = roles.stream().map(Role::getCode).toList();
            List<String> permissions = jdbcTemplate.queryForList("""
                    select distinct mp.permission_code
                    from account_role ar
                    join role_permission rp on rp.tenant_id = ar.tenant_id and rp.role_id = ar.role_id
                    join menu_permission mp on mp.tenant_id = rp.tenant_id and mp.id = rp.permission_id
                    where ar.tenant_id = ? and ar.account_id = ? and mp.permission_code is not null
                    order by mp.permission_code
                    """, String.class, organizationId, account.getId());
            List<Long> campusIds = jdbcTemplate.queryForList(
                    "select campus_id from account_campus where tenant_id = ? and account_id = ?",
                    Long.class,
                    organizationId,
                    account.getId()
            );
            return new LoginProfile(
                    user == null ? account.getId() : user.getId(),
                    user == null ? account.getUsername() : user.getRealName(),
                    roleCodes,
                    permissions,
                    campusIds,
                    effectiveDataScope(roles)
            );
        });
    }

    private void upgradePasswordIfNeeded(Account account, String rawPassword) {
        if (!passwordService.needsUpgrade(account.getPasswordHash())) {
            return;
        }
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getTenantId, account.getTenantId())
                .eq(Account::getId, account.getId())
                .eq(Account::getDeleted, false)
                .set(Account::getPasswordHash, passwordService.encode(rawPassword)));
    }

    private Map<String, Object> buildLoginResult(Account account, Tenant organization, LoginProfile profile, LoginPrincipal principal, String refreshToken) {
        JwtClaims claims = JwtPrincipalAdapter.from(principal);
        return Map.of(
                "accessToken", jwtService.createAccessToken(claims),
                "refreshToken", refreshToken,
                "expiresIn", 7200,
                "user", Map.of(
                        "id", profile.userId(),
                        "accountId", account.getId(),
                        "name", profile.realName(),
                        "tenantId", account.getTenantId(),
                        "organizationName", organization.getName(),
                        "organizationCode", organization.getCode(),
                        "roles", profile.roleCodes(),
                        "permissions", profile.permissions(),
                        "campusIds", profile.campusIds()
                )
        );
    }

    private Tenant loadOrganization(Long organizationId) {
        return jdbcTemplate.query("""
                        select id, name, code, status, plan_code
                        from tenant
                        where id = ? and deleted = 0
                        limit 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        throw new BizException(401, "机构不存在或已停用");
                    }
                    Tenant item = new Tenant();
                    item.setId(rs.getLong("id"));
                    item.setName(rs.getString("name"));
                    item.setCode(rs.getString("code"));
                    item.setStatus(rs.getString("status"));
                    item.setPlanCode(rs.getString("plan_code"));
                    return item;
                },
                organizationId);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public record LoginProfile(
            Long userId,
            String realName,
            List<String> roleCodes,
            List<String> permissions,
            List<Long> campusIds,
            com.edusphere.security.context.DataScope dataScope
    ) {
    }
}
