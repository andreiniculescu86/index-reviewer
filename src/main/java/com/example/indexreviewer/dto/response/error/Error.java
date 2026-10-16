package com.example.indexreviewer.dto.response.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"code", "field", "value", "message"})
public class Error {

    /**
     * Mandatory application error code identifying the error, i.e. INVALID_FILE.
     */
    @NonNull
    private ErrorCode code;

    /**
     * Optional name of the field or request part that caused the error.
     */
    private String field;

    /**
     * Optional value that caused the error.
     */
    private String value;

    /**
     * Optional human readable message, used for logs or debugging.
     */
    private String message;

    public Error(ErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }
}
