package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.Teacher;
import com.edusphere.course.mapper.TeacherMapper;
import com.edusphere.security.context.DataScopeSupport;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherMapper teacherMapper;

    public TeacherController(TeacherMapper teacherMapper) {
        this.teacherMapper = teacherMapper;
    }

    @GetMapping
    @RequirePermission("course:teacher:view")
    public ApiResult<PageResult<Teacher>> listTeachers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTenantId, tenantId)
                .eq(Teacher::getDeleted, false);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Teacher::getName, keyword)
                    .or().like(Teacher::getPhone, keyword)
                    .or().like(Teacher::getEmployeeNo, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Teacher::getStatus, status);
        }

        DataScopeSupport.applyCampusScope(wrapper, null, Teacher::getCreatedBy);
        wrapper.orderByDesc(Teacher::getCreatedAt);

        Page<Teacher> result = teacherMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePermission("course:teacher:view")
    public ApiResult<Teacher> getTeacher(@PathVariable Long id) {
        Long tenantId = SecurityContext.tenantId();
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTenantId, tenantId)
                .eq(Teacher::getId, id)
                .eq(Teacher::getDeleted, false));
        if (teacher == null) {
            throw new BizException(404, "教师不存在");
        }
        return ApiResult.ok(teacher);
    }

    @PostMapping
    @RequirePermission("course:teacher:create")
    public ApiResult<Long> createTeacher(@RequestBody @Valid CreateTeacherRequest request) {
        Long tenantId = SecurityContext.tenantId();

        if (teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTenantId, tenantId)
                .eq(Teacher::getEmployeeNo, request.employeeNo())
                .eq(Teacher::getDeleted, false)) > 0) {
            throw new BizException(409, "工号已存在");
        }

        Teacher teacher = new Teacher();
        teacher.setTenantId(tenantId);
        teacher.setEmployeeNo(request.employeeNo());
        teacher.setName(request.name());
        teacher.setPhone(request.phone());
        teacher.setEmail(request.email());
        teacher.setGender(request.gender());
        teacher.setSubjects(request.subjects());
        teacher.setTitle(request.title());
        teacher.setStatus("ACTIVE");
        teacher.setCreatedBy(SecurityContext.accountId());
        teacherMapper.insert(teacher);
        return ApiResult.ok(teacher.getId());
    }

    @PutMapping("/{id}")
    @RequirePermission("course:teacher:update")
    public ApiResult<Void> updateTeacher(@PathVariable Long id, @RequestBody @Valid UpdateTeacherRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTenantId, tenantId)
                .eq(Teacher::getId, id)
                .eq(Teacher::getDeleted, false));
        if (teacher == null) {
            throw new BizException(404, "教师不存在");
        }

        teacher.setName(request.name());
        teacher.setPhone(request.phone());
        teacher.setEmail(request.email());
        teacher.setGender(request.gender());
        teacher.setSubjects(request.subjects());
        teacher.setTitle(request.title());
        teacher.setUpdatedBy(SecurityContext.accountId());
        teacherMapper.updateById(teacher);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/status")
    @RequirePermission("course:teacher:update")
    public ApiResult<Void> updateTeacherStatus(@PathVariable Long id, @RequestBody @Valid UpdateStatusRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTenantId, tenantId)
                .eq(Teacher::getId, id)
                .eq(Teacher::getDeleted, false));
        if (teacher == null) {
            throw new BizException(404, "教师不存在");
        }

        teacher.setStatus(request.status());
        teacher.setUpdatedBy(SecurityContext.accountId());
        teacherMapper.updateById(teacher);
        return ApiResult.ok();
    }

    public record CreateTeacherRequest(
            @NotBlank String employeeNo,
            @NotBlank String name,
            @NotBlank String phone,
            String email,
            String gender,
            String subjects,
            String title
    ) {}

    public record UpdateTeacherRequest(
            @NotBlank String name,
            @NotBlank String phone,
            String email,
            String gender,
            String subjects,
            String title
    ) {}

    public record UpdateStatusRequest(@NotBlank String status) {}
}
