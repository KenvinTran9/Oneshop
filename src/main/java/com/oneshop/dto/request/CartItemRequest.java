package com.oneshop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartItemRequest(
        @NotNull Long productId,
        @Min(1) @Max(99) int quantity,
        @Size(max = 100) String size,
        @Size(max = 100) String shade
) {
}
