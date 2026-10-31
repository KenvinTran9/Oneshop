package com.oneshop.entity;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    REPORTED("Khách đã báo chuyển khoản — chờ đối soát"),
    PAID("Đã thanh toán"),
    REFUND_PENDING("Chờ xử lý hoàn tiền"),
    REFUNDED("Đã hoàn tiền");

    private final String label;
    PaymentStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
