package com.oneshop.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class OrderMailListener {
    private final OrderMailService mail;
    public OrderMailListener(OrderMailService mail) { this.mail = mail; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void updated(OrderUpdateMail event) {
        try { mail.sendUpdate(event); }
        catch (Exception failure) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not send update for order {} ({})",
                    event.code(), failure.getClass().getSimpleName());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void placed(OrderService.Placed event) {
        // SMTP failures must never turn an already committed checkout into an error response.
        try { mail.sendConfirmation(event.code()); }
        catch (Exception failure) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Confirmation email pending for order {} ({})",
                    event.code(), failure.getClass().getSimpleName());
        }
    }
}
