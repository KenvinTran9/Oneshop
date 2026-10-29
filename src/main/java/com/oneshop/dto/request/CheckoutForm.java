package com.oneshop.dto.request;

import jakarta.validation.constraints.*;

public class CheckoutForm {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 120, message = "Họ tên tối đa 120 ký tự.")
    private String customerName;
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "(?:0[0-9]{9}|\\+84[0-9]{9})", message = "Nhập số điện thoại Việt Nam hợp lệ.")
    private String customerPhone;
    @NotBlank(message = "Vui lòng nhập địa chỉ nhận hàng.")
    @Size(min = 10, max = 255, message = "Địa chỉ từ 10 đến 255 ký tự.")
    private String shippingAddress;
    @NotNull(message = "Vui lòng chọn cửa hàng xử lý đơn.")
    @Positive
    private Long storeId;
    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
    private String note;
    @NotBlank
    @Pattern(regexp = "[a-f0-9-]{36}")
    private String checkoutToken;
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { customerName = v == null ? null : v.trim(); }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { customerPhone = v == null ? null : v.trim(); }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { shippingAddress = v == null ? null : v.trim(); }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { storeId = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v == null ? null : v.trim(); }
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String v) { checkoutToken = v; }
}
