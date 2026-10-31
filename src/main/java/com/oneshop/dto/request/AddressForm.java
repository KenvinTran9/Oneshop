package com.oneshop.dto.request;
import jakarta.validation.constraints.*;
public record AddressForm(@NotBlank @Size(max=60) String label,@NotBlank @Size(max=120) String recipient,
        @NotBlank @Pattern(regexp="[0-9+() .-]{8,20}") String phone,@NotBlank @Size(max=255) String address){}
