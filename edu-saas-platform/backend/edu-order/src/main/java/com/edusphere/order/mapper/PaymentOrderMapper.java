package com.edusphere.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edusphere.order.domain.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
