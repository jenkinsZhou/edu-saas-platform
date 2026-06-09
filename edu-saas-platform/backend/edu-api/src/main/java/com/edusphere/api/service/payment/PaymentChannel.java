package com.edusphere.api.service.payment;

public interface PaymentChannel {

    String channelCode();

    String defaultPaymentMethod();

    String successfulPaymentReason();

    String successfulRefundReason();
}
