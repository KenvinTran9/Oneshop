package com.oneshop.dto.response;

import java.math.BigDecimal;

public record CartItemView(
        Long productId,
        String sku,
        String name,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        long availableStock,
        String warning
) {
}
