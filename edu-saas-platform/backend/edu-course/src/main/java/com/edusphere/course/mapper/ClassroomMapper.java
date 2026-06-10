package com.edusphere.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.course.domain.Classroom;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {
}
