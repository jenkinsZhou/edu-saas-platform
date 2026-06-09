package com.edusphere.api.service.payment;

import org.springframework.stereotype.Component;

@Component
public class OfflinePaymentChannel implements PaymentChannel {

    @Override
    public String channelCode() {
        return "OFFLINE";
    }

    @Override
    public String defaultPaymentMethod() {
        return "CASH";
    }

    @Override
    public String successfulPaymentReason() {
        return "手工收款成功";
    }

    @Override
    public String successfulRefundReason() {
        return "手工退款成功";
    }
}
