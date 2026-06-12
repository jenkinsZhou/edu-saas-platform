package com.edusphere.api.service.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.edusphere.api.service.security.PasswordService;
import com.edusphere.common.exception.BizException;
import com.edusphere.system.domain.Account;
import com.edusphere.system.domain.MenuPermission;
import com.edusphere.system.domain.Role;
import com.edusphere.system.domain.SysUser;
import com.edusphere.system.mapper.AccountMapper;
import com.edusphere.system.mapper.MenuPermissionMapper;
import com.edusphere.system.mapper.RoleMapper;
import com.edusphere.system.mapper.SysUserMapper;
import com.edusphere.tenant.domain.Tenant;
import com.edusphere.tenant.domain.TenantTheme;
import com.edusphere.tenant.mapper.TenantMapper;
import com.edusphere.tenant.mapper.TenantThemeMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 租户自助开通：创建租户 + 默认主题 + 权限目录 + 内置角色 + 管理员账号。
 * 权限目录从模板租户（默认租户1，由 seed.sql 初始化）克隆，保证新租户与演示租户能力一致。
 */
@Service
public class TenantProvisioningService {

    private static final long TEMPLATE_TENANT_ID = 1L;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{1,31}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{2,31}$");
    private static final Set<String> ALLOWED_PLANS = Set.of("basic", "standard", "premium");

    private final TenantMapper tenantMapper;
    private final TenantThemeMapper tenantThemeMapper;
    private final MenuPermissionMapper menuPermissionMapper;
    private final RoleMapper roleMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordService passwordService;
    private final JdbcTemplate jdbcTemplate;
    private final IdentifierGenerator identifierGenerator;

    public TenantProvisioningService(TenantMapper tenantMapper,
                                     TenantThemeMapper tenantThemeMapper,
                                     MenuPermissionMapper menuPermissionMapper,
                                     RoleMapper roleMapper,
                                     AccountMapper accountMapper,
                                     SysUserMapper sysUserMapper,
                                     PasswordService passwordService,
                                     JdbcTemplate jdbcTemplate,
                                     IdentifierGenerator identifierGenerator) {
        this.tenantMapper = tenantMapper;
        this.tenantThemeMapper = tenantThemeMapper;
        this.menuPermissionMapper = menuPermissionMapper;
        this.roleMapper = roleMapper;
        this.accountMapper = accountMapper;
        this.sysUserMapper = sysUserMapper;
        this.passwordService = passwordService;
        this.jdbcTemplate = jdbcTemplate;
        this.identifierGenerator = identifierGenerator;
    }

    public record RegistrationRequest(
            String organizationName,
            String organizationCode,
            String adminUsername,
            String adminPassword,
            String adminPhone,
            String adminEmail,
            String planCode
    ) {
    }

    public record RegistrationResult(Long tenantId, String organizationCode, String adminUsername) {
    }

    @Transactional
    public RegistrationResult register(RegistrationRequest request) {
        String name = trimmed(request.organizationName());
        String code = trimmed(request.organizationCode()).toLowerCase();
        String username = trimmed(request.adminUsername());
        String password = request.adminPassword() == null ? "" : request.adminPassword();
        String planCode = trimmed(request.planCode()).toLowerCase();

        validate(name, code, username, password, planCode);

        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setCode(code);
        tenant.setStatus("ACTIVE");
        tenant.setPlanCode(planCode);
        try {
            tenantMapper.insert(tenant);
        } catch (DuplicateKeyException e) {
            throw new BizException(409, "机构编码已被使用，请更换");
        }
        Long tenantId = tenant.getId();

        createDefaultTheme(tenantId);
        Map<Long, Long> permissionIdMapping = clonePermissionCatalog(tenantId);
        Long adminRoleId = createBuiltinRoles(tenantId);
        grantAllPermissions(tenantId, adminRoleId, permissionIdMapping.values());
        Long accountId = createAdminAccount(tenantId, username, password, request.adminPhone(), request.adminEmail());
        bindAdminRole(tenantId, accountId, adminRoleId);

        return new RegistrationResult(tenantId, code, username);
    }

    private void validate(String name, String code, String username, String password, String planCode) {
        if (name.length() < 2 || name.length() > 50) {
            throw new BizException(400, "机构名称长度需在2-50个字符之间");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BizException(400, "机构编码需以小写字母开头，仅含小写字母、数字、连字符，长度2-32");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BizException(400, "管理员账号需以字母开头，仅含字母、数字、下划线，长度3-32");
        }
        if (password.length() < 8 || password.length() > 64) {
            throw new BizException(400, "密码长度需在8-64个字符之间");
        }
        if (!ALLOWED_PLANS.contains(planCode)) {
            throw new BizException(400, "无效的套餐类型");
        }
        Long existing = jdbcTemplate.queryForObject(
                "select count(*) from tenant where code = ? and deleted = 0", Long.class, code);
        if (existing != null && existing > 0) {
            throw new BizException(409, "机构编码已被使用，请更换");
        }
    }

