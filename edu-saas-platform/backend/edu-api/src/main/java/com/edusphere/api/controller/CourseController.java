package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.AttendanceRecord;
import com.edusphere.course.domain.Campus;
import com.edusphere.course.domain.ClassGroup;
import com.edusphere.course.domain.ClassEnrollment;
import com.edusphere.course.domain.CourseProduct;
import com.edusphere.course.domain.LessonSession;
import com.edusphere.course.domain.Student;
import com.edusphere.course.mapper.AttendanceRecordMapper;
import com.edusphere.course.mapper.ClassEnrollmentMapper;
import com.edusphere.course.mapper.CampusMapper;
import com.edusphere.course.mapper.ClassGroupMapper;
import com.edusphere.course.mapper.CourseProductMapper;
import com.edusphere.course.mapper.LessonSessionMapper;
import com.edusphere.course.mapper.StudentMapper;
import com.edusphere.security.context.DataScopeSupport;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseProductMapper courseProductMapper;
    private final CampusMapper campusMapper;
    private final ClassGroupMapper classGroupMapper;
    private final LessonSessionMapper lessonSessionMapper;
    private final StudentMapper studentMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ClassEnrollmentMapper classEnrollmentMapper;
    private final com.edusphere.api.license.LicenseQuotaService licenseQuotaService;

    public CourseController(
            CourseProductMapper courseProductMapper,
            CampusMapper campusMapper,
            ClassGroupMapper classGroupMapper,
            LessonSessionMapper lessonSessionMapper,
            StudentMapper studentMapper,
            AttendanceRecordMapper attendanceRecordMapper,
            ClassEnrollmentMapper classEnrollmentMapper,
            com.edusphere.api.license.LicenseQuotaService licenseQuotaService
    ) {
        this.courseProductMapper = courseProductMapper;
        this.campusMapper = campusMapper;
        this.classGroupMapper = classGroupMapper;
        this.lessonSessionMapper = lessonSessionMapper;
        this.studentMapper = studentMapper;
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.classEnrollmentMapper = classEnrollmentMapper;
        this.licenseQuotaService = licenseQuotaService;
    }

    @GetMapping("/products")
    @RequirePermission("course:product:view")
    public ApiResult<PageResult<Map<String, Object>>> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<CourseProduct> wrapper = new LambdaQueryWrapper<CourseProduct>()
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false)
                .orderByDesc(CourseProduct::getCreatedAt);
        DataScopeSupport.applyOwnerScope(wrapper, CourseProduct::getCreatedBy);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(CourseProduct::getName, keyword)
                    .or()
                    .like(CourseProduct::getCategoryCode, keyword)
                    .or()
                    .like(CourseProduct::getExtensionTemplateCode, keyword));
        }
        if (hasText(status)) {
            wrapper.eq(CourseProduct::getStatus, status);
        }
        PageData<CourseProduct> pageData = selectPage(courseProductMapper, wrapper, page, pageSize);
        List<CourseProduct> products = pageData.records();
        return ApiResult.ok(PageResult.of(products.stream()
                .map(product -> Map.<String, Object>of(
                        "id", product.getId(),
                        "name", product.getName(),
                        "categoryCode", product.getCategoryCode(),
                        "deliveryMode", product.getDeliveryMode(),
                        "billingMode", product.getBillingMode(),
                        "totalLessons", product.getTotalLessons() == null ? 0 : product.getTotalLessons(),
                        "listPrice", product.getListPrice() == null ? "0.00" : product.getListPrice().toPlainString(),
                        "extensionTemplateCode", product.getExtensionTemplateCode() == null ? "" : product.getExtensionTemplateCode(),
                        "status", product.getStatus()
                ))
                .toList(), pageData.total(), pageData.page(), pageData.pageSize(), Map.of()));
    }

    @GetMapping("/products/{id}")
    @RequirePermission("course:product:view")
    public ApiResult<CourseProduct> productDetail(@PathVariable Long id) {
        CourseProduct product = loadProduct(id);
        return ApiResult.ok(product);
    }

    @PostMapping("/products")
    @RequirePermission("course:product:create")
    @Transactional
    public ApiResult<Long> createProduct(@RequestBody @Valid CourseProductRequest request) {
        Long tenantId = SecurityContext.tenantId();
        CourseProduct product = new CourseProduct();
        product.setTenantId(tenantId);
        product.setName(request.name());
        product.setCategoryCode(request.categoryCode());
        product.setDeliveryMode(request.deliveryMode());
        product.setBillingMode(request.billingMode());
        product.setTotalLessons(request.totalLessons());
        product.setListPrice(request.listPrice());
        product.setExtensionTemplateCode(request.extensionTemplateCode());
        product.setStatus(request.status());
        product.setCreatedBy(SecurityContext.accountId());
        courseProductMapper.insert(product);
        return ApiResult.ok(product.getId());
    }

    @PutMapping("/products/{id}")
    @RequirePermission("course:product:update")
    @Transactional
    public ApiResult<Void> updateProduct(@PathVariable Long id, @RequestBody @Valid CourseProductRequest request) {
        Long tenantId = SecurityContext.tenantId();
        int updated = courseProductMapper.update(null, new LambdaUpdateWrapper<CourseProduct>()
                .eq(CourseProduct::getId, id)
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false)
                .set(CourseProduct::getName, request.name())
                .set(CourseProduct::getCategoryCode, request.categoryCode())
                .set(CourseProduct::getDeliveryMode, request.deliveryMode())
                .set(CourseProduct::getBillingMode, request.billingMode())
                .set(CourseProduct::getTotalLessons, request.totalLessons())
                .set(CourseProduct::getListPrice, request.listPrice())
                .set(CourseProduct::getExtensionTemplateCode, request.extensionTemplateCode())
                .set(CourseProduct::getStatus, request.status())
                .set(CourseProduct::getUpdatedBy, SecurityContext.accountId()));
        if (updated == 0) {
            throw new BizException(404, "课程不存在");
        }
        return ApiResult.ok();
    }

    @PutMapping("/products/{id}/disable")
    @RequirePermission("course:product:disable")
    @Transactional
    public ApiResult<Void> disableProduct(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        int updated = courseProductMapper.update(null, new LambdaUpdateWrapper<CourseProduct>()
                .eq(CourseProduct::getId, id)
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false)
                .set(CourseProduct::getStatus, "DISABLED"));
        if (updated == 0) {
            throw new BizException(404, "课程不存在");
        }
        return ApiResult.ok();
    }

    @GetMapping("/campuses")
    @RequirePermission("course:product:view")
    public ApiResult<List<Map<String, Object>>> campuses() {
        Long tenantId = SecurityContext.tenantId();
        List<Campus> campuses = campusMapper.selectList(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getTenantId, tenantId)
                .eq(Campus::getDeleted, false)
                .orderByAsc(Campus::getId));
        return ApiResult.ok(campuses.stream()
                .map(campus -> Map.<String, Object>of(
                        "id", campus.getId(),
                        "name", campus.getName(),
                        "code", campus.getCode(),
                        "address", campus.getAddress() == null ? "" : campus.getAddress(),
                        "status", campus.getStatus()
                ))
                .toList());
    }

    @GetMapping("/classes")
    @RequirePermission("course:product:view")
    public ApiResult<PageResult<Map<String, Object>>> classes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<ClassGroup> wrapper = new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getDeleted, false)
                .orderByDesc(ClassGroup::getCreatedAt);
        DataScopeSupport.applyCampusScope(wrapper, ClassGroup::getCampusId, ClassGroup::getCreatedBy);
        if (hasText(keyword)) {
            wrapper.like(ClassGroup::getName, keyword);
        }
        if (hasText(status)) {
            wrapper.eq(ClassGroup::getStatus, status);
        }
        PageData<ClassGroup> pageData = selectPage(classGroupMapper, wrapper, page, pageSize);
        List<ClassGroup> classGroups = pageData.records();
        Map<Long, CourseProduct> productMap = loadProducts(classGroups.stream().map(ClassGroup::getCourseProductId).toList());
        Map<Long, Campus> campusMap = loadCampuses(classGroups.stream().map(ClassGroup::getCampusId).filter(id -> id != null && id > 0).toList());
        return ApiResult.ok(PageResult.of(classGroups.stream()
                .map(group -> {
                    CourseProduct product = productMap.get(group.getCourseProductId());
                    Campus campus = group.getCampusId() == null ? null : campusMap.get(group.getCampusId());
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", group.getId());
                    payload.put("name", group.getName());
                    payload.put("courseProductId", group.getCourseProductId());
                    payload.put("courseProductName", product == null ? "" : product.getName());
                    payload.put("campusId", group.getCampusId() == null ? 0L : group.getCampusId());
                    payload.put("campusName", campus == null ? "" : campus.getName());
                    payload.put("headTeacherId", group.getHeadTeacherId() == null ? 0L : group.getHeadTeacherId());
                    payload.put("capacity", group.getCapacity() == null ? 0 : group.getCapacity());
                    payload.put("startDate", group.getStartDate() == null ? "" : group.getStartDate().toString());
                    payload.put("endDate", group.getEndDate() == null ? "" : group.getEndDate().toString());
                    payload.put("status", group.getStatus());
                    return payload;
                })
                .toList(), pageData.total(), pageData.page(), pageData.pageSize(), Map.of()));
    }

    @PostMapping("/classes")
    @RequirePermission("course:product:create")
    @Transactional
    public ApiResult<Long> createClass(@RequestBody @Valid ClassGroupRequest request) {
        Long tenantId = SecurityContext.tenantId();
        validateClassRequest(tenantId, request);
        ClassGroup classGroup = new ClassGroup();
        classGroup.setTenantId(tenantId);
        classGroup.setCourseProductId(request.courseProductId());
        classGroup.setCampusId(request.campusId());
        classGroup.setName(request.name());
        classGroup.setHeadTeacherId(request.headTeacherId());
        classGroup.setCapacity(request.capacity());
        classGroup.setStartDate(request.startDate());
        classGroup.setEndDate(request.endDate());
        classGroup.setStatus(request.status());
        classGroup.setCreatedBy(SecurityContext.accountId());
        classGroupMapper.insert(classGroup);
        return ApiResult.ok(classGroup.getId());
    }

    @PutMapping("/classes/{id}")
    @RequirePermission("course:product:update")
    @Transactional
    public ApiResult<Void> updateClass(@PathVariable Long id, @RequestBody @Valid ClassGroupRequest request) {
        Long tenantId = SecurityContext.tenantId();
        validateClassRequest(tenantId, request);
        int updated = classGroupMapper.update(null, new LambdaUpdateWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getId, id)
                .eq(ClassGroup::getDeleted, false)
                .set(ClassGroup::getCourseProductId, request.courseProductId())
                .set(ClassGroup::getCampusId, request.campusId())
                .set(ClassGroup::getName, request.name())
                .set(ClassGroup::getHeadTeacherId, request.headTeacherId())
                .set(ClassGroup::getCapacity, request.capacity())
                .set(ClassGroup::getStartDate, request.startDate())
                .set(ClassGroup::getEndDate, request.endDate())
                .set(ClassGroup::getStatus, request.status())
                .set(ClassGroup::getUpdatedBy, SecurityContext.accountId()));
        if (updated == 0) {
            throw new BizException(404, "班级不存在");
        }
        return ApiResult.ok();
    }

    @GetMapping("/lessons")
    @RequirePermission("course:lesson:view")
    public ApiResult<PageResult<Map<String, Object>>> lessons(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        List<Long> accessibleClassGroupIds = accessibleClassGroupIds(tenantId);
        if (accessibleClassGroupIds.isEmpty()) {
            return ApiResult.ok(PageResult.of(List.of(), 0, normalizePage(page), normalizePageSize(pageSize), Map.of()));
        }
        LambdaQueryWrapper<LessonSession> wrapper = new LambdaQueryWrapper<LessonSession>()
                .eq(LessonSession::getTenantId, tenantId)
                .eq(LessonSession::getDeleted, false)
                .in(LessonSession::getClassGroupId, accessibleClassGroupIds)
                .orderByDesc(LessonSession::getPlannedStartAt);
        if (hasText(status)) {
            wrapper.eq(LessonSession::getStatus, status);
        }
        PageData<LessonSession> pageData = selectPage(lessonSessionMapper, wrapper, page, pageSize);
        List<LessonSession> lessons = pageData.records();
        Map<Long, ClassGroup> classGroupMap = loadClassGroups(lessons.stream().map(LessonSession::getClassGroupId).toList());
        Map<Long, CourseProduct> productMap = loadProducts(classGroupMap.values().stream().map(ClassGroup::getCourseProductId).toList());
        Map<Long, Campus> campusMap = loadCampuses(classGroupMap.values().stream().map(ClassGroup::getCampusId).filter(id -> id != null && id > 0).toList());
        return ApiResult.ok(PageResult.of(lessons.stream()
                .map(lesson -> {
                    ClassGroup classGroup = classGroupMap.get(lesson.getClassGroupId());
                    CourseProduct product = classGroup == null ? null : productMap.get(classGroup.getCourseProductId());
                    Campus campus = classGroup == null || classGroup.getCampusId() == null ? null : campusMap.get(classGroup.getCampusId());
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", lesson.getId());
                    payload.put("classGroupId", lesson.getClassGroupId());
                    payload.put("classGroupName", classGroup == null ? "" : classGroup.getName());
                    payload.put("courseProductName", product == null ? "" : product.getName());
                    payload.put("campusName", campus == null ? "" : campus.getName());
                    payload.put("teacherId", lesson.getTeacherId() == null ? 0L : lesson.getTeacherId());
                    payload.put("classroomId", lesson.getClassroomId() == null ? 0L : lesson.getClassroomId());
                    payload.put("onlineRoomUrl", lesson.getOnlineRoomUrl() == null ? "" : lesson.getOnlineRoomUrl());
                    payload.put("plannedStartAt", lesson.getPlannedStartAt() == null ? "" : lesson.getPlannedStartAt().toString());
                    payload.put("plannedEndAt", lesson.getPlannedEndAt() == null ? "" : lesson.getPlannedEndAt().toString());
                    payload.put("status", lesson.getStatus());
                    return payload;
                })
                .toList(), pageData.total(), pageData.page(), pageData.pageSize(), Map.of()));
    }

    @PostMapping("/lessons")
    @RequirePermission("course:lesson:create")
    @Transactional
    public ApiResult<Long> createLesson(@RequestBody @Valid LessonSessionRequest request) {
        Long tenantId = SecurityContext.tenantId();
        validateLessonRequest(tenantId, request);
        LessonSession lesson = new LessonSession();
        lesson.setTenantId(tenantId);
        lesson.setClassGroupId(request.classGroupId());
        lesson.setTeacherId(request.teacherId());
        lesson.setClassroomId(request.classroomId());
        lesson.setOnlineRoomUrl(request.onlineRoomUrl());
        lesson.setPlannedStartAt(request.plannedStartAt());
        lesson.setPlannedEndAt(request.plannedEndAt());
        lesson.setStatus(request.status());
        lesson.setCreatedBy(SecurityContext.accountId());
        lessonSessionMapper.insert(lesson);
        return ApiResult.ok(lesson.getId());
    }

    @PutMapping("/lessons/{id}")
    @RequirePermission("course:lesson:update")
    @Transactional
    public ApiResult<Void> updateLesson(@PathVariable Long id, @RequestBody @Valid LessonSessionRequest request) {
        Long tenantId = SecurityContext.tenantId();
        validateLessonRequest(tenantId, request);
        int updated = lessonSessionMapper.update(null, new LambdaUpdateWrapper<LessonSession>()
                .eq(LessonSession::getTenantId, tenantId)
                .eq(LessonSession::getId, id)
                .eq(LessonSession::getDeleted, false)
                .set(LessonSession::getClassGroupId, request.classGroupId())
                .set(LessonSession::getTeacherId, request.teacherId())
                .set(LessonSession::getClassroomId, request.classroomId())
                .set(LessonSession::getOnlineRoomUrl, request.onlineRoomUrl())
                .set(LessonSession::getPlannedStartAt, request.plannedStartAt())
                .set(LessonSession::getPlannedEndAt, request.plannedEndAt())
                .set(LessonSession::getStatus, request.status())
                .set(LessonSession::getUpdatedBy, SecurityContext.accountId()));
        if (updated == 0) {
            throw new BizException(404, "课次不存在");
        }
        return ApiResult.ok();
    }

    @GetMapping("/students")
    @RequirePermission("course:student:view")
    public ApiResult<PageResult<Map<String, Object>>> students(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getDeleted, false)
                .orderByDesc(Student::getCreatedAt);
        DataScopeSupport.applyOwnerScope(wrapper, Student::getCreatedBy);
        if (hasText(keyword)) {
            wrapper.and(q -> q.like(Student::getName, keyword)
                    .or()
                    .like(Student::getPhone, keyword)
                    .or()
                    .like(Student::getGuardianName, keyword)
                    .or()
                    .like(Student::getGuardianPhone, keyword));
        }
        if (hasText(status)) {
            wrapper.eq(Student::getStatus, status);
        }
        PageData<Student> pageData = selectPage(studentMapper, wrapper, page, pageSize);
        List<Student> students = pageData.records();
        return ApiResult.ok(PageResult.of(students.stream()
                .map(student -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", student.getId());
                    payload.put("name", student.getName());
                    payload.put("phone", student.getPhone() == null ? "" : student.getPhone());
                    payload.put("guardianName", student.getGuardianName() == null ? "" : student.getGuardianName());
                    payload.put("guardianPhone", student.getGuardianPhone() == null ? "" : student.getGuardianPhone());
                    payload.put("source", student.getSource() == null ? "" : student.getSource());
                    payload.put("status", student.getStatus());
                    return payload;
                })
                .toList(), pageData.total(), pageData.page(), pageData.pageSize(), Map.of()));
    }

    @PostMapping("/students")
    @RequirePermission("course:student:create")
    @Transactional
    public ApiResult<Long> createStudent(@RequestBody @Valid StudentRequest request) {
        Long tenantId = SecurityContext.tenantId();
        licenseQuotaService.assertCanCreateStudent();
        Student student = new Student();
        student.setTenantId(tenantId);
        student.setName(request.name());
        student.setPhone(request.phone());
        student.setGuardianName(request.guardianName());
        student.setGuardianPhone(request.guardianPhone());
        student.setSource(request.source());
        student.setStatus(request.status());
        student.setCreatedBy(SecurityContext.accountId());
        studentMapper.insert(student);
        return ApiResult.ok(student.getId());
    }

    @PutMapping("/students/{id}")
    @RequirePermission("course:student:update")
    @Transactional
    public ApiResult<Void> updateStudent(@PathVariable Long id, @RequestBody @Valid StudentRequest request) {
        Long tenantId = SecurityContext.tenantId();
        int updated = studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getId, id)
                .eq(Student::getDeleted, false)
                .set(Student::getName, request.name())
                .set(Student::getPhone, request.phone())
                .set(Student::getGuardianName, request.guardianName())
                .set(Student::getGuardianPhone, request.guardianPhone())
                .set(Student::getSource, request.source())
                .set(Student::getStatus, request.status())
                .set(Student::getUpdatedBy, SecurityContext.accountId()));
        if (updated == 0) {
            throw new BizException(404, "学员不存在");
        }
        return ApiResult.ok();
    }

    @GetMapping("/attendance")
    @RequirePermission("course:attendance:view")
    public ApiResult<List<Map<String, Object>>> attendance(@RequestParam Long lessonId) {
        Long tenantId = SecurityContext.tenantId();
        loadLessonWithTenant(tenantId, lessonId);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getDeleted, false)
                .orderByDesc(Student::getCreatedAt);
        DataScopeSupport.applyOwnerScope(wrapper, Student::getCreatedBy);
        List<Student> students = studentMapper.selectList(wrapper);
        Map<Long, AttendanceRecord> recordMap = loadAttendanceRecordMap(tenantId, lessonId);
        return ApiResult.ok(students.stream()
                .map(student -> {
                    AttendanceRecord record = recordMap.get(student.getId());
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("studentId", student.getId());
                    payload.put("studentName", student.getName());
                    payload.put("phone", student.getPhone() == null ? "" : student.getPhone());
                    payload.put("status", record == null ? "PENDING" : record.getStatus());
                    payload.put("remark", record == null || record.getRemark() == null ? "" : record.getRemark());
                    payload.put("checkedAt", record == null || record.getCheckedAt() == null ? "" : record.getCheckedAt().toString());
                    return payload;
                })
                .toList());
    }

    @PutMapping("/attendance")
    @RequirePermission("course:attendance:update")
    @Transactional
    public ApiResult<Void> saveAttendance(@RequestBody @Valid AttendanceSaveRequest request) {
        Long tenantId = SecurityContext.tenantId();
        loadLessonWithTenant(tenantId, request.lessonId());
        Map<Long, Student> studentMap = loadStudents(tenantId, request.records().stream().map(AttendanceItemRequest::studentId).toList());
        for (AttendanceItemRequest item : request.records()) {
            if (!studentMap.containsKey(item.studentId())) {
                throw new BizException(404, "学员不存在");
            }
            AttendanceRecord existing = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecord>()
                    .eq(AttendanceRecord::getTenantId, tenantId)
                    .eq(AttendanceRecord::getLessonSessionId, request.lessonId())
                    .eq(AttendanceRecord::getStudentId, item.studentId())
                    .eq(AttendanceRecord::getDeleted, false));
            LocalDateTime checkedAt = Objects.equals(item.status(), "PENDING") ? null : LocalDateTime.now();
            if (existing == null) {
                AttendanceRecord record = new AttendanceRecord();
                record.setTenantId(tenantId);
                record.setLessonSessionId(request.lessonId());
                record.setStudentId(item.studentId());
                record.setStatus(item.status());
                record.setRemark(item.remark());
                record.setCheckedAt(checkedAt);
                record.setCreatedBy(SecurityContext.accountId());
                attendanceRecordMapper.insert(record);
            } else {
                attendanceRecordMapper.update(null, new LambdaUpdateWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getTenantId, tenantId)
                        .eq(AttendanceRecord::getId, existing.getId())
                        .eq(AttendanceRecord::getDeleted, false)
                        .set(AttendanceRecord::getStatus, item.status())
                        .set(AttendanceRecord::getRemark, item.remark())
                        .set(AttendanceRecord::getCheckedAt, checkedAt)
                        .set(AttendanceRecord::getUpdatedBy, SecurityContext.accountId()));
            }
        }
        return ApiResult.ok();
    }

    @GetMapping("/enrollments")
    @RequirePermission("course:student:view")
    public ApiResult<PageResult<Map<String, Object>>> enrollments(
            @RequestParam(required = false) Long classGroupId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<ClassEnrollment> wrapper = new LambdaQueryWrapper<ClassEnrollment>()
                .eq(ClassEnrollment::getTenantId, tenantId)
                .eq(ClassEnrollment::getDeleted, false)
                .orderByDesc(ClassEnrollment::getCreatedAt);
        if (classGroupId != null && classGroupId > 0) {
            loadClassGroupWithTenant(tenantId, classGroupId);
            wrapper.eq(ClassEnrollment::getClassGroupId, classGroupId);
        } else {
            List<Long> accessibleClassGroupIds = accessibleClassGroupIds(tenantId);
            if (accessibleClassGroupIds.isEmpty()) {
                return ApiResult.ok(PageResult.of(List.of(), 0, normalizePage(page), normalizePageSize(pageSize), Map.of()));
            }
            wrapper.in(ClassEnrollment::getClassGroupId, accessibleClassGroupIds);
        }
        PageData<ClassEnrollment> pageData = selectPage(classEnrollmentMapper, wrapper, page, pageSize);
        List<ClassEnrollment> enrollments = pageData.records();
        Map<Long, Student> studentMap = loadStudents(tenantId, enrollments.stream().map(ClassEnrollment::getStudentId).toList());
        Map<Long, ClassGroup> classGroupMap = loadClassGroups(enrollments.stream().map(ClassEnrollment::getClassGroupId).toList());
        return ApiResult.ok(PageResult.of(enrollments.stream()
                .map(enrollment -> {
                    Student student = studentMap.get(enrollment.getStudentId());
                    ClassGroup classGroup = classGroupMap.get(enrollment.getClassGroupId());
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", enrollment.getId());
                    payload.put("classGroupId", enrollment.getClassGroupId());
                    payload.put("classGroupName", classGroup == null ? "" : classGroup.getName());
                    payload.put("studentId", enrollment.getStudentId());
                    payload.put("studentName", student == null ? "" : student.getName());
                    payload.put("enrollStatus", enrollment.getEnrollStatus());
                    payload.put("enrollDate", enrollment.getEnrollDate() == null ? "" : enrollment.getEnrollDate().toString());
                    payload.put("remark", enrollment.getRemark() == null ? "" : enrollment.getRemark());
                    return payload;
                })
                .toList(), pageData.total(), pageData.page(), pageData.pageSize(), Map.of()));
    }

    @PostMapping("/enrollments")
    @RequirePermission("course:student:create")
    @Transactional
    public ApiResult<Void> saveEnrollments(@RequestBody @Valid EnrollmentRequest request) {
        Long tenantId = SecurityContext.tenantId();
        loadClassGroupWithTenant(tenantId, request.classGroupId());
        Map<Long, Student> studentMap = loadStudents(tenantId, request.studentIds());
        for (Long studentId : request.studentIds()) {
            if (!studentMap.containsKey(studentId)) {
                throw new BizException(404, "学员不存在");
            }
            ClassEnrollment existing = classEnrollmentMapper.selectOne(new LambdaQueryWrapper<ClassEnrollment>()
                    .eq(ClassEnrollment::getTenantId, tenantId)
                    .eq(ClassEnrollment::getClassGroupId, request.classGroupId())
                    .eq(ClassEnrollment::getStudentId, studentId)
                    .eq(ClassEnrollment::getDeleted, false));
            if (existing == null) {
                ClassEnrollment enrollment = new ClassEnrollment();
                enrollment.setTenantId(tenantId);
                enrollment.setClassGroupId(request.classGroupId());
                enrollment.setStudentId(studentId);
                enrollment.setEnrollStatus("ACTIVE");
                enrollment.setEnrollDate(LocalDate.now());
                enrollment.setRemark(request.remark());
                enrollment.setCreatedBy(SecurityContext.accountId());
                classEnrollmentMapper.insert(enrollment);
            } else {
                classEnrollmentMapper.update(null, new LambdaUpdateWrapper<ClassEnrollment>()
                        .eq(ClassEnrollment::getTenantId, tenantId)
                        .eq(ClassEnrollment::getId, existing.getId())
                        .eq(ClassEnrollment::getDeleted, false)
                        .set(ClassEnrollment::getEnrollStatus, "ACTIVE")
                        .set(ClassEnrollment::getEnrollDate, LocalDate.now())
                        .set(ClassEnrollment::getRemark, request.remark())
                        .set(ClassEnrollment::getUpdatedBy, SecurityContext.accountId()));
            }
        }
        return ApiResult.ok();
    }

    private void validateClassRequest(Long tenantId, ClassGroupRequest request) {
        CourseProduct product = loadProductWithTenant(tenantId, request.courseProductId());
        if (request.campusId() != null && request.campusId() > 0) {
            Campus campus = campusMapper.selectOne(new LambdaQueryWrapper<Campus>()
                    .eq(Campus::getTenantId, tenantId)
                    .eq(Campus::getId, request.campusId())
                    .eq(Campus::getDeleted, false));
            if (campus == null) {
                throw new BizException(404, "校区不存在");
            }
            DataScopeSupport.requireCampusAccess(campus.getId(), campus.getCreatedBy());
        }
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BizException(400, "结束日期不能早于开始日期");
        }
        if (product == null) {
            throw new BizException(404, "课程不存在");
        }
    }

    private void validateLessonRequest(Long tenantId, LessonSessionRequest request) {
        ClassGroup classGroup = loadClassGroupWithTenant(tenantId, request.classGroupId());
        if (classGroup == null) {
            throw new BizException(404, "班级不存在");
        }
        if (request.plannedEndAt().isBefore(request.plannedStartAt()) || request.plannedEndAt().isEqual(request.plannedStartAt())) {
            throw new BizException(400, "下课时间必须晚于上课时间");
        }
    }

    private LessonSession loadLessonWithTenant(Long tenantId, Long id) {
        LessonSession lesson = lessonSessionMapper.selectOne(new LambdaQueryWrapper<LessonSession>()
                .eq(LessonSession::getTenantId, tenantId)
                .eq(LessonSession::getId, id)
                .eq(LessonSession::getDeleted, false));
        if (lesson == null) {
            throw new BizException(404, "课次不存在");
        }
        loadClassGroupWithTenant(tenantId, lesson.getClassGroupId());
        return lesson;
    }

    private ClassGroup loadClassGroupWithTenant(Long tenantId, Long id) {
        ClassGroup classGroup = classGroupMapper.selectOne(new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getId, id)
                .eq(ClassGroup::getDeleted, false));
        if (classGroup == null) {
            throw new BizException(404, "班级不存在");
        }
        DataScopeSupport.requireCampusAccess(classGroup.getCampusId(), classGroup.getCreatedBy());
        return classGroup;
    }

    private CourseProduct loadProduct(Long id) {
        Long tenantId = SecurityContext.tenantId();
        return loadProductWithTenant(tenantId, id);
    }

    private CourseProduct loadProductWithTenant(Long tenantId, Long id) {
        LambdaQueryWrapper<CourseProduct> wrapper = new LambdaQueryWrapper<CourseProduct>()
                .eq(CourseProduct::getId, id)
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false);
        DataScopeSupport.applyOwnerScope(wrapper, CourseProduct::getCreatedBy);
        CourseProduct product = courseProductMapper.selectOne(wrapper);
        if (product == null) {
            throw new BizException(404, "课程不存在");
        }
        return product;
    }

    private Map<Long, CourseProduct> loadProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<CourseProduct> wrapper = new LambdaQueryWrapper<CourseProduct>()
                .eq(CourseProduct::getTenantId, tenantId)
                .eq(CourseProduct::getDeleted, false)
                .in(CourseProduct::getId, ids);
        DataScopeSupport.applyOwnerScope(wrapper, CourseProduct::getCreatedBy);
        List<CourseProduct> products = courseProductMapper.selectList(wrapper);
        Map<Long, CourseProduct> map = new LinkedHashMap<>();
        products.forEach(product -> map.put(product.getId(), product));
        return map;
    }

    private Map<Long, Campus> loadCampuses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        List<Campus> campuses = campusMapper.selectList(new LambdaQueryWrapper<Campus>()
                .eq(Campus::getTenantId, tenantId)
                .eq(Campus::getDeleted, false)
                .in(Campus::getId, ids));
        Map<Long, Campus> map = new LinkedHashMap<>();
        campuses.forEach(campus -> map.put(campus.getId(), campus));
        return map;
    }

    private Map<Long, ClassGroup> loadClassGroups(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<ClassGroup> wrapper = new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getDeleted, false)
                .in(ClassGroup::getId, ids);
        DataScopeSupport.applyCampusScope(wrapper, ClassGroup::getCampusId, ClassGroup::getCreatedBy);
        List<ClassGroup> classGroups = classGroupMapper.selectList(wrapper);
        Map<Long, ClassGroup> map = new LinkedHashMap<>();
        classGroups.forEach(classGroup -> map.put(classGroup.getId(), classGroup));
        return map;
    }

    private Map<Long, AttendanceRecord> loadAttendanceRecordMap(Long tenantId, Long lessonId) {
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getTenantId, tenantId)
                .eq(AttendanceRecord::getLessonSessionId, lessonId)
                .eq(AttendanceRecord::getDeleted, false));
        Map<Long, AttendanceRecord> map = new LinkedHashMap<>();
        records.forEach(record -> map.put(record.getStudentId(), record));
        return map;
    }

    private Map<Long, Student> loadStudents(Long tenantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getTenantId, tenantId)
                .eq(Student::getDeleted, false)
                .in(Student::getId, ids);
        DataScopeSupport.applyOwnerScope(wrapper, Student::getCreatedBy);
        List<Student> students = studentMapper.selectList(wrapper);
        Map<Long, Student> map = new LinkedHashMap<>();
        students.forEach(student -> map.put(student.getId(), student));
        return map;
    }

    private List<Long> accessibleClassGroupIds(Long tenantId) {
        LambdaQueryWrapper<ClassGroup> wrapper = new LambdaQueryWrapper<ClassGroup>()
                .eq(ClassGroup::getTenantId, tenantId)
                .eq(ClassGroup::getDeleted, false);
        DataScopeSupport.applyCampusScope(wrapper, ClassGroup::getCampusId, ClassGroup::getCreatedBy);
        return classGroupMapper.selectList(wrapper).stream().map(ClassGroup::getId).toList();
    }

    private long normalizePage(long page) {
        return Math.max(1, page);
    }

    private long normalizePageSize(long pageSize) {
        return Math.min(Math.max(1, pageSize), 100);
    }

    private <T> PageData<T> selectPage(BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper, long page, long pageSize) {
        long normalizedPage = normalizePage(page);
        long normalizedPageSize = normalizePageSize(pageSize);
        Page<T> result = mapper.selectPage(new Page<>(normalizedPage, normalizedPageSize), wrapper);
        return new PageData<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PageData<T>(List<T> records, long total, long page, long pageSize) {
    }

    public record CourseProductRequest(
            @NotBlank String name,
            @NotBlank String categoryCode,
            @NotBlank String deliveryMode,
            @NotBlank String billingMode,
            Integer totalLessons,
            BigDecimal listPrice,
            String extensionTemplateCode,
            @NotNull String status
    ) {
    }

    public record ClassGroupRequest(
            @NotNull Long courseProductId,
            Long campusId,
            @NotBlank String name,
            Long headTeacherId,
            Integer capacity,
            LocalDate startDate,
            LocalDate endDate,
            @NotBlank String status
    ) {
    }

    public record LessonSessionRequest(
            @NotNull Long classGroupId,
            Long teacherId,
            Long classroomId,
            String onlineRoomUrl,
            @NotNull LocalDateTime plannedStartAt,
            @NotNull LocalDateTime plannedEndAt,
            @NotBlank String status
    ) {
    }

    public record StudentRequest(
            @NotBlank String name,
            String phone,
            String guardianName,
            String guardianPhone,
            String source,
            @NotBlank String status
    ) {
    }

    public record AttendanceSaveRequest(
            @NotNull Long lessonId,
            @NotNull List<AttendanceItemRequest> records
    ) {
    }

    public record AttendanceItemRequest(
            @NotNull Long studentId,
            @NotBlank String status,
            String remark
    ) {
    }

    public record EnrollmentRequest(
            @NotNull Long classGroupId,
            @NotNull List<Long> studentIds,
            String remark
    ) {
    }
}
