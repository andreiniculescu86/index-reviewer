package com.example.indexreviewer.model.trace;

/**
 * Severity of a finding. An error fails the review, a warning is handled in a deterministic way and an info does
 * not affect the review.
 */
public enum Severity {
    ERROR,
    WARNING,
    INFO
}
