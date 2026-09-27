package com.oneshop.exception;

public class ProductNotFoundException extends IllegalArgumentException {

    public ProductNotFoundException(String slug) {
        super("Product not found: " + slug);
    }
}
