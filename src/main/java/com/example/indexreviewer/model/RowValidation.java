package com.example.indexreviewer.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result of one validation step, e.g. one security data row or the review dates.
 */
@Getter
public class RowValidation {

    private final List<String> errors;
    private final List<String> warnings;

    public RowValidation(List<String> errors, List<String> warnings) {
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    public RowValidation() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean hasNoIssues() {
        return !hasErrors() && !hasWarnings();
    }
}
