package com.edusphere.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.system.domain.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
