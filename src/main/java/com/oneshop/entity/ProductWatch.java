package com.oneshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="product_watches", uniqueConstraints=@UniqueConstraint(name="uk_watch_user_product",columnNames={"user_id","product_id"}))
public class ProductWatch extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private Product product;
    @Column(nullable=false,length=36) private String deliveryId;
    @Column(nullable=false) private boolean emailEnabled;
    private LocalDateTime availableAt;
    private LocalDateTime readAt;
    private LocalDateTime notifiedAt;
    public boolean isEmailEnabled(){return emailEnabled;} public void setEmailEnabled(boolean value){emailEnabled=value;}
    public LocalDateTime getAvailableAt(){return availableAt;} public void setAvailableAt(LocalDateTime value){availableAt=value;}
    public LocalDateTime getReadAt(){return readAt;} public void setReadAt(LocalDateTime value){readAt=value;}
    private LocalDateTime lastAttemptAt;
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public Product getProduct(){return product;} public void setProduct(Product value){product=value;}
    public String getDeliveryId(){return deliveryId;} public void setDeliveryId(String value){deliveryId=value;}
    public LocalDateTime getNotifiedAt(){return notifiedAt;} public void setNotifiedAt(LocalDateTime value){notifiedAt=value;}
    public LocalDateTime getLastAttemptAt(){return lastAttemptAt;} public void setLastAttemptAt(LocalDateTime value){lastAttemptAt=value;}
}
