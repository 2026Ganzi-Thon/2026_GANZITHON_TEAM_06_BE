package com.ganzithon.station.user.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** pin과 pinConfirm이 같은지 검사합니다. 실패 시 fieldErrors의 field는 "pinConfirm"으로 내려갑니다. */
@Documented
@Constraint(validatedBy = PinConfirmMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PinConfirmMatch {

    String message() default "PIN 확인이 일치하지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
