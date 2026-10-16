package com.example.indexreviewer.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CsvFileValidator.class)
public @interface CsvFile {

    String message() default "must be a non-empty .csv file";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
