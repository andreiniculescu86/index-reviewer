package com.example.indexreviewer.dto.response.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"dateTime", "status", "title", "instance", "errors"})
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTime = LocalDateTime.now();

    @Setter
    private int status;

    @Setter
    private String title;

    @Setter
    private String instance;

    private List<Error> errors;

    public ErrorResponse(List<Error> errors) {
        this.errors = List.copyOf(errors);
    }

    /**
     * Get simplified error response with 1 error from code and message.
     *
     * @param code    the application error code, i.e. INVALID_FILE
     * @param message the exact application error message, developer/debug purpose only
     * @return the new ErrorResponse message
     */
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(List.of(new Error(code, message)));
    }
}