    private void createDefaultTheme(Long tenantId) {
        TenantTheme theme = new TenantTheme();
        theme.setTenantId(tenantId);
        theme.setName("默认专业主题");
        theme.setPrimaryColor("#2563eb");
        theme.setAccentColor("#16a34a");
        theme.setLayout("side");
        theme.setCustomCssVarsJson(
                "{\"surfaceColor\":\"#ffffff\",\"sidebarColor\":\"#111827\",\"sidebarTextColor\":\"#e5e7eb\"}");
        tenantThemeMapper.insert(theme);
    }

    /**
     * 克隆模板租户的菜单/权限目录，返回 模板权限id -> 新权限id 的映射
     */
    private Map<Long, Long> clonePermissionCatalog(Long tenantId) {
        List<MenuPermission> templates = menuPermissionMapper.selectList(new LambdaQueryWrapper<MenuPermission>()
                .eq(MenuPermission::getTenantId, TEMPLATE_TENANT_ID)
                .eq(MenuPermission::getDeleted, false)
                .orderByAsc(MenuPermission::getSortNo));
        if (templates.isEmpty()) {
            throw new BizException(500, "权限模板缺失，无法开通租户");
        }

        Map<Long, Long> idMapping = new HashMap<>();
        for (MenuPermission template : templates) {
            MenuPermission copy = new MenuPermission();
            copy.setTenantId(tenantId);
            copy.setName(template.getName());
            copy.setType(template.getType());
            copy.setRoutePath(template.getRoutePath());
            copy.setPermissionCode(template.getPermissionCode());
            copy.setSortNo(template.getSortNo());
            menuPermissionMapper.insert(copy);
            idMapping.put(template.getId(), copy.getId());
        }
        // 第二遍修正父子关系（模板按 sortNo 排序，父节点可能晚于子节点插入）
        for (MenuPermission template : templates) {
            if (template.getParentId() != null) {
                Long newId = idMapping.get(template.getId());
                Long newParentId = idMapping.get(template.getParentId());
                if (newParentId != null) {
                    MenuPermission update = new MenuPermission();
                    update.setId(newId);
                    update.setParentId(newParentId);
                    menuPermissionMapper.updateById(update);
                }
            }
        }
        return idMapping;
    }

    /**
     * 创建内置角色，返回管理员角色id
     */
    private Long createBuiltinRoles(Long tenantId) {
        Long adminRoleId = insertRole(tenantId, "总部管理员", "TENANT_ADMIN", "ALL");
        insertRole(tenantId, "校区校长", "CAMPUS_PRINCIPAL", "CAMPUS");
        insertRole(tenantId, "教务老师", "ACADEMIC_ADMIN", "CUSTOM");
        insertRole(tenantId, "任课老师", "TEACHER", "OWNER");
        return adminRoleId;
    }

    private Long insertRole(Long tenantId, String name, String code, String dataScope) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(name);
        role.setCode(code);
        role.setDataScope(com.edusphere.security.context.DataScope.valueOf(dataScope));
        role.setSystemBuiltin(true);
        roleMapper.insert(role);
        return role.getId();
    }

    private void grantAllPermissions(Long tenantId, Long roleId, Iterable<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            jdbcTemplate.update(
                    "insert into role_permission (id, tenant_id, role_id, permission_id) values (?, ?, ?, ?)",
                    nextId(), tenantId, roleId, permissionId);
        }
    }

    private Long createAdminAccount(Long tenantId, String username, String password, String phone, String email) {
        Account account = new Account();
        account.setTenantId(tenantId);
        account.setUsername(username);
        account.setPhone(trimmedOrNull(phone));
        account.setEmail(trimmedOrNull(email));
        account.setPasswordHash(passwordService.encode(password));
        account.setStatus("ACTIVE");
        accountMapper.insert(account);

        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setAccountId(account.getId());
        user.setRealName("机构管理员");
        user.setGender("UNKNOWN");
        sysUserMapper.insert(user);

        return account.getId();
    }

    private void bindAdminRole(Long tenantId, Long accountId, Long roleId) {
        jdbcTemplate.update(
                "insert into account_role (id, tenant_id, account_id, role_id) values (?, ?, ?, ?)",
                nextId(), tenantId, accountId, roleId);
    }

    private long nextId() {
        return identifierGenerator.nextId(null).longValue();
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimmedOrNull(String value) {
        String result = trimmed(value);
        return result.isEmpty() ? null : result;
    }
}
