package com.example.indexreviewer.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the errors and warnings of all validated rows of a review, plus infos that do not affect the review.
 */
@Getter
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    public void add(RowValidation row) {
        errors.addAll(row.getErrors());
        warnings.addAll(row.getWarnings());
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addInfo(String info) {
        infos.add(info);
    }

    public void addAll(ValidationResult other) {
        errors.addAll(other.errors);
        warnings.addAll(other.warnings);
        infos.addAll(other.infos);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
