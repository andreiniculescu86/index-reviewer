package com.example.indexreviewer.model;

import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.Severity;

import java.util.List;

/**
 * Outcome of a review. ERROR means the review stopped on an unexpected error; its trace shows how far it got.
 */
public enum ReviewStatus {
    // Declared from least to most severe; mostSevere relies on this order
    SUCCESS,
    SUCCESS_WITH_WARNINGS,
    FAILED,
    ERROR;

    /**
     * Derives the status from the findings: any error fails the review, any warning marks the result.
     */
    public static ReviewStatus of(List<Finding> findings) {
        if (contains(findings, Severity.ERROR)) {
            return FAILED;
        }
        return contains(findings, Severity.WARNING) ? SUCCESS_WITH_WARNINGS : SUCCESS;
    }

    /**
     * Returns the more severe of this status and the other one, so that a status only gets worse during a review.
     */
    public ReviewStatus getMostSevere(ReviewStatus other) {
        return compareTo(other) >= 0 ? this : other;
    }

    /**
     * Returns whether the review completed and produced an index composition.
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == SUCCESS_WITH_WARNINGS;
    }

    private static boolean contains(List<Finding> findings, Severity severity) {
        return findings.stream().anyMatch(finding -> finding.getSeverity() == severity);
    }
}
