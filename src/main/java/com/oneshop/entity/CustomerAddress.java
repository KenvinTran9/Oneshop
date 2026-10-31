package com.oneshop.entity;
import jakarta.persistence.*;
@Entity
@Table(name="customer_addresses")
public class CustomerAddress extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private User customer;
    @Column(nullable=false,length=60) private String label;
    @Column(nullable=false,length=120) private String recipient;
    @Column(nullable=false,length=20) private String phone;
    @Column(nullable=false,length=255) private String address;
    public User getCustomer(){return customer;} public void setCustomer(User v){customer=v;}
    public String getLabel(){return label;} public void setLabel(String v){label=v;}
    public String getRecipient(){return recipient;} public void setRecipient(String v){recipient=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
}
