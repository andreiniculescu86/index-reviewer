package com.example.indexreviewer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReviewDatesValidator.class)
public @interface ValidReviewDates {

    String message() default "cutoffDate must not be after reviewDate";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
