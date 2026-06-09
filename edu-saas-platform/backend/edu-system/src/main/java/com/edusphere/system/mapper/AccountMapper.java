package com.edusphere.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.system.domain.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
