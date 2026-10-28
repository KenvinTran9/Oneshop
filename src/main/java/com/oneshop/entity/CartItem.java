package com.oneshop.entity;

import org.springframework.data.mongodb.core.mapping.DocumentReference;

public class CartItem {

    @DocumentReference
    private Product product;

    private int quantity;

    private String selectedSize = "";

    private String selectedShade = "";

    public String getSelectedSize() { return selectedSize; }
    public void setSelectedSize(String selectedSize) { this.selectedSize = selectedSize; }
    public String getSelectedShade() { return selectedShade; }
    public void setSelectedShade(String selectedShade) { this.selectedShade = selectedShade; }

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
}
