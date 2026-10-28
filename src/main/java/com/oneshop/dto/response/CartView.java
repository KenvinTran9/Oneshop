package com.oneshop.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartView(List<CartItemView> items, BigDecimal subtotal, long itemCount) {
}
