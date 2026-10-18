package com.example.indexreviewer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * Rejects separators the CSV parser cannot use: its quote and escape characters, the null character and line breaks.
 * A missing separator is reported by its own constraint.
 */
public class CsvSeparatorValidator implements ConstraintValidator<CsvSeparator, Character> {

    private static final Set<Character> UNSUPPORTED_SEPARATORS = Set.of('"', '\\', '\0', '\r', '\n');

    @Override
    public boolean isValid(Character separator, ConstraintValidatorContext context) {
        return separator == null || !UNSUPPORTED_SEPARATORS.contains(separator);
    }
}
