package com.edusphere.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.course.domain.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
