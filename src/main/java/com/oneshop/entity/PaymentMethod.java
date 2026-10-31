package com.oneshop.entity;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
