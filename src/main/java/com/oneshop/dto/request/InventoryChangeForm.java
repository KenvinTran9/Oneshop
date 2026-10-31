package com.oneshop.dto.request;

import jakarta.validation.constraints.*;
public class InventoryChangeForm {
    @NotNull private Long storeId;
    @NotNull private Long productId;
    @NotNull @Min(-100000) @Max(100000) private Integer delta;
    @NotBlank @Size(max = 300) private String reason;
    @NotBlank @Pattern(regexp = "[a-fA-F0-9-]{36}") private String requestToken;
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { storeId = v; }
    public Long getProductId() { return productId; }
    public void setProductId(Long v) { productId = v; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer v) { delta = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String v) { requestToken = v; }
}
