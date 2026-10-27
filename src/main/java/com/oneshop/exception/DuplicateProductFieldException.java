package com.oneshop.exception;

public class DuplicateProductFieldException extends RuntimeException {

    private final String field;

    public DuplicateProductFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
