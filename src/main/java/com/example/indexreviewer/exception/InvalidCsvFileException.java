package com.example.indexreviewer.exception;

import lombok.Getter;

@Getter
public class InvalidCsvFileException extends RuntimeException {

    private final String part;

    public InvalidCsvFileException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public InvalidCsvFileException(String part, String message, Throwable cause) {
        super(message, cause);
        this.part = part;
    }
}
