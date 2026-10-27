package com.oneshop.exception;

public class InvalidProductReferenceException extends RuntimeException {

    private final String field;

    public InvalidProductReferenceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
