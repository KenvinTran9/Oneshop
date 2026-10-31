package com.oneshop.service;

import com.oneshop.entity.Order;
import java.util.UUID;

/** Immutable snapshot: each committed update keeps its own status and delivery details. */
public record OrderUpdateMail(String deliveryId, String code, String email, String customerName,
        String title, String message, String status, String paymentStatus, String carrier,
        String shipperName, String shipperPhone, String trackingCode, String estimatedDelivery, String deliveryNote) {
    public static OrderUpdateMail of(Order order, String title, String message) {
        return new OrderUpdateMail(UUID.randomUUID().toString(), order.getCode(), order.getCustomerEmail(),
                order.getCustomerName(), title, message, order.getStatus().getLabel(),
                order.getPaymentStatus().getLabel(), order.getCarrier(), order.getShipperName(),
                order.getShipperPhone(), order.getTrackingCode(),
                order.getEstimatedDelivery() == null ? null : order.getEstimatedDelivery().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                order.getDeliveryNote());
    }
}
