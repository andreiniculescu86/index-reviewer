package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.RowValidation;
import com.example.indexreviewer.model.SecurityRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates security data rows and reports every problem of a row. A missing row is a warning and excludes the
 * security, an invalid value is an error.
 */
@Component
public class SecurityRecordValidator {

    private static final int MAX_DIGITS = 20;

    public RowValidation validateCutoff(Integer securityId, SecurityRecord row) {
        if (row == null || row.price() == null) {
            return new RowValidation(List.of(),
                List.of("Security %s excluded: no price on the cut-off date".formatted(securityId)));
        }
        List<String> errors = new ArrayList<>();
        requireValidValue(securityId, "Price", row.price(), errors);
        return new RowValidation(errors, List.of());
    }

    public RowValidation validateReview(Integer securityId, SecurityRecord row) {
        if (row == null) {
            return new RowValidation(List.of(),
                List.of("Security %s excluded: no shares and free float on the review date".formatted(securityId)));
        }
        List<String> errors = new ArrayList<>();
        requireValidValue(securityId, "Shares", row.shares(), errors);
        requireValidValue(securityId, "Free float", row.freeFloat(), errors);
        if (row.freeFloat() != null && row.freeFloat().compareTo(BigDecimal.ONE) > 0) {
            errors.add("Security %s: free float must be at most 1 but is %s".formatted(securityId, row.freeFloat()));
        }
        return new RowValidation(errors, List.of());
    }

    public RowValidation checkDuplicate(SecurityRecord first, SecurityRecord second, LocalDate date) {
        Integer securityId = first.securityId();
        if (!sameValues(first, second)) {
            return new RowValidation(
                List.of("Security %s: conflicting security data rows for %s".formatted(securityId, date)), List.of());
        }
        return new RowValidation(List.of(),
            List.of("Security %s has duplicate security data rows for %s, counted once".formatted(securityId, date)));
    }

    private static void requireValidValue(Integer securityId, String name, BigDecimal value, List<String> errors) {
        if (value == null || value.signum() <= 0) {
            errors.add("Security %s: %s must be positive but is %s".formatted(securityId, name, value));
        } else if (value.scale() > MAX_DIGITS || value.precision() - value.scale() > MAX_DIGITS) {
            errors.add("Security %s: %s must have at most %d integer digits and %d decimal places"
                .formatted(securityId, name, MAX_DIGITS, MAX_DIGITS));
        }
    }

    private static boolean sameValues(SecurityRecord first, SecurityRecord second) {
        return sameValue(first.price(), second.price())
            && sameValue(first.shares(), second.shares())
            && sameValue(first.freeFloat(), second.freeFloat());
    }

    // compareTo instead of equals, so that 0.5 and 0.50 count as the same value
    private static boolean sameValue(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
}
