package com.edusphere.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edusphere.common.api.ApiResult;
import com.edusphere.common.api.PageResult;
import com.edusphere.common.exception.BizException;
import com.edusphere.course.domain.Classroom;
import com.edusphere.course.mapper.ClassroomMapper;
import com.edusphere.security.context.SecurityContext;
import com.edusphere.security.permission.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomMapper classroomMapper;

    public ClassroomController(ClassroomMapper classroomMapper) {
        this.classroomMapper = classroomMapper;
    }

    @GetMapping
    @RequirePermission("course:classroom:view")
    public ApiResult<PageResult<Classroom>> listClassrooms(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {

        Long tenantId = SecurityContext.tenantId();
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTenantId, tenantId)
                .eq(Classroom::getDeleted, false);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Classroom::getStatus, status);
        }

        wrapper.orderByDesc(Classroom::getCreatedAt);
        Page<Classroom> result = classroomMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return ApiResult.ok(PageResult.of(result));
    }

    @PostMapping
    @RequirePermission("course:classroom:create")
    public ApiResult<Long> createClassroom(@RequestBody @Valid CreateClassroomRequest request) {
        Long tenantId = SecurityContext.tenantId();

        Classroom classroom = new Classroom();
        classroom.setTenantId(tenantId);
        classroom.setCampusId(request.campusId());
        classroom.setName(request.name());
        classroom.setRoomNo(request.roomNo());
        classroom.setCapacity(request.capacity());
        classroom.setEquipment(request.equipment());
        classroom.setStatus("AVAILABLE");
        classroom.setCreatedBy(SecurityContext.accountId());
        classroomMapper.insert(classroom);
        return ApiResult.ok(classroom.getId());
    }

    @PutMapping("/{id}")
    @RequirePermission("course:classroom:update")
    public ApiResult<Void> updateClassroom(@PathVariable Long id, @RequestBody @Valid UpdateClassroomRequest request) {
        Long tenantId = SecurityContext.tenantId();
        Classroom classroom = classroomMapper.selectOne(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getTenantId, tenantId)
                .eq(Classroom::getId, id)
                .eq(Classroom::getDeleted, false));

        if (classroom == null) {
            throw new BizException(404, "教室不存在");
        }

        classroom.setName(request.name());
        classroom.setCapacity(request.capacity());
        classroom.setEquipment(request.equipment());
        classroom.setStatus(request.status());
        classroom.setUpdatedBy(SecurityContext.accountId());
        classroomMapper.updateById(classroom);
        return ApiResult.ok();
    }

    public record CreateClassroomRequest(
            Long campusId,
            @NotBlank String name,
            @NotBlank String roomNo,
            @NotNull Integer capacity,
            String equipment
    ) {}

    public record UpdateClassroomRequest(
            @NotBlank String name,
            @NotNull Integer capacity,
            String equipment,
            @NotBlank String status
    ) {}
}
