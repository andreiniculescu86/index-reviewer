package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
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

    public List<Finding> validateCutoff(Integer securityId, Security row) {
        if (row == null || row.getPrice() == null) {
            return List.of(Finding.builder()
                .code(FindingCode.MISSING_CUTOFF_PRICE)
                .securityId(securityId)
                .field("price")
                .message("Security %s excluded: no price on the cut-off date".formatted(securityId))
                .build());
        }
        List<Finding> findings = new ArrayList<>();
        requireValidValue(securityId, "price", "Price", row.getPrice(), findings);
        return findings;
    }

    public List<Finding> validateReview(Integer securityId, Security row) {
        if (row == null) {
            return List.of(Finding.builder()
                .code(FindingCode.MISSING_REVIEW_DATA)
                .securityId(securityId)
                .message("Security %s excluded: no shares and free float on the review date".formatted(securityId))
                .build());
        }
        List<Finding> findings = new ArrayList<>();
        requireValidValue(securityId, "shares", "Shares", row.getShares(), findings);
        requireValidValue(securityId, "freeFloat", "Free float", row.getFreeFloat(), findings);
        if (row.getFreeFloat() != null && row.getFreeFloat().compareTo(BigDecimal.ONE) > 0) {
            findings.add(invalidValue(securityId, "freeFloat",
                "Security %s: free float must be at most 1 but is %s".formatted(securityId, row.getFreeFloat())));
        }
        return findings;
    }

    public Finding checkDuplicate(Security first, Security second, LocalDate date) {
        Integer securityId = first.getSecurityId();
        if (!sameValues(first, second)) {
            return Finding.builder()
                .code(FindingCode.CONFLICTING_DUPLICATE)
                .securityId(securityId)
                .field("date")
                .message("Security %s: conflicting security data rows for %s".formatted(securityId, date))
                .build();
        }
        return Finding.builder()
            .code(FindingCode.DUPLICATE_ROW)
            .securityId(securityId)
            .field("date")
            .message("Security %s has duplicate security data rows for %s, counted once".formatted(securityId, date))
            .build();
    }

    private static void requireValidValue(Integer securityId, String field, String name, BigDecimal value,
                                          List<Finding> findings) {
        if (value == null || value.signum() <= 0) {
            findings.add(invalidValue(securityId, field,
                "Security %s: %s must be positive but is %s".formatted(securityId, name, value)));
        } else if (value.scale() > MAX_DIGITS || value.precision() - value.scale() > MAX_DIGITS) {
            findings.add(invalidValue(securityId, field,
                "Security %s: %s must have at most %d integer digits and %d decimal places"
                    .formatted(securityId, name, MAX_DIGITS, MAX_DIGITS)));
        }
    }

    private static Finding invalidValue(Integer securityId, String field, String message) {
        return Finding.builder()
            .code(FindingCode.INVALID_VALUE)
            .securityId(securityId)
            .field(field)
            .message(message)
            .build();
    }

    private static boolean sameValues(Security first, Security second) {
        return sameValue(first.getPrice(), second.getPrice())
            && sameValue(first.getShares(), second.getShares())
            && sameValue(first.getFreeFloat(), second.getFreeFloat());
    }

    // compareTo instead of equals, so that 0.5 and 0.50 count as the same value
    private static boolean sameValue(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
}
