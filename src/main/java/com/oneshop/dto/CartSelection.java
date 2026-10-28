package com.oneshop.dto;

import java.io.Serializable;

/** A server-side cart line identity, also safe to keep in an HTTP session. */
public record CartSelection(Long productId, String size, String shade) implements Serializable {
    public CartSelection {
        size = size == null ? "" : size.trim();
        shade = shade == null ? "" : shade.trim();
    }
}
