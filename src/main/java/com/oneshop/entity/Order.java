package com.oneshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(unique = true, length = 36)
    private String checkoutToken;
    @Column(length = 120)
    private String customerEmail;
    @Column(precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Column(length = 500)
    private String note;
    @Column(length = 30)
    private String emailStatus = "PENDING";
    private java.time.LocalDateTime emailSentAt;
    @jakarta.persistence.OneToMany(mappedBy = "order", cascade = jakarta.persistence.CascadeType.ALL)
    @jakarta.persistence.OrderBy("id ASC")
    private java.util.List<OrderItem> items = new java.util.ArrayList<>();
    @jakarta.persistence.OneToMany(mappedBy = "order", cascade = jakarta.persistence.CascadeType.ALL)
    @jakarta.persistence.OrderBy("id ASC")
    private java.util.List<OrderEvent> events = new java.util.ArrayList<>();

    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String value) { checkoutToken = value; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String value) { customerEmail = value; }
    public BigDecimal getShippingFee() { return shippingFee == null ? BigDecimal.ZERO : shippingFee; }
    public void setShippingFee(BigDecimal value) { shippingFee = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
    public String getEmailStatus() { return emailStatus; }
    public void setEmailStatus(String value) { emailStatus = value; }
    public java.time.LocalDateTime getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(java.time.LocalDateTime value) { emailSentAt = value; }
    public java.util.List<OrderItem> getItems() { return items; }
    public java.util.List<OrderEvent> getEvents() { return events; }

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Column(nullable = false, length = 255)
    private String shippingAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
