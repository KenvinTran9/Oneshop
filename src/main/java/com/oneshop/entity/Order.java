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

    @Enumerated(EnumType.STRING) @Column(length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.COD;
    @Enumerated(EnumType.STRING) @Column(length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private java.time.LocalDateTime paidAt;
    @Column(length = 120) private String paymentReference;
    @Column(length = 120) private String paymentCheckedBy;
    @Column(length = 120) private String refundReference;
    @Column(length = 100) private String bankName;
    @Column(length = 34) private String bankAccountNumber;
    @Column(length = 120) private String bankAccountHolder;
    @Column(length = 300) private String bankQrImage;
    @Column(length = 100) private String carrier;
    @Column(length = 100) private String shipperName;
    @Column(length = 20) private String shipperPhone;
    @Column(length = 100) private String trackingCode;
    private java.time.LocalDate estimatedDelivery;
    @Column(length = 300) private String deliveryNote;

    public PaymentMethod getPaymentMethod() { return paymentMethod == null ? PaymentMethod.COD : paymentMethod; }
    public void setPaymentMethod(PaymentMethod v) { paymentMethod = v; }
    public PaymentStatus getPaymentStatus() {
        return paymentStatus == null ? (getPaymentMethod() == PaymentMethod.COD && status == OrderStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.UNPAID) : paymentStatus;
    }
    public void setPaymentStatus(PaymentStatus v) { paymentStatus = v; }
    public java.time.LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(java.time.LocalDateTime v) { paidAt = v; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String v) { paymentReference = v; }
    public String getPaymentCheckedBy() { return paymentCheckedBy; }
    public void setPaymentCheckedBy(String v) { paymentCheckedBy = v; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String v) { refundReference = v; }
    public String getBankName() { return bankName; }
    public void setBankName(String v) { bankName = v; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String v) { bankAccountNumber = v; }
    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String v) { bankAccountHolder = v; }
    public String getBankQrImage() { return bankQrImage; }
    public void setBankQrImage(String v) { bankQrImage = v; }
    public String getCarrier() { return carrier; }
    public void setCarrier(String v) { carrier = v; }
    public String getShipperName() { return shipperName; }
    public void setShipperName(String v) { shipperName = v; }
    public String getShipperPhone() { return shipperPhone; }
    public void setShipperPhone(String v) { shipperPhone = v; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String v) { trackingCode = v; }
    public java.time.LocalDate getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(java.time.LocalDate v) { estimatedDelivery = v; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String v) { deliveryNote = v; }

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
