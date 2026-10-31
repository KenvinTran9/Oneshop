package com.oneshop.dto.request;

import jakarta.validation.constraints.*;

public record PaymentReviewForm(@NotNull Action action,
        @NotBlank(message = "Nhập mã giao dịch hoặc lý do đối soát.")
        @Size(max = 120, message = "Nội dung đối soát tối đa 120 ký tự.") String reference) {
    public enum Action { CONFIRM_RECEIPT, REJECT_REPORT, CONFIRM_REFUND }
}
