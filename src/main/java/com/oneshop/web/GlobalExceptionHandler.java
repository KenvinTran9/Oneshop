package com.oneshop.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String notFound(IllegalArgumentException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "pages/not-found";
    }
}
