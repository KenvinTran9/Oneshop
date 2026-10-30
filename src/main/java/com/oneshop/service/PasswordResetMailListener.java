package com.oneshop.service;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class PasswordResetMailListener {
    private final PasswordResetMailService mail;
    private final ThreadPoolTaskExecutor executor;
    public PasswordResetMailListener(PasswordResetMailService mail, ThreadPoolTaskExecutor passwordResetMailExecutor) {
        this.mail = mail; this.executor = passwordResetMailExecutor;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requested(PasswordResetService.Requested event) {
        // Deliver after commit and off the response thread, including on provider/queue failure.
        try { executor.execute(() -> mail.send(event)); }
        catch (org.springframework.core.task.TaskRejectedException failure) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password reset mail queue is busy");
        }
    }
}
