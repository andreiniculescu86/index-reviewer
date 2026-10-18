package com.example.indexreviewer.exception;

import lombok.Getter;

/**
 * A requested resource does not exist. The field and value name the identifier that was not found.
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final String field;
    private final String value;

    public NotFoundException(String field, String value, String message) {
        super(message);
        this.field = field;
        this.value = value;
    }
}
