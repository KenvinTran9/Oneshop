package com.oneshop.entity;

import jakarta.persistence.*;

/** Audit trail for receipts and manual corrections; order reservations remain in order history. */
@Entity
@Table(name = "stock_movements", uniqueConstraints = @UniqueConstraint(columnNames = "request_token"))
public class StockMovement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false) private Store store;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(nullable = false) private Product product;
    @Column(nullable = false, length = 120) private String actorEmail;
    @Column(nullable = false, length = 300) private String reason;
    @Column(nullable = false, length = 40) private String requestToken;
    @Column(nullable = false) private int delta;
    @Column(nullable = false) private int quantityBefore;
    @Column(nullable = false) private int quantityAfter;
    public Store getStore() { return store; }
    public void setStore(Store v) { store = v; }
    public Product getProduct() { return product; }
    public void setProduct(Product v) { product = v; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String v) { actorEmail = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String v) { requestToken = v; }
    public int getDelta() { return delta; }
    public void setDelta(int v) { delta = v; }
    public int getQuantityBefore() { return quantityBefore; }
    public void setQuantityBefore(int v) { quantityBefore = v; }
    public int getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(int v) { quantityAfter = v; }
}
