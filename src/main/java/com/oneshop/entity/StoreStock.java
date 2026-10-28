package com.oneshop.entity;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@CompoundIndex(name="uk_store_product", def="{'store':1,'product':1}", unique=true)
@Document("store_stocks")
public class StoreStock extends BaseEntity {

    @DocumentReference
    private Store store;

    @DocumentReference
    private Product product;

    private int quantity;

    @Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private BigDecimal storePrice;

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
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

    public BigDecimal getStorePrice() {
        return storePrice;
    }

    public void setStorePrice(BigDecimal storePrice) {
        this.storePrice = storePrice;
    }
}
