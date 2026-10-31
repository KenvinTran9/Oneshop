package com.oneshop.entity;

public enum OrderStatus {
    NEW("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PACKING("Đang chuẩn bị"),
    SHIPPING("Đang giao hàng"),
    COMPLETED("Đã giao hàng · Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
    public java.util.List<OrderStatus> getNextStatuses() {
        return switch (this) {
            case NEW -> java.util.List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> java.util.List.of(PACKING, CANCELLED);
            case PACKING -> java.util.List.of(SHIPPING, CANCELLED);
            case SHIPPING -> java.util.List.of(COMPLETED);
            default -> java.util.List.of();
        };
    }
}
