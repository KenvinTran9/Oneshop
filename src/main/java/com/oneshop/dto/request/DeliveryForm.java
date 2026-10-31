package com.oneshop.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record DeliveryForm(
        @Size(max = 100, message = "Đơn vị giao hàng tối đa 100 ký tự.") String carrier,
        @Size(max = 100, message = "Tên shipper tối đa 100 ký tự.") String shipperName,
        @Pattern(regexp = "(?:|0[0-9]{9}|\\+84[0-9]{9})", message = "Số điện thoại shipper chưa hợp lệ.") String shipperPhone,
        @Size(max = 100, message = "Mã vận đơn tối đa 100 ký tự.") String trackingCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate estimatedDelivery,
        @Size(max = 300, message = "Ghi chú giao hàng tối đa 300 ký tự.") String deliveryNote,
        @NotNull Long expectedVersion) {
    @AssertTrue(message = "Vui lòng nhập cả tên và số điện thoại shipper, hoặc để trống cả hai.")
    public boolean isShipperComplete() {
        return (shipperName == null || shipperName.isBlank()) == (shipperPhone == null || shipperPhone.isBlank());
    }
}
