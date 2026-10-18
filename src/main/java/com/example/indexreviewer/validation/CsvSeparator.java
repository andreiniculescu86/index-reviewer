package com.example.indexreviewer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CsvSeparatorValidator.class)
public @interface CsvSeparator {

    String message() default "must not be a quote, a backslash, a line break or the null character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
