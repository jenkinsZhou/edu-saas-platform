package com.edusphere.api.service.payment;

import com.edusphere.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentChannelRegistry {

    private final Map<String, PaymentChannel> channels;

    public PaymentChannelRegistry(List<PaymentChannel> channels) {
        this.channels = channels.stream().collect(Collectors.toMap(PaymentChannel::channelCode, Function.identity()));
    }

    public PaymentChannel require(String channelCode) {
        PaymentChannel channel = channels.get(channelCode);
        if (channel == null) {
            throw new BizException(400, "支付通道未配置：" + channelCode);
        }
        return channel;
    }
}
