-- 演示数据初始化脚本
-- 重要：必须用 utf8mb4 字符集导入，否则中文会乱码：
--   mysql -uroot --default-character-set=utf8mb4 < seed.sql
SET NAMES utf8mb4;

USE edu_saas;

INSERT INTO tenant (id, name, code, status, plan_code)
VALUES (1, '演示培训机构', 'demo', 'ACTIVE', 'standard')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    status = VALUES(status),
    plan_code = VALUES(plan_code);

INSERT INTO tenant_theme (id, tenant_id, name, primary_color, accent_color, logo_url, layout, custom_css_vars_json, created_by)
VALUES (101, 1, '默认专业主题', '#2563eb', '#16a34a', NULL, 'side', JSON_OBJECT('surfaceColor', '#ffffff', 'sidebarColor', '#111827', 'sidebarTextColor', '#e5e7eb'), 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    primary_color = VALUES(primary_color),
    accent_color = VALUES(accent_color),
    layout = VALUES(layout),
    custom_css_vars_json = VALUES(custom_css_vars_json);

INSERT INTO account (id, tenant_id, username, phone, email, password_hash, status, created_by)
VALUES (1001, 1, 'admin', '13800000000', 'admin@example.com', '$2a$12$ef6ACx7XxmR4t7HuJheqBOF//GPyie/fPrJUEHIcvqvUqC2aQXpxS', 'ACTIVE', 1001)
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    status = VALUES(status);

INSERT INTO sys_user (id, tenant_id, account_id, real_name, avatar_url, gender, created_by)
VALUES (2001, 1, 1001, '系统管理员', NULL, 'UNKNOWN', 1001)
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    avatar_url = VALUES(avatar_url),
    gender = VALUES(gender);

INSERT INTO role (id, tenant_id, name, code, data_scope, system_builtin, created_by)
VALUES
    (3001, 1, '总部管理员', 'TENANT_ADMIN', 'ALL', 1, 1001),
    (3002, 1, '校区校长', 'CAMPUS_PRINCIPAL', 'CAMPUS', 1, 1001),
    (3003, 1, '教务老师', 'ACADEMIC_ADMIN', 'CUSTOM', 1, 1001),
    (3004, 1, '任课老师', 'TEACHER', 'OWNER', 1, 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    data_scope = VALUES(data_scope),
    system_builtin = VALUES(system_builtin);

INSERT INTO menu_permission (id, tenant_id, parent_id, name, type, route_path, permission_code, sort_no, created_by)
VALUES
    (4001, 1, NULL, '运营总览', 'MENU', '/', 'dashboard:view', 10, 1001),
    (4002, 1, NULL, '课程中心', 'MENU', '/courses', 'course:product:view', 20, 1001),
    (4003, 1, NULL, '账号权限', 'MENU', '/security', 'system:role:view', 30, 1001),
    (4004, 1, NULL, '机构主题', 'MENU', '/tenant-theme', 'tenant:theme:view', 40, 1001),
    (4005, 1, NULL, '订单中心', 'MENU', '/orders', 'order:order:view', 50, 1001),
    (4101, 1, 4002, '新增课程', 'BUTTON', NULL, 'course:product:create', 21, 1001),
    (4102, 1, 4002, '编辑课程', 'BUTTON', NULL, 'course:product:update', 22, 1001),
    (4103, 1, 4002, '下架课程', 'BUTTON', NULL, 'course:product:disable', 23, 1001),
    (4111, 1, 4002, '班级查看', 'BUTTON', NULL, 'course:class:view', 24, 1001),
    (4112, 1, 4002, '班级新增', 'BUTTON', NULL, 'course:class:create', 25, 1001),
    (4113, 1, 4002, '班级编辑', 'BUTTON', NULL, 'course:class:update', 26, 1001),
    (4121, 1, 4002, '课次查看', 'BUTTON', NULL, 'course:lesson:view', 27, 1001),
    (4122, 1, 4002, '课次新增', 'BUTTON', NULL, 'course:lesson:create', 28, 1001),
    (4123, 1, 4002, '课次编辑', 'BUTTON', NULL, 'course:lesson:update', 29, 1001),
    (4131, 1, 4002, '学员查看', 'BUTTON', NULL, 'course:student:view', 30, 1001),
    (4132, 1, 4002, '学员新增', 'BUTTON', NULL, 'course:student:create', 31, 1001),
    (4133, 1, 4002, '学员编辑', 'BUTTON', NULL, 'course:student:update', 32, 1001),
    (4141, 1, 4002, '考勤查看', 'BUTTON', NULL, 'course:attendance:view', 33, 1001),
    (4142, 1, 4002, '考勤保存', 'BUTTON', NULL, 'course:attendance:update', 34, 1001),
    (4251, 1, 4005, '订单查看', 'BUTTON', NULL, 'order:order:view', 51, 1001),
    (4252, 1, 4005, '订单新增', 'BUTTON', NULL, 'order:order:create', 52, 1001),
    (4253, 1, 4005, '订单确认', 'BUTTON', NULL, 'order:order:confirm', 53, 1001),
    (4254, 1, 4005, '订单取消', 'BUTTON', NULL, 'order:order:cancel', 54, 1001),
    (4255, 1, 4005, '收款查看', 'BUTTON', NULL, 'order:payment:view', 55, 1001),
    (4256, 1, 4005, '收款登记', 'BUTTON', NULL, 'order:payment:create', 56, 1001),
    (4257, 1, 4005, '退款查看', 'BUTTON', NULL, 'order:refund:view', 57, 1001),
    (4258, 1, 4005, '退款登记', 'BUTTON', NULL, 'order:refund:create', 58, 1001),
    (4201, 1, 4003, '账号查看', 'BUTTON', NULL, 'system:account:view', 31, 1001),
    (4202, 1, 4003, '账号新增', 'BUTTON', NULL, 'system:account:create', 32, 1001),
    (4203, 1, 4003, '账号编辑', 'BUTTON', NULL, 'system:account:update', 33, 1001),
    (4211, 1, 4003, '角色查看', 'BUTTON', NULL, 'system:role:view', 34, 1001),
    (4212, 1, 4003, '角色新增', 'BUTTON', NULL, 'system:role:create', 35, 1001),
    (4213, 1, 4003, '角色授权', 'BUTTON', NULL, 'system:role:assign', 36, 1001),
    (4221, 1, 4003, '菜单查看', 'BUTTON', NULL, 'system:menu:view', 37, 1001),
    (4222, 1, 4003, '菜单新增', 'BUTTON', NULL, 'system:menu:create', 38, 1001),
    (4223, 1, 4003, '菜单编辑', 'BUTTON', NULL, 'system:menu:update', 39, 1001),
    (4224, 1, 4003, '菜单删除', 'BUTTON', NULL, 'system:menu:delete', 40, 1001),
    (4231, 1, 4003, '操作日志查看', 'BUTTON', NULL, 'system:operation-log:view', 41, 1001)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    type = VALUES(type),
    route_path = VALUES(route_path),
    permission_code = VALUES(permission_code),
    sort_no = VALUES(sort_no);

INSERT INTO account_role (id, tenant_id, account_id, role_id)
VALUES (5001, 1, 1001, 3001)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

DELETE FROM role_permission WHERE tenant_id = 1 AND role_id = 3001;

INSERT INTO role_permission (id, tenant_id, role_id, permission_id)
VALUES
    (6001, 1, 3001, 4001),
    (6002, 1, 3001, 4002),
    (6003, 1, 3001, 4003),
    (6004, 1, 3001, 4004),
    (6005, 1, 3001, 4101),
    (6006, 1, 3001, 4102),
    (6007, 1, 3001, 4103),
    (6018, 1, 3001, 4111),
    (6019, 1, 3001, 4112),
    (6020, 1, 3001, 4113),
    (6021, 1, 3001, 4121),
    (6022, 1, 3001, 4122),
    (6023, 1, 3001, 4123),
    (6024, 1, 3001, 4131),
    (6025, 1, 3001, 4132),
    (6026, 1, 3001, 4133),
    (6027, 1, 3001, 4141),
    (6028, 1, 3001, 4142),
    (6040, 1, 3001, 4005),
    (6041, 1, 3001, 4251),
    (6042, 1, 3001, 4252),
    (6043, 1, 3001, 4253),
    (6044, 1, 3001, 4254),
    (6045, 1, 3001, 4255),
    (6046, 1, 3001, 4256),
    (6047, 1, 3001, 4257),
    (6048, 1, 3001, 4258),
    (6029, 1, 3001, 4201),
    (6030, 1, 3001, 4202),
    (6031, 1, 3001, 4203),
    (6032, 1, 3001, 4211),
    (6033, 1, 3001, 4212),
    (6034, 1, 3001, 4213),
    (6035, 1, 3001, 4221),
    (6036, 1, 3001, 4222),
    (6037, 1, 3001, 4223),
    (6038, 1, 3001, 4224),
    (6039, 1, 3001, 4231)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO campus (id, tenant_id, name, code, address, status, created_by)
VALUES
    (7001, 1, '总部校区', 'HQ', '演示城市中心路 1 号', 'ACTIVE', 1001),
    (7002, 1, '线上校区', 'ONLINE', '线上教学中心', 'ACTIVE', 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    status = VALUES(status);

INSERT INTO account_campus (id, tenant_id, account_id, campus_id)
VALUES
    (5101, 1, 1001, 7001),
    (5102, 1, 1001, 7002)
ON DUPLICATE KEY UPDATE campus_id = VALUES(campus_id);

INSERT INTO course_product (id, tenant_id, name, category_code, delivery_mode, billing_mode, total_lessons, list_price, extension_template_code, status, created_by)
VALUES
    (8001, 1, '初中数学春季班', 'ACADEMIC', 'OFFLINE', 'LESSON', 60, 6800.00, 'academic_course', 'ON_SALE', 1001),
    (8002, 1, '少儿美术启蒙', 'ART', 'OFFLINE', 'TERM', 36, 4200.00, 'art_course', 'ON_SALE', 1001),
    (8003, 1, '英语直播强化课', 'ONLINE', 'LIVE', 'PACKAGE', 24, 2999.00, 'online_course', 'ON_SALE', 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category_code = VALUES(category_code),
    delivery_mode = VALUES(delivery_mode),
    billing_mode = VALUES(billing_mode),
    total_lessons = VALUES(total_lessons),
    list_price = VALUES(list_price),
    extension_template_code = VALUES(extension_template_code),
    status = VALUES(status);

INSERT INTO class_group (id, tenant_id, course_product_id, campus_id, name, head_teacher_id, capacity, start_date, end_date, status, created_by)
VALUES
    (9001, 1, 8001, 7001, '初中数学春季 A 班', 2001, 24, '2026-03-01', '2026-06-30', 'OPEN', 1001),
    (9002, 1, 8002, 7001, '少儿美术周末班', 2001, 18, '2026-03-08', '2026-06-29', 'OPEN', 1001),
    (9003, 1, 8003, 7002, '英语直播晚间班', 2001, 80, '2026-03-10', '2026-05-30', 'OPEN', 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    capacity = VALUES(capacity),
    status = VALUES(status);

INSERT INTO student (id, tenant_id, name, phone, guardian_name, guardian_phone, source, status, created_by)
VALUES
    (10001, 1, '张小明', '13900000001', '张女士', '13910000001', '转介绍', 'ACTIVE', 1001),
    (10002, 1, '李可欣', '13900000002', '李先生', '13910000002', '试听', 'ACTIVE', 1001),
    (10003, 1, '王一凡', '13900000003', '王女士', '13910000003', '线上咨询', 'ACTIVE', 1001)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    phone = VALUES(phone),
    guardian_name = VALUES(guardian_name),
    guardian_phone = VALUES(guardian_phone),
    source = VALUES(source),
    status = VALUES(status);

INSERT INTO class_enrollment (id, tenant_id, class_group_id, student_id, enroll_status, enroll_date, remark, created_by)
VALUES
    (11001, 1, 9001, 10001, 'ACTIVE', '2026-03-01', '演示报名', 1001),
    (11002, 1, 9001, 10002, 'ACTIVE', '2026-03-01', '演示报名', 1001)
ON DUPLICATE KEY UPDATE
    enroll_status = VALUES(enroll_status),
    enroll_date = VALUES(enroll_date),
    remark = VALUES(remark);

-- 教师/教室/排课/考勤/课消/转班/通知/合同/优惠券/报表 模块权限
INSERT INTO menu_permission (id, tenant_id, parent_id, name, type, route_path, permission_code, sort_no, created_by)
VALUES
    (4301, 1, 4002, '考勤打卡', 'BUTTON', NULL, 'course:attendance:create', 35, 1001),
    (4302, 1, 4002, '教室查看', 'BUTTON', NULL, 'course:classroom:view', 36, 1001),
    (4303, 1, 4002, '教室新增', 'BUTTON', NULL, 'course:classroom:create', 37, 1001),
    (4304, 1, 4002, '教室编辑', 'BUTTON', NULL, 'course:classroom:update', 38, 1001),
    (4305, 1, 4002, '课消查看', 'BUTTON', NULL, 'course:consumption:view', 39, 1001),
    (4306, 1, 4002, '排课查看', 'BUTTON', NULL, 'course:schedule:view', 40, 1001),
    (4307, 1, 4002, '排课新增', 'BUTTON', NULL, 'course:schedule:create', 41, 1001),
    (4308, 1, 4002, '教师查看', 'BUTTON', NULL, 'course:teacher:view', 42, 1001),
    (4309, 1, 4002, '教师新增', 'BUTTON', NULL, 'course:teacher:create', 43, 1001),
    (4310, 1, 4002, '教师编辑', 'BUTTON', NULL, 'course:teacher:update', 44, 1001),
    (4311, 1, 4002, '转班查看', 'BUTTON', NULL, 'course:transfer:view', 45, 1001),
    (4312, 1, 4002, '转班申请', 'BUTTON', NULL, 'course:transfer:create', 46, 1001),
    (4313, 1, 4002, '转班审批', 'BUTTON', NULL, 'course:transfer:approve', 47, 1001),
    (4321, 1, 4005, '优惠券查看', 'BUTTON', NULL, 'marketing:coupon:view', 59, 1001),
    (4322, 1, 4005, '优惠券新增', 'BUTTON', NULL, 'marketing:coupon:create', 60, 1001),
    (4323, 1, 4005, '合同查看', 'BUTTON', NULL, 'order:contract:view', 61, 1001),
    (4324, 1, 4005, '合同新增', 'BUTTON', NULL, 'order:contract:create', 62, 1001),
    (4325, 1, 4005, '合同到期提醒', 'BUTTON', NULL, 'order:contract:notify', 63, 1001),
    (4331, 1, 4001, '营收报表', 'BUTTON', NULL, 'report:revenue:view', 11, 1001),
    (4332, 1, 4001, '学员报表', 'BUTTON', NULL, 'report:student:view', 12, 1001),
    (4333, 1, 4001, '教师报表', 'BUTTON', NULL, 'report:teacher:view', 13, 1001),
    (4334, 1, 4001, '考勤报表', 'BUTTON', NULL, 'report:attendance:view', 14, 1001),
    (4335, 1, 4001, '课消报表', 'BUTTON', NULL, 'report:consumption:view', 15, 1001),
    (4336, 1, 4001, '看板报表', 'BUTTON', NULL, 'report:dashboard:view', 16, 1001),
    (4341, 1, 4003, '通知查看', 'BUTTON', NULL, 'system:notification:view', 42, 1001),
    (4342, 1, 4003, '通知发送', 'BUTTON', NULL, 'system:notification:send', 43, 1001)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    type = VALUES(type),
    permission_code = VALUES(permission_code),
    sort_no = VALUES(sort_no);

INSERT INTO role_permission (id, tenant_id, role_id, permission_id)
VALUES
    (6101, 1, 3001, 4301),
    (6102, 1, 3001, 4302),
    (6103, 1, 3001, 4303),
    (6104, 1, 3001, 4304),
    (6105, 1, 3001, 4305),
    (6106, 1, 3001, 4306),
    (6107, 1, 3001, 4307),
    (6108, 1, 3001, 4308),
    (6109, 1, 3001, 4309),
    (6110, 1, 3001, 4310),
    (6111, 1, 3001, 4311),
    (6112, 1, 3001, 4312),
    (6113, 1, 3001, 4313),
    (6121, 1, 3001, 4321),
    (6122, 1, 3001, 4322),
    (6123, 1, 3001, 4323),
    (6124, 1, 3001, 4324),
    (6125, 1, 3001, 4325),
    (6131, 1, 3001, 4331),
    (6132, 1, 3001, 4332),
    (6133, 1, 3001, 4333),
    (6134, 1, 3001, 4334),
    (6135, 1, 3001, 4335),
    (6136, 1, 3001, 4336),
    (6141, 1, 3001, 4341),
    (6142, 1, 3001, 4342)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

-- 学员管理菜单（前端 /students 页面）
INSERT INTO menu_permission (id, tenant_id, parent_id, name, type, route_path, permission_code, sort_no, created_by)
VALUES (4006, 1, NULL, '学员管理', 'MENU', '/students', 'course:student:view', 25, 1001)
ON DUPLICATE KEY UPDATE name = VALUES(name), route_path = VALUES(route_path),
    type = VALUES(type), permission_code = VALUES(permission_code), sort_no = VALUES(sort_no);

INSERT INTO role_permission (id, tenant_id, role_id, permission_id)
VALUES (6143, 1, 3001, 4006)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
