package com.example.indexreviewer.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidReviewDataException extends RuntimeException {

    private final List<String> errors;

    public InvalidReviewDataException(List<String> errors) {
        super(errors.size() + " invalid review data values: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }
}
