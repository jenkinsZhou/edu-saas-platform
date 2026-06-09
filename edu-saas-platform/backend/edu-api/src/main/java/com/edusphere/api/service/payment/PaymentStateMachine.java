package com.edusphere.api.service.payment;

import com.edusphere.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class PaymentStateMachine {

    private static final Set<String> PAYMENT_TERMINAL = Set.of("SUCCESS", "FAILED", "CLOSED");
    private static final Set<String> REFUND_TERMINAL = Set.of("SUCCESS", "FAILED", "CLOSED");
    private static final Map<String, Set<String>> PAYMENT_TRANSITIONS = Map.of(
            "INIT", Set.of("PAYING", "SUCCESS", "FAILED", "CLOSED"),
            "PAYING", Set.of("SUCCESS", "FAILED", "CLOSED"),
            "SUCCESS", Set.of(),
            "FAILED", Set.of(),
            "CLOSED", Set.of()
    );
    private static final Map<String, Set<String>> REFUND_TRANSITIONS = Map.of(
            "INIT", Set.of("REFUNDING", "SUCCESS", "FAILED", "CLOSED"),
            "REFUNDING", Set.of("SUCCESS", "FAILED", "CLOSED"),
            "SUCCESS", Set.of(),
            "FAILED", Set.of(),
            "CLOSED", Set.of()
    );

    public void requirePaymentTransition(String fromStatus, String toStatus) {
        requireTransition("支付单", fromStatus, toStatus, PAYMENT_TRANSITIONS);
    }

    public void requireRefundTransition(String fromStatus, String toStatus) {
        requireTransition("退款单", fromStatus, toStatus, REFUND_TRANSITIONS);
    }

    public boolean isTerminalPaymentStatus(String status) {
        return PAYMENT_TERMINAL.contains(status);
    }

    public boolean isTerminalRefundStatus(String status) {
        return REFUND_TERMINAL.contains(status);
    }

    private void requireTransition(String label, String fromStatus, String toStatus, Map<String, Set<String>> transitions) {
        if (!transitions.containsKey(toStatus)) {
            throw new BizException(400, label + "状态非法");
        }
        if (fromStatus == null || fromStatus.isBlank() || fromStatus.equals(toStatus)) {
            return;
        }
        Set<String> allowedTargets = transitions.get(fromStatus);
        if (allowedTargets == null) {
            throw new BizException(400, label + "当前状态非法");
        }
        if (!allowedTargets.contains(toStatus)) {
            throw new BizException(409, label + "不能从 " + fromStatus + " 流转到 " + toStatus);
        }
    }
}
