package com.edusphere.api.event;

import com.edusphere.common.trace.OperationLogEvent;
import com.edusphere.system.domain.OperationLog;
import com.edusphere.system.mapper.OperationLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
public class OperationLogEventListener {

    private final OperationLogMapper operationLogMapper;

    public OperationLogEventListener(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Async("operationLogExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OperationLogEvent event) {
        OperationLog log = new OperationLog();
        log.setTenantId(event.tenantId());
        log.setAccountId(event.accountId());
        log.setUsername(event.username());
        log.setModule(event.module());
        log.setAction(event.action());
        log.setTargetType(event.targetType());
        log.setTargetId(event.targetId());
        log.setSuccess(event.success());
        log.setRequestId(event.requestId());
        log.setDetail(event.detail());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
