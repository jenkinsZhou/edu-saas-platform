package com.edusphere.api.service.payment;

import com.edusphere.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentCallbackVerifier {

    private final String callbackSecret;

    public PaymentCallbackVerifier(@Value("${edu.payment.callback-secret:dev-callback-secret}") String callbackSecret) {
        this.callbackSecret = callbackSecret;
    }

    public void verifySharedSecret(String actualSecret) {
        if (actualSecret == null || !MessageDigest.isEqual(
                callbackSecret.getBytes(StandardCharsets.UTF_8),
                actualSecret.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BizException(401, "回调签名无效");
        }
    }
}
