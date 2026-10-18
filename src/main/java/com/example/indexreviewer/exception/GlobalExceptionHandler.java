package com.example.indexreviewer.exception;

import com.example.indexreviewer.dto.response.error.Error;
import com.example.indexreviewer.dto.response.error.ErrorCode;
import com.example.indexreviewer.dto.response.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Map<String, ErrorCode> CONSTRAINT_CODES = Map.of(
        "NotNull", ErrorCode.MISSING_VALUE,
        "CsvFile", ErrorCode.INVALID_FILE,
        "ValidReviewDates", ErrorCode.INVALID_DATE_RANGE);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error while handling a request", ex);
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Unexpected error. Check the server logs.");
        return createResponseEntity(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * An uploaded CSV file cannot be parsed: a required header is missing, a value has the wrong type or a line
     * is malformed. The field names the upload part, e.g. universe.
     */
    @ExceptionHandler(InvalidCsvFileException.class)
    public ResponseEntity<Object> handleInvalidCsvFile(InvalidCsvFileException ex, WebRequest request) {
        Error error = new Error(ErrorCode.INVALID_FILE, ex.getPart(), null, ex.getMessage());
        return createResponseEntity(new ErrorResponse(List.of(error)), new HttpHeaders(), HttpStatus.BAD_REQUEST,
            request);
    }

    /**
     * A review id, or a security id within a review, does not exist.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        Error error = new Error(ErrorCode.NOT_FOUND, ex.getField(), ex.getValue(), ex.getMessage());
        return createResponseEntity(new ErrorResponse(List.of(error)), new HttpHeaders(), HttpStatus.NOT_FOUND,
            request);
    }

    /**
     * The requested index type has no review implementation.
     */
    @ExceptionHandler(IndexNotConfiguredException.class)
    public ResponseEntity<Object> handleIndexNotConfigured(IndexNotConfiguredException ex, WebRequest request) {
        Error error = new Error(ErrorCode.INDEX_NOT_CONFIGURED, "indexType", String.valueOf(ex.getIndexType()),
            ex.getMessage());
        return createResponseEntity(new ErrorResponse(List.of(error)), new HttpHeaders(),
            HttpStatus.UNPROCESSABLE_CONTENT, request);
    }

    /**
     * Bean Validation failures on controller parameters, e.g. a file part that is empty or not a .csv file
     * ({@code @CsvFile}), a null config field ({@code @NotNull}) or a cut-off date after the review date
     * ({@code @ValidReviewDates}). Collects every failed check into one response.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<Error> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameter = parameterName(result.getMethodParameter());
            if (result instanceof ParameterErrors parameterErrors) {
                errors.addAll(bindingErrors(parameter, parameterErrors));
            } else {
                String value = displayValue(result.getArgument());
                result.getResolvableErrors().forEach(error -> errors.add(
                    new Error(codeFor(error), parameter, value, error.getDefaultMessage())));
            }
        }
        return handleExceptionInternal(ex, new ErrorResponse(errors), headers, status, request);
    }

    /**
     * Bean Validation failures on a {@code @Valid} request object when Spring validates it on its own, without
     * method validation. Kept as a fallback so these errors use the same response format.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<Error> errors = bindingErrors(parameterName(ex.getParameter()), ex.getBindingResult());
        return handleExceptionInternal(ex, new ErrorResponse(errors), headers, status, request);
    }

    /**
     * A required multipart part is missing, i.e. the universe, securityData or composition CSV file was not
     * uploaded. Spring reports only the first missing part.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
        MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String part = ex.getRequestPartName();
        Error error = new Error(ErrorCode.MISSING_FILE, part, null,
            "Required CSV file '%s' is missing. Upload it as a file in the multipart part '%s'.".formatted(part, part));
        return handleExceptionInternal(ex, new ErrorResponse(List.of(error)), headers, status, request);
    }

    /**
     * The JSON in the configRequest part or the request body cannot be read: malformed JSON, an unknown
     * index type or a date that is not in ISO format. Jackson stops at the first invalid value.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ErrorResponse body = new ErrorResponse(List.of(unreadableJsonError(ex)));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * A query or path parameter cannot be converted to its type, e.g. an unknown indexType or a cutoffDate that
     * is not an ISO date in GET /v1/reviews.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
            : ex.getPropertyName();
        Error error = new Error(ErrorCode.INVALID_VALUE, name, String.valueOf(ex.getValue()),
            expectedType(ex.getRequiredType()));
        return handleExceptionInternal(ex, new ErrorResponse(List.of(error)), headers, status, request);
    }

    /**
     * Last step for every error handled by this class. Converts any remaining Spring ProblemDetail body into an
     * ErrorResponse and fills in status, title and the request path.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
        Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        ErrorResponse errorResponse = toErrorResponse(body, statusCode);
        errorResponse.setStatus(statusCode.value());
        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        errorResponse.setTitle(httpStatus != null ? httpStatus.getReasonPhrase() : null);
        if (request instanceof ServletWebRequest servletRequest) {
            errorResponse.setInstance(servletRequest.getRequest().getRequestURI());
        }
        return new ResponseEntity<>(errorResponse, headers, statusCode);
    }

    private static ErrorResponse toErrorResponse(Object body, HttpStatusCode statusCode) {
        if (body instanceof ErrorResponse errorResponse) {
            return errorResponse;
        }
        ErrorCode code = statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_REQUEST;
        String detail = body instanceof ProblemDetail problem ? problem.getDetail() : null;
        return ErrorResponse.of(code, detail);
    }

    private static List<Error> bindingErrors(String parameter, Errors errors) {
        List<Error> result = new ArrayList<>();
        for (FieldError error : errors.getFieldErrors()) {
            result.add(new Error(codeFor(error), parameter + "." + error.getField(),
                displayValue(error.getRejectedValue()), error.getDefaultMessage()));
        }
        errors.getGlobalErrors().forEach(error -> result.add(
            new Error(codeFor(error), parameter, null, error.getDefaultMessage())));
        return result;
    }

    private static ErrorCode codeFor(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        String constraint = codes == null || codes.length == 0 ? "" : codes[codes.length - 1];
        return CONSTRAINT_CODES.getOrDefault(constraint, ErrorCode.INVALID_VALUE);
    }

    private static Error unreadableJsonError(HttpMessageNotReadableException ex) {
        UnrecognizedPropertyException unknownField = findCause(ex, UnrecognizedPropertyException.class);
        if (unknownField != null) {
            return new Error(ErrorCode.INVALID_VALUE, jsonPath(unknownField), unknownField.getPropertyName(),
                "unknown field; allowed: " + unknownField.getKnownPropertyIds().stream()
                    .map(Object::toString)
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        InvalidFormatException invalidFormat = findCause(ex, InvalidFormatException.class);
        if (invalidFormat != null) {
            return new Error(ErrorCode.INVALID_VALUE, jsonPath(invalidFormat), String.valueOf(invalidFormat.getValue()),
                expectedType(invalidFormat.getTargetType()));
        }
        MismatchedInputException mismatchedInput = findCause(ex, MismatchedInputException.class);
        if (mismatchedInput != null && mismatchedInput.getTargetType() != null) {
            return new Error(ErrorCode.INVALID_VALUE, jsonPath(mismatchedInput), null,
                expectedType(mismatchedInput.getTargetType()));
        }
        StreamReadException malformed = findCause(ex, StreamReadException.class);
        if (malformed != null && malformed.getLocation() != null) {
            return new Error(ErrorCode.MALFORMED_JSON, "malformed JSON at line %d, column %d"
                .formatted(malformed.getLocation().getLineNr(), malformed.getLocation().getColumnNr()));
        }
        return new Error(ErrorCode.MALFORMED_JSON, "request body or JSON part could not be read");
    }


    private static String expectedType(Class<?> type) {
        if (type == null) {
            return "unexpected value type";
        }
        if (type.isEnum()) {
            return "allowed values: " + Arrays.stream(type.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        }
        if (LocalDate.class.equals(type)) {
            return "expected an ISO date (yyyy-MM-dd)";
        }
        return "expected type " + type.getSimpleName();
    }

    private static String displayValue(Object value) {
        if (value instanceof MultipartFile file) {
            return file.getOriginalFilename();
        }
        return value == null ? null : String.valueOf(value);
    }

    private static String jsonPath(JacksonException ex) {
        List<JacksonException.Reference> path = ex.getPath();
        StringBuilder field = new StringBuilder(rootName(path));
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                field.append('.').append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                field.append('[').append(reference.getIndex()).append(']');
            }
        }
        return field.toString();
    }

    private static String rootName(List<JacksonException.Reference> path) {
        if (path.isEmpty() || path.getFirst().from() == null) {
            return "body";
        }
        Object root = path.getFirst().from();
        String simpleName = (root instanceof Class<?> type ? type : root.getClass()).getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }

    private static String parameterName(MethodParameter parameter) {
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        if (requestPart != null) {
            String name = requestPart.name().isEmpty() ? requestPart.value() : requestPart.name();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return parameter.getParameterName();
    }
}
