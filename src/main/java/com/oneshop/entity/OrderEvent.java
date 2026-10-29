package com.oneshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_events")
public class OrderEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;
    @Column(nullable = false, length = 500)
    private String message;
    public Order getOrder() { return order; }
    public void setOrder(Order value) { order = value; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus value) { status = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
}
