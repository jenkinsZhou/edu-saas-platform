package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.cache.RedisSupportService;
import com.edusphere.common.exception.BizException;
import com.edusphere.common.trace.OperationLogEvent;
import com.edusphere.common.trace.RequestTraceContext;
import com.edusphere.api.service.security.PasswordService;
import com.edusphere.security.context.DataScope;
import com.edusphere.security.context.LoginPrincipal;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import com.edusphere.system.domain.Account;
import com.edusphere.system.domain.MenuPermission;
import com.edusphere.system.domain.OperationLog;
import com.edusphere.system.domain.Role;
import com.edusphere.system.domain.SysUser;
import com.edusphere.system.mapper.AccountMapper;
import com.edusphere.system.mapper.MenuPermissionMapper;
import com.edusphere.system.mapper.OperationLogMapper;
import com.edusphere.system.mapper.RoleMapper;
import com.edusphere.system.mapper.SysUserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.Duration;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final MenuPermissionMapper menuPermissionMapper;
    private final OperationLogMapper operationLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisSupportService redisSupportService;
    private final PasswordService passwordService;
    private final com.edusphere.api.license.LicenseQuotaService licenseQuotaService;
    private final com.edusphere.api.license.LicenseService licenseService;

    public SystemController(
            AccountMapper accountMapper,
            SysUserMapper sysUserMapper,
            RoleMapper roleMapper,
            MenuPermissionMapper menuPermissionMapper,
            OperationLogMapper operationLogMapper,
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher eventPublisher,
            RedisSupportService redisSupportService,
            PasswordService passwordService,
            com.edusphere.api.license.LicenseQuotaService licenseQuotaService,
            com.edusphere.api.license.LicenseService licenseService
    ) {
        this.accountMapper = accountMapper;
        this.sysUserMapper = sysUserMapper;
        this.roleMapper = roleMapper;
        this.menuPermissionMapper = menuPermissionMapper;
        this.operationLogMapper = operationLogMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
        this.redisSupportService = redisSupportService;
        this.passwordService = passwordService;
        this.licenseQuotaService = licenseQuotaService;
        this.licenseService = licenseService;
    }

    @GetMapping("/accounts")
    @RequirePermission("system:account:view")
    public ApiResult<PageResult<Map<String, Object>>> accounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getDeleted, false)
                .orderByDesc(Account::getCreatedAt);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(Account::getUsername, keyword)
                    .or()
                    .like(Account::getPhone, keyword)
                    .or()
                    .like(Account::getEmail, keyword));
        }
        if (hasText(status)) {
            wrapper.eq(Account::getStatus, status);
        }
        Page<Account> accountPage = accountMapper.selectPage(new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);
        List<Account> accounts = accountPage.getRecords();
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        Map<Long, SysUser> userByAccountId = new LinkedHashMap<>();
        if (!accountIds.isEmpty()) {
            sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getTenantId, tenantId)
                            .in(SysUser::getAccountId, accountIds)
                            .eq(SysUser::getDeleted, false))
                    .forEach(user -> userByAccountId.put(user.getAccountId(), user));
        }
        return ApiResult.ok(PageResult.of(accounts.stream()
                .map(account -> {
                    SysUser user = userByAccountId.get(account.getId());
                    return Map.<String, Object>of(
                            "id", account.getId(),
                            "username", account.getUsername(),
                            "realName", user == null ? "" : user.getRealName(),
                            "phone", account.getPhone() == null ? "" : account.getPhone(),
                            "email", account.getEmail() == null ? "" : account.getEmail(),
                            "status", account.getStatus(),
                            "roleIds", accountRoleIds(tenantId, account.getId()),
                            "campusIds", accountCampusIds(tenantId, account.getId())
                    );
                })
                .toList(), accountPage.getTotal(), accountPage.getCurrent(), accountPage.getSize(), buildAccountSummary(tenantId, keyword, status)));
    }

    @PostMapping("/accounts")
    @RequirePermission("system:account:create")
    @Transactional
    public ApiResult<Long> createAccount(@RequestBody @Valid AccountRequest request) {
        Long tenantId = SecurityContext.tenantId();
        licenseQuotaService.assertCanCreateAccount();
        Account duplicated = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getUsername, request.username())
                .eq(Account::getDeleted, false));
        if (duplicated != null) {
            throw new BizException(409, "账号名已存在");
        }

        Account account = new Account();
        account.setTenantId(tenantId);
        account.setUsername(request.username());
        account.setPhone(request.phone());
        account.setEmail(request.email());
        requireStrongPassword(request.password());
        account.setPasswordHash(passwordService.encode(request.password()));
        account.setStatus(request.status());
        account.setCreatedBy(SecurityContext.accountId());
        accountMapper.insert(account);

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setAccountId(account.getId());
        user.setRealName(request.realName());
        user.setGender("UNKNOWN");
        user.setCreatedBy(SecurityContext.accountId());
        sysUserMapper.insert(user);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            assignAccountRoles(tenantId, account.getId(), request.roleIds());
        }
        assignAccountCampuses(tenantId, account.getId(), request.campusIds());
        evictAccountAuthCache(tenantId, account.getId());
        recordOperation("SYSTEM", "CREATE_ACCOUNT", "ACCOUNT", account.getId(), "username=" + account.getUsername());
        return ApiResult.ok(account.getId());
    }

    @PutMapping("/accounts/{id}")
    @RequirePermission("system:account:update")
    @Transactional
    public ApiResult<Void> updateAccount(@PathVariable Long id, @RequestBody @Valid AccountUpdateRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getId, id)
                .eq(Account::getDeleted, false));
        if (account == null) {
            throw new BizException(404, "账号不存在");
        }

        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getId, id)
                .eq(Account::getDeleted, false)
                .set(Account::getPhone, request.phone())
                .set(Account::getEmail, request.email())
                .set(Account::getUpdatedBy, SecurityContext.accountId()));

        int updatedUsers = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getAccountId, id)
                .eq(SysUser::getDeleted, false)
                .set(SysUser::getRealName, request.realName()));
        if (updatedUsers == 0) {
            SysUser user = new SysUser();
            user.setTenantId(tenantId);
            user.setAccountId(id);
            user.setRealName(request.realName());
            user.setGender("UNKNOWN");
            user.setCreatedBy(SecurityContext.accountId());
            sysUserMapper.insert(user);
        }

        assignAccountRoles(tenantId, id, request.roleIds());
        assignAccountCampuses(tenantId, id, request.campusIds());
        evictAccountAuthCache(tenantId, id);
        recordOperation("SYSTEM", "UPDATE_ACCOUNT", "ACCOUNT", id, "username=" + account.getUsername());
        return ApiResult.ok();
    }

    @PutMapping("/accounts/{id}/password")
    @RequirePermission("system:account:update")
    @Transactional
    public ApiResult<Void> resetAccountPassword(@PathVariable Long id, @RequestBody @Valid PasswordResetRequest request) {
        Long tenantId = SecurityContext.tenantId();
        requireStrongPassword(request.password());
        int updated = accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getId, id)
                .eq(Account::getDeleted, false)
                .set(Account::getPasswordHash, passwordService.encode(request.password())));
        if (updated == 0) {
            throw new BizException(404, "账号不存在");
        }
        evictAccountAuthCache(tenantId, id);
        recordOperation("SYSTEM", "RESET_ACCOUNT_PASSWORD", "ACCOUNT", id, null);
        return ApiResult.ok();
    }

    @PutMapping("/accounts/{id}/status")
    @RequirePermission("system:account:update")
    @Transactional
    public ApiResult<Void> updateAccountStatus(@PathVariable Long id, @RequestBody @Valid StatusRequest request) {
        Long tenantId = SecurityContext.tenantId();
        int updated = accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getId, id)
                .eq(Account::getDeleted, false)
                .set(Account::getStatus, request.status()));
        if (updated == 0) {
            throw new BizException(404, "账号不存在");
        }
        evictAccountAuthCache(tenantId, id);
        recordOperation("SYSTEM", "UPDATE_ACCOUNT_STATUS", "ACCOUNT", id, "status=" + request.status());
        return ApiResult.ok();
    }

    @GetMapping("/roles")
    @RequirePermission("system:role:view")
    public ApiResult<PageResult<Map<String, Object>>> roles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataScope,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getDeleted, false)
                .orderByAsc(Role::getId);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(Role::getName, keyword)
                    .or()
                    .like(Role::getCode, keyword));
        }
        if (hasText(dataScope)) {
            wrapper.eq(Role::getDataScope, dataScope);
        }
        Page<Role> rolePage = roleMapper.selectPage(new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);
        List<Role> roles = rolePage.getRecords();
        return ApiResult.ok(PageResult.of(roles.stream()
                .map(role -> Map.<String, Object>of(
                        "id", role.getId(),
                        "name", role.getName(),
                        "code", role.getCode(),
                        "dataScope", role.getDataScope(),
                        "systemBuiltin", role.getSystemBuiltin()
                ))
                .toList(), rolePage.getTotal(), rolePage.getCurrent(), rolePage.getSize(), buildRoleSummary(tenantId, keyword, dataScope)));
    }

    @PostMapping("/roles")
    @RequirePermission("system:role:create")
    @Transactional
    public ApiResult<Long> createRole(@RequestBody @Valid RoleRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Role duplicated = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getCode, request.code())
                .eq(Role::getDeleted, false));
        if (duplicated != null) {
            throw new BizException(409, "角色编码已存在");
        }
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(request.name());
        role.setCode(request.code());
        role.setDataScope(request.dataScope());
        role.setSystemBuiltin(false);
        roleMapper.insert(role);
        if (request.permissionIds() != null) {
            assignRolePermissions(tenantId, role.getId(), request.permissionIds());
        }
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "CREATE_ROLE", "ROLE", role.getId(), "code=" + role.getCode());
        return ApiResult.ok(role.getId());
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("system:role:create")
    @Transactional
    public ApiResult<Void> updateRole(@PathVariable Long id, @RequestBody @Valid RoleUpdateRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getId, id)
                .eq(Role::getDeleted, false));
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        int updated = roleMapper.update(null, new LambdaUpdateWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getId, id)
                .eq(Role::getDeleted, false)
                .set(Role::getName, request.name())
                .set(Role::getDataScope, request.dataScope()));
        if (updated == 0) {
            throw new BizException(404, "角色不存在");
        }
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "UPDATE_ROLE", "ROLE", id, "code=" + role.getCode());
        return ApiResult.ok();
    }

    @PutMapping("/roles/{id}/permissions")
    @RequirePermission("system:role:assign")
    @Transactional
    public ApiResult<Void> assignRolePermissions(@PathVariable Long id, @RequestBody @Valid RolePermissionRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getId, id)
                .eq(Role::getDeleted, false));
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        assignRolePermissions(tenantId, id, request.permissionIds());
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "ASSIGN_ROLE_PERMISSION", "ROLE", id, "permissions=" + request.permissionIds().size());
        return ApiResult.ok();
    }

    @GetMapping("/roles/{id}/permissions")
    @RequirePermission("system:role:view")
    public ApiResult<List<Long>> rolePermissions(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getId, id)
                .eq(Role::getDeleted, false));
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        List<Long> permissionIds = jdbcTemplate.queryForList(
                "select permission_id from role_permission where tenant_id = ? and role_id = ?",
                Long.class,
                tenantId,
                id
        );
        return ApiResult.ok(permissionIds);
    }

    @GetMapping("/menus")
    @RequirePermission("system:menu:view")
    public ApiResult<PageResult<Map<String, Object>>> menus(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<MenuPermission> wrapper = new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getDeleted, false)
                .orderByAsc(MenuPermission::getSortNo);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(MenuPermission::getName, keyword)
                    .or()
                    .like(MenuPermission::getRoutePath, keyword)
                    .or()
                    .like(MenuPermission::getPermissionCode, keyword));
        }
        if (hasText(type)) {
            wrapper.eq(MenuPermission::getType, type);
        }
        Page<MenuPermission> menuPage = menuPermissionMapper.selectPage(new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);
        List<MenuPermission> menus = menuPage.getRecords();
        return ApiResult.ok(PageResult.of(menus.stream()
                .map(menu -> Map.<String, Object>of(
                        "id", menu.getId(),
                        "parentId", menu.getParentId() == null ? 0L : menu.getParentId(),
                        "name", menu.getName(),
                        "type", menu.getType(),
                        "path", menu.getRoutePath() == null ? "" : menu.getRoutePath(),
                        "permissionCode", menu.getPermissionCode() == null ? "" : menu.getPermissionCode(),
                        "sortNo", menu.getSortNo()
                ))
                .toList(), menuPage.getTotal(), menuPage.getCurrent(), menuPage.getSize(), buildMenuSummary(tenantId, keyword, type)));
    }

    @PostMapping("/menus")
    @RequirePermission("system:menu:create")
    @Transactional
    public ApiResult<Long> createMenu(@RequestBody @Valid MenuRequest request) {
        Long tenantId = SecurityContext.tenantId();
        ensurePermissionCodeAvailable(tenantId, null, request.permissionCode());
        MenuPermission menu = new MenuPermission();
        menu.setTenantId(tenantId);
        menu.setParentId(normalizeParentId(request.parentId()));
        menu.setName(request.name());
        menu.setType(request.type());
        menu.setRoutePath(request.routePath());
        menu.setPermissionCode(request.permissionCode());
        menu.setSortNo(request.sortNo());
        menuPermissionMapper.insert(menu);
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "CREATE_MENU", "MENU_PERMISSION", menu.getId(), "name=" + menu.getName());
        return ApiResult.ok(menu.getId());
    }

    @PutMapping("/menus/{id}")
    @RequirePermission("system:menu:update")
    @Transactional
    public ApiResult<Void> updateMenu(@PathVariable Long id, @RequestBody @Valid MenuRequest request) {
        Long tenantId = SecurityContext.tenantId();
        MenuPermission menu = menuPermissionMapper.selectOne(new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getId, id)
                .eq(MenuPermission::getDeleted, false));
        if (menu == null) {
            throw new BizException(404, "菜单不存在");
        }
        Long parentId = normalizeParentId(request.parentId());
        if (Objects.equals(parentId, id)) {
            throw new BizException(400, "上级菜单不能选择自己");
        }
        ensurePermissionCodeAvailable(tenantId, id, request.permissionCode());
        int updated = menuPermissionMapper.update(null, new LambdaUpdateWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getId, id)
                .eq(MenuPermission::getDeleted, false)
                .set(MenuPermission::getParentId, parentId)
                .set(MenuPermission::getName, request.name())
                .set(MenuPermission::getType, request.type())
                .set(MenuPermission::getRoutePath, request.routePath())
                .set(MenuPermission::getPermissionCode, request.permissionCode())
                .set(MenuPermission::getSortNo, request.sortNo()));
        if (updated == 0) {
            throw new BizException(404, "菜单不存在");
        }
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "UPDATE_MENU", "MENU_PERMISSION", id, "name=" + request.name());
        return ApiResult.ok();
    }

    @DeleteMapping("/menus/{id}")
    @RequirePermission("system:menu:delete")
    @Transactional
    public ApiResult<Void> deleteMenu(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        Long childCount = menuPermissionMapper.selectCount(new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getParentId, id)
                .eq(MenuPermission::getDeleted, false));
        if (childCount > 0) {
            throw new BizException(409, "存在子菜单，不能删除");
        }
        Integer bindCount = jdbcTemplate.queryForObject(
                "select count(*) from role_permission where tenant_id = ? and permission_id = ?",
                Integer.class,
                tenantId,
                id
        );
        if (bindCount != null && bindCount > 0) {
            throw new BizException(409, "权限已被角色使用，不能删除");
        }
        int updated = menuPermissionMapper.update(null, new LambdaUpdateWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getId, id)
                .eq(MenuPermission::getDeleted, false)
                .set(MenuPermission::getDeleted, true));
        if (updated == 0) {
            throw new BizException(404, "菜单不存在");
        }
        evictTenantAuthCache(tenantId);
        recordOperation("SYSTEM", "DELETE_MENU", "MENU_PERMISSION", id, null);
        return ApiResult.ok();
    }

    @GetMapping("/operation-logs")
    @RequirePermission("system:operation-log:view")
    public ApiResult<PageResult<Map<String, Object>>> operationLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String requestId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getTenantId, tenantId)
                .orderByDesc(OperationLog::getCreatedAt);
        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (action != null && !action.isBlank()) {
            wrapper.like(OperationLog::getAction, action);
        }
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (requestId != null && !requestId.isBlank()) {
            wrapper.like(OperationLog::getRequestId, requestId);
        }
        Page<OperationLog> logPage = operationLogMapper.selectPage(new Page<>(normalizePage(page), normalizePageSize(pageSize)), wrapper);
        List<OperationLog> pageLogs = logPage.getRecords();
        return ApiResult.ok(PageResult.of(pageLogs.stream()
                .map(this::toOperationLogPayload)
                .toList(), logPage.getTotal(), logPage.getCurrent(), logPage.getSize(), Map.of()));
    }

    @GetMapping("/nav")
    public ApiResult<Map<String, Object>> nav() {
        Long tenantId = SecurityContext.tenantId();
        Long accountId = SecurityContext.accountId();
        String cacheKey = "nav:" + tenantId + ":" + accountId;
        Map<String, Object> navigation = redisSupportService.getOrLoadJson(cacheKey, Duration.ofMinutes(5), Map.class, () -> buildNavigationPayload(tenantId));
        return ApiResult.ok(navigation);
    }

    private Map<String, Object> buildNavigationPayload(Long tenantId) {
        LoginPrincipal principal = SecurityContext.current()
                .orElseThrow(() -> new IllegalStateException("No login principal in current request"));
        List<MenuPermission> menus = menuPermissionMapper.selectList(new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getDeleted, false)
                .eq(MenuPermission::getType, "MENU")
                .orderByAsc(MenuPermission::getSortNo));
        List<Long> accountRoleIds = accountRoleIds(tenantId, principal.accountId());
        List<Long> permissionIds = accountRoleIds.isEmpty()
                ? List.of()
                : jdbcTemplate.queryForList(
                        """
                        select distinct rp.permission_id
                        from role_permission rp
                        where rp.tenant_id = ? and rp.role_id in (
                            select ar.role_id from account_role ar where ar.tenant_id = ? and ar.account_id = ?
                        )
                        """,
                        Long.class,
                        tenantId,
                        tenantId,
                        principal.accountId()
                );
        Map<Long, MenuNode> nodeMap = new LinkedHashMap<>();
        for (MenuPermission menu : menus) {
            if (!isMenuVisible(menu, principal.permissions())) {
                continue;
            }
            if (!isMenuLicensed(menu.getRoutePath())) {
                continue;
            }
            nodeMap.put(menu.getId(), new MenuNode(
                    menu.getId(),
                    menu.getParentId() == null ? 0L : menu.getParentId(),
                    menu.getName(),
                    menu.getRoutePath() == null ? "" : menu.getRoutePath(),
                    menu.getPermissionCode() == null ? "" : menu.getPermissionCode(),
                    menu.getSortNo(),
                    new ArrayList<>()
            ));
        }
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode node : nodeMap.values()) {
            if (node.parentId() == null || node.parentId() == 0L || !nodeMap.containsKey(node.parentId())) {
                roots.add(node);
                continue;
            }
            nodeMap.get(node.parentId()).children().add(node);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user", Map.of(
                "accountId", principal.accountId(),
                "userId", principal.userId(),
                "tenantId", principal.tenantId(),
                "roles", principal.roles(),
                "permissions", principal.permissions(),
                "campusIds", principal.campusIds()
        ));
        payload.put("menus", roots);
        payload.put("permissionIds", permissionIds);
        return payload;
    }

    private Map<String, Object> toOperationLogPayload(OperationLog log) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", log.getId());
        payload.put("username", log.getUsername() == null ? "" : log.getUsername());
        payload.put("module", log.getModule());
        payload.put("action", log.getAction());
        payload.put("targetType", log.getTargetType() == null ? "" : log.getTargetType());
        payload.put("targetId", log.getTargetId() == null ? "" : log.getTargetId());
        payload.put("success", log.getSuccess());
        payload.put("requestId", log.getRequestId() == null ? "" : log.getRequestId());
        payload.put("detail", log.getDetail() == null ? "" : log.getDetail());
        payload.put("createdAt", log.getCreatedAt());
        return payload;
    }

    /** 前端菜单路由 → 授权功能码映射；未列出的路由视为核心模块，始终放行。 */
    private static final Map<String, String> MENU_FEATURE_BY_ROUTE = Map.ofEntries(
            Map.entry("/attendance", "attendance"),
            Map.entry("/classrooms", "classroom"),
            Map.entry("/scheduling", "scheduling"),
            Map.entry("/consumption", "consumption"),
            Map.entry("/transfers", "transfer"),
            Map.entry("/teachers", "teacher"),
            Map.entry("/coupons", "marketing"),
            Map.entry("/marketing", "marketing"),
            Map.entry("/contracts", "contract"),
            Map.entry("/notifications", "notification"),
            Map.entry("/reports", "report")
    );

    /** 该菜单是否在当前授权范围内（用于按授权隐藏侧边栏）。 */
    private boolean isMenuLicensed(String routePath) {
        if (routePath == null || routePath.isBlank()) {
            return true;
        }
        String feature = MENU_FEATURE_BY_ROUTE.get(routePath);
        return feature == null || licenseService.isFeatureEnabled(feature);
    }

    private boolean isMenuVisible(MenuPermission menu, java.util.Set<String> permissions) {
        if (menu.getPermissionCode() == null || menu.getPermissionCode().isBlank()) {
            return true;
        }
        return permissions != null && permissions.contains(menu.getPermissionCode());
    }

    public record MenuNode(
            Long id,
            Long parentId,
            String name,
            String path,
            String permissionCode,
            Integer sortNo,
            List<MenuNode> children
    ) {
    }

    private void assignAccountRoles(Long tenantId, Long accountId, List<Long> roleIds) {
        jdbcTemplate.update("delete from account_role where tenant_id = ? and account_id = ?", tenantId, accountId);
        List<Long> safeRoleIds = roleIds == null ? List.of() : roleIds;
        for (Long roleId : safeRoleIds.stream().filter(Objects::nonNull).distinct().toList()) {
            jdbcTemplate.update(
                    "insert into account_role (id, tenant_id, account_id, role_id) values (?, ?, ?, ?)",
                    System.nanoTime(),
                    tenantId,
                    accountId,
                    roleId
            );
        }
    }

    private List<Long> accountRoleIds(Long tenantId, Long accountId) {
        return jdbcTemplate.queryForList(
                "select role_id from account_role where tenant_id = ? and account_id = ?",
                Long.class,
                tenantId,
                accountId
        );
    }

    private void assignAccountCampuses(Long tenantId, Long accountId, List<Long> campusIds) {
        jdbcTemplate.update("delete from account_campus where tenant_id = ? and account_id = ?", tenantId, accountId);
        List<Long> safeCampusIds = campusIds == null ? List.of() : campusIds.stream().filter(Objects::nonNull).distinct().toList();
        ensureCampusesBelongTenant(tenantId, safeCampusIds);
        for (Long campusId : safeCampusIds) {
            jdbcTemplate.update(
                    "insert into account_campus (id, tenant_id, account_id, campus_id) values (?, ?, ?, ?)",
                    System.nanoTime(),
                    tenantId,
                    accountId,
                    campusId
            );
        }
    }

    private List<Long> accountCampusIds(Long tenantId, Long accountId) {
        return jdbcTemplate.queryForList(
                "select campus_id from account_campus where tenant_id = ? and account_id = ?",
                Long.class,
                tenantId,
                accountId
        );
    }

    private void ensureCampusesBelongTenant(Long tenantId, List<Long> campusIds) {
        if (campusIds == null || campusIds.isEmpty()) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from campus where tenant_id = ? and deleted = 0 and id in (" + placeholders(campusIds.size()) + ")",
                Integer.class,
                buildParams(tenantId, campusIds)
        );
        if (count == null || count != campusIds.size()) {
            throw new BizException(400, "存在无效校区");
        }
    }

    private Object[] buildParams(Long tenantId, List<Long> ids) {
        Object[] params = new Object[ids.size() + 1];
        params[0] = tenantId;
        for (int i = 0; i < ids.size(); i++) {
            params[i + 1] = ids.get(i);
        }
        return params;
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private void assignRolePermissions(Long tenantId, Long roleId, List<Long> permissionIds) {
        jdbcTemplate.update("delete from role_permission where tenant_id = ? and role_id = ?", tenantId, roleId);
        for (Long permissionId : permissionIds.stream().filter(Objects::nonNull).distinct().toList()) {
            jdbcTemplate.update(
                    "insert into role_permission (id, tenant_id, role_id, permission_id) values (?, ?, ?, ?)",
                    System.nanoTime(),
                    tenantId,
                    roleId,
                    permissionId
            );
        }
    }

    private void ensurePermissionCodeAvailable(Long tenantId, Long currentId, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return;
        }
        LambdaQueryWrapper<MenuPermission> wrapper = new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getPermissionCode, permissionCode)
                .eq(MenuPermission::getDeleted, false);
        if (currentId != null) {
            wrapper.ne(MenuPermission::getId, currentId);
        }
        Long count = menuPermissionMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BizException(409, "权限码已存在");
        }
    }

    private long normalizePage(long page) {
        return Math.max(1, page);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long normalizePageSize(long pageSize) {
        return Math.min(Math.max(1, pageSize), 100);
    }

    private <T> List<T> pageSlice(List<T> source, long page, long pageSize) {
        long normalizedPage = normalizePage(page);
        long normalizedPageSize = normalizePageSize(pageSize);
        long fromIndex = Math.max(0, (normalizedPage - 1) * normalizedPageSize);
        if (fromIndex >= source.size()) {
            return List.of();
        }
        long toIndex = Math.min(source.size(), fromIndex + normalizedPageSize);
        return source.subList((int) fromIndex, (int) toIndex);
    }

    private Map<String, Object> buildAccountSummary(Long tenantId, String keyword, String status) {
        long activeCount = accountMapper.selectCount(accountSummaryWrapper(tenantId, keyword, status, "ACTIVE"));
        long disabledCount = accountMapper.selectCount(accountSummaryWrapper(tenantId, keyword, status, "DISABLED"));
        return Map.of(
                "activeCount", activeCount,
                "disabledCount", disabledCount
        );
    }

    private Map<String, Object> buildRoleSummary(Long tenantId, String keyword, String dataScope) {
        return Map.of(
                "builtinCount", roleMapper.selectCount(roleSummaryWrapper(tenantId, keyword, dataScope, true)),
                "customCount", roleMapper.selectCount(roleSummaryWrapper(tenantId, keyword, dataScope, false))
        );
    }

    private Map<String, Object> buildMenuSummary(Long tenantId, String keyword, String type) {
        return Map.of(
                "menuCount", menuPermissionMapper.selectCount(menuSummaryWrapper(tenantId, keyword, type, "MENU")),
                "buttonCount", menuPermissionMapper.selectCount(menuSummaryWrapper(tenantId, keyword, type, "BUTTON"))
        );
    }

    private LambdaQueryWrapper<Account> accountSummaryWrapper(Long tenantId, String keyword, String status, String summaryStatus) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getTenantId, tenantId)
                .eq(Account::getDeleted, false);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(Account::getUsername, keyword)
                    .or()
                    .like(Account::getPhone, keyword)
                    .or()
                    .like(Account::getEmail, keyword));
        }
        if (hasText(status)) {
            wrapper.eq(Account::getStatus, status);
        }
        return wrapper.eq(Account::getStatus, summaryStatus);
    }

    private LambdaQueryWrapper<Role> roleSummaryWrapper(Long tenantId, String keyword, String dataScope, boolean systemBuiltin) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getDeleted, false);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(Role::getName, keyword)
                    .or()
                    .like(Role::getCode, keyword));
        }
        if (hasText(dataScope)) {
            wrapper.eq(Role::getDataScope, dataScope);
        }
        return wrapper.eq(Role::getSystemBuiltin, systemBuiltin);
    }

    private LambdaQueryWrapper<MenuPermission> menuSummaryWrapper(Long tenantId, String keyword, String type, String summaryType) {
        LambdaQueryWrapper<MenuPermission> wrapper = new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, tenantId)
                .eq(MenuPermission::getDeleted, false);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(MenuPermission::getName, keyword)
                    .or()
                    .like(MenuPermission::getRoutePath, keyword)
                    .or()
                    .like(MenuPermission::getPermissionCode, keyword));
        }
        if (hasText(type)) {
            wrapper.eq(MenuPermission::getType, type);
        }
        return wrapper.eq(MenuPermission::getType, summaryType);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }

    private void recordOperation(String module, String action, String targetType, Long targetId, String detail) {
        Long accountId = SecurityContext.accountId();
        eventPublisher.publishEvent(new OperationLogEvent(
                SecurityContext.tenantId(),
                accountId,
                resolveUsername(accountId),
                module,
                action,
                targetType,
                targetId,
                true,
                RequestTraceContext.requestIdOrNull(),
                detail
        ));
    }

    private void evictAccountAuthCache(Long tenantId, Long accountId) {
        redisSupportService.evict("auth:profile:" + tenantId + ":" + accountId);
        redisSupportService.evict("nav:" + tenantId + ":" + accountId);
    }

    private void requireStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BizException(400, "密码至少需要 8 位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BizException(400, "密码必须同时包含字母和数字");
        }
    }

    private void evictTenantAuthCache(Long tenantId) {
        redisSupportService.evictByPrefix("auth:profile:" + tenantId + ":");
        redisSupportService.evictByPrefix("nav:" + tenantId + ":");
    }

    private String resolveUsername(Long accountId) {
        if (accountId == null) {
            return "";
        }
        Account account = accountMapper.selectById(accountId);
        return account == null ? "" : account.getUsername();
    }

    public record AccountRequest(
            @NotBlank String username,
            @NotBlank String realName,
            String phone,
            String email,
            @NotBlank String password,
            @NotBlank String status,
            List<Long> roleIds,
            List<Long> campusIds
    ) {
    }

    public record AccountUpdateRequest(
            @NotBlank String realName,
            String phone,
            String email,
            @NotNull List<Long> roleIds,
            List<Long> campusIds
    ) {
    }

    public record PasswordResetRequest(@NotBlank String password) {
    }

    public record StatusRequest(@NotBlank String status) {
    }

    public record RoleRequest(
            @NotBlank String name,
            @NotBlank String code,
            @NotNull DataScope dataScope,
            List<Long> permissionIds
    ) {
    }

    public record RoleUpdateRequest(
            @NotBlank String name,
            @NotNull DataScope dataScope
    ) {
    }

    public record RolePermissionRequest(@NotNull List<Long> permissionIds) {
    }

    public record MenuRequest(
            Long parentId,
            @NotBlank String name,
            @NotBlank String type,
            String routePath,
            String permissionCode,
            @NotNull Integer sortNo
    ) {
    }
}
