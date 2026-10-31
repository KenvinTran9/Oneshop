package com.oneshop.dto.response;

import com.oneshop.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderView(String code, OrderStatus status, LocalDateTime createdAt, String customerName,
        String customerEmail, String customerPhone, String shippingAddress, String storeName, String note,
        BigDecimal subtotal, BigDecimal shippingFee, BigDecimal totalAmount, List<Item> items,
        List<Event> events, String emailStatus, LocalDateTime emailSentAt,
        Payment payment, Delivery delivery, long version) {
    public record Payment(com.oneshop.entity.PaymentMethod method, com.oneshop.entity.PaymentStatus status,
            LocalDateTime paidAt, String reference, String bankName, String accountNumber, String accountHolder,
            String qrImage, String transferContent) { }
    public record Delivery(String carrier, String shipperName, String shipperPhone, String trackingCode,
            java.time.LocalDate estimatedDelivery, String note) {
        public boolean isPresent() { return carrier != null || shipperName != null || trackingCode != null || estimatedDelivery != null || note != null; }
    }
    public record Item(String name, String imageUrl, String size, String shade, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) { }
    public record Event(OrderStatus status, String message, LocalDateTime time) { }
    public boolean isCustomerCancellable() { return status == OrderStatus.NEW; }
    public boolean isDeliveryReady() { return delivery.shipperName() != null && !delivery.shipperName().isBlank()
            && delivery.shipperPhone() != null && !delivery.shipperPhone().isBlank(); }
    public boolean isDeliveryEditable() { return status == OrderStatus.PACKING || status == OrderStatus.SHIPPING; }
    public boolean isTransferReportAllowed() {
        return (status == OrderStatus.NEW || status == OrderStatus.CONFIRMED) && payment.method() == com.oneshop.entity.PaymentMethod.BANK_TRANSFER
                && payment.status() == com.oneshop.entity.PaymentStatus.UNPAID;
    }
    public List<OrderStatus> getNextStatuses() {
        return status.getNextStatuses().stream().filter(next -> next != OrderStatus.COMPLETED)
                .filter(next -> next == OrderStatus.CANCELLED || next == OrderStatus.CONFIRMED
                || payment.method() != com.oneshop.entity.PaymentMethod.BANK_TRANSFER
                || payment.status() == com.oneshop.entity.PaymentStatus.PAID).toList();
    }
    public String getEmailStatusLabel() {
        return switch (emailStatus == null ? "PENDING" : emailStatus) {
            case "SENT" -> "Đã gửi email xác nhận";
            case "NOT_CONFIGURED" -> "Chưa cấu hình gửi email xác nhận";
            case "FAILED" -> "Gửi email thất bại — có thể thử lại";
            default -> "Đang chờ gửi email";
        };
    }
}
