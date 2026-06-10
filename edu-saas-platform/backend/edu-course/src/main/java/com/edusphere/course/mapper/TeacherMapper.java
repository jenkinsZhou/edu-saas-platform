package com.edusphere.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.course.domain.Teacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
}
