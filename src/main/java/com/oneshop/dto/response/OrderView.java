package com.oneshop.dto.response;

import com.oneshop.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderView(String code, OrderStatus status, LocalDateTime createdAt, String customerName,
        String customerEmail, String customerPhone, String shippingAddress, String storeName, String note,
        BigDecimal subtotal, BigDecimal shippingFee, BigDecimal totalAmount, List<Item> items,
        List<Event> events, String emailStatus, LocalDateTime emailSentAt) {
    public record Item(String name, String imageUrl, String size, String shade, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal) { }
    public record Event(OrderStatus status, String message, LocalDateTime time) { }
    public boolean isCustomerCancellable() { return status == OrderStatus.NEW; }
    public String getEmailStatusLabel() {
        return switch (emailStatus == null ? "PENDING" : emailStatus) {
            case "SENT" -> "Đã gửi email xác nhận";
            case "NOT_CONFIGURED" -> "Chưa cấu hình gửi email xác nhận";
            case "FAILED" -> "Gửi email thất bại — có thể thử lại";
            default -> "Đang chờ gửi email";
        };
    }
}
