package com.oneshop.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Không thể đăng ký bằng email này. Hãy kiểm tra lại hoặc đăng nhập.");
    }
}
