package com.example.indexreviewer.model.trace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Type of a finding. Each code has a fixed severity, so which data problem stops a review is defined in one place.
 */
@Getter
@RequiredArgsConstructor
public enum FindingCode {
    MISSING_DATE(Severity.ERROR),
    COMPOSITION_SIZE(Severity.ERROR),
    TOO_FEW_ELIGIBLE(Severity.ERROR),
    INVALID_VALUE(Severity.ERROR),
    CONFLICTING_DUPLICATE(Severity.ERROR),
    SELECTION_SIZE(Severity.ERROR),
    INVALID_WEIGHTS(Severity.ERROR),
    MISSING_CUTOFF_PRICE(Severity.WARNING),
    MISSING_REVIEW_DATA(Severity.WARNING),
    DUPLICATE_ROW(Severity.WARNING),
    EXISTING_COMPONENT_NOT_IN_UNIVERSE(Severity.WARNING),
    SELECTION_TIE(Severity.WARNING),
    IGNORED_SECURITIES(Severity.INFO);

    private final Severity severity;
}
