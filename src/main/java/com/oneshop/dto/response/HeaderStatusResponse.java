package com.oneshop.dto.response;

public record HeaderStatusResponse(boolean authenticated, String email, boolean isAdmin, long itemCount) { }
