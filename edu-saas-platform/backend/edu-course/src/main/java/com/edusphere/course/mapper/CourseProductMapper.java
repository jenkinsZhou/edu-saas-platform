package com.edusphere.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.course.domain.CourseProduct;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseProductMapper extends BaseMapper<CourseProduct> {
}
