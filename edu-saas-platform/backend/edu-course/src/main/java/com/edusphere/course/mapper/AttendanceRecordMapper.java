package com.edusphere.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.course.domain.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
}
