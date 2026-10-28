package com.oneshop.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Document("order_items")
public class OrderItem extends BaseEntity {

    @DocumentReference
    private Order order;

    @DocumentReference
    private Product product;

    private int quantity;

    @Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
