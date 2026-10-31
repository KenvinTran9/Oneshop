package com.oneshop.dto.request;
import jakarta.validation.constraints.*;
public record ProfileForm(@NotBlank @Size(max=120) String fullName,@NotBlank @Pattern(regexp="[0-9+() .-]{8,20}") String phone){}
