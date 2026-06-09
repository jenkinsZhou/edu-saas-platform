package com.edusphere.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.tenant.domain.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
