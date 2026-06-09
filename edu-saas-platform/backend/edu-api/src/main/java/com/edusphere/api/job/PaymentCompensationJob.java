package com.edusphere.api.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.edusphere.order.domain.PaymentOrder;
import com.edusphere.order.domain.RefundOrder;
import com.edusphere.order.mapper.PaymentOrderMapper;
import com.edusphere.order.mapper.RefundOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentCompensationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompensationJob.class);
    private static final List<String> PENDING_PAYMENT_STATUS = List.of("INIT", "PAYING");
    private static final List<String> PENDING_REFUND_STATUS = List.of("INIT", "REFUNDING");

    private final PaymentOrderMapper paymentOrderMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final boolean enabled;
    private final int batchSize;
    private final int maxCheckCount;

    public PaymentCompensationJob(
            PaymentOrderMapper paymentOrderMapper,
            RefundOrderMapper refundOrderMapper,
            @Value("${edu.payment.compensation.enabled:true}") boolean enabled,
            @Value("${edu.payment.compensation.batch-size:100}") int batchSize,
            @Value("${edu.payment.compensation.max-check-count:12}") int maxCheckCount
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.refundOrderMapper = refundOrderMapper;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.maxCheckCount = maxCheckCount;
    }

    @Scheduled(fixedDelayString = "${edu.payment.compensation.fixed-delay-ms:60000}", initialDelayString = "${edu.payment.compensation.initial-delay-ms:30000}")
    public void compensate() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int paymentCount = compensatePaymentOrders(now);
        int refundCount = compensateRefundOrders(now);
        if (paymentCount > 0 || refundCount > 0) {
            log.info("payment_compensation checkedPaymentOrders={} checkedRefundOrders={}", paymentCount, refundCount);
        }
    }

    private int compensatePaymentOrders(LocalDateTime now) {
        List<PaymentOrder> orders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getDeleted, false)
                .in(PaymentOrder::getStatus, PENDING_PAYMENT_STATUS)
                .le(PaymentOrder::getNextCheckAt, now)
                .orderByAsc(PaymentOrder::getNextCheckAt)
                .last("limit " + batchSize));
        for (PaymentOrder order : orders) {
            int nextCount = (order.getCheckCount() == null ? 0 : order.getCheckCount()) + 1;
            boolean close = nextCount >= maxCheckCount;
            paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTenantId, order.getTenantId())
                    .eq(PaymentOrder::getId, order.getId())
                    .eq(PaymentOrder::getDeleted, false)
                    .in(PaymentOrder::getStatus, PENDING_PAYMENT_STATUS)
                    .set(PaymentOrder::getLastCheckedAt, now)
                    .set(PaymentOrder::getCheckCount, nextCount)
                    .set(PaymentOrder::getStatusReason, close ? "补偿检查超限，自动关闭" : "等待支付通道结果")
                    .set(PaymentOrder::getStatus, close ? "CLOSED" : order.getStatus())
                    .set(PaymentOrder::getClosedAt, close ? now : null)
                    .set(PaymentOrder::getNextCheckAt, close ? null : now.plusMinutes(Math.min(30, nextCount * 5L))));
        }
        return orders.size();
    }

    private int compensateRefundOrders(LocalDateTime now) {
        List<RefundOrder> orders = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getDeleted, false)
                .in(RefundOrder::getStatus, PENDING_REFUND_STATUS)
                .le(RefundOrder::getNextCheckAt, now)
                .orderByAsc(RefundOrder::getNextCheckAt)
                .last("limit " + batchSize));
        for (RefundOrder order : orders) {
            int nextCount = (order.getCheckCount() == null ? 0 : order.getCheckCount()) + 1;
            boolean close = nextCount >= maxCheckCount;
            refundOrderMapper.update(null, new LambdaUpdateWrapper<RefundOrder>()
                    .eq(RefundOrder::getTenantId, order.getTenantId())
                    .eq(RefundOrder::getId, order.getId())
                    .eq(RefundOrder::getDeleted, false)
                    .in(RefundOrder::getStatus, PENDING_REFUND_STATUS)
                    .set(RefundOrder::getLastCheckedAt, now)
                    .set(RefundOrder::getCheckCount, nextCount)
                    .set(RefundOrder::getStatusReason, close ? "补偿检查超限，自动关闭" : "等待退款通道结果")
                    .set(RefundOrder::getStatus, close ? "CLOSED" : order.getStatus())
                    .set(RefundOrder::getClosedAt, close ? now : null)
                    .set(RefundOrder::getNextCheckAt, close ? null : now.plusMinutes(Math.min(30, nextCount * 5L))));
        }
        return orders.size();
    }
}
