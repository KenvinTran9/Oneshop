package com.oneshop.validation;

import com.oneshop.dto.request.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConfirmationValidator
        implements ConstraintValidator<PasswordConfirmation, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null
                || request.password() == null
                || request.confirmPassword() == null
                || request.confirmPassword().isBlank()) {
            return true;
        }

        if (request.password().equals(request.confirmPassword())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
