package com.ganzithon.station.user.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PinConfirmMatchValidator implements ConstraintValidator<PinConfirmMatch, UserSignupRequest> {

    @Override
    public boolean isValid(UserSignupRequest value, ConstraintValidatorContext context) {
        if (value.pin() == null || value.pinConfirm() == null || value.pin().equals(value.pinConfirm())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("pinConfirm")
                .addConstraintViolation();
        return false;
    }
}
