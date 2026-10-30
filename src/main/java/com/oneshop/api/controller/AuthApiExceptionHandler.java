package com.oneshop.api.controller;

import io.jsonwebtoken.JwtException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthApiController.class)
public class AuthApiExceptionHandler {
    @ExceptionHandler({AuthenticationException.class, JwtException.class, IllegalArgumentException.class})
    ResponseEntity<?> rejectedCredentials(Exception failure) {
        return ResponseEntity.status(401).body(Map.of("message", "Thông tin xác thực không hợp lệ."));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<?> invalidRequest(Exception failure) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu đăng nhập không hợp lệ."));
    }
}
