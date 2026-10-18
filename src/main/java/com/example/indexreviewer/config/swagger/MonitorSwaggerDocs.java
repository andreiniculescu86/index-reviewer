package com.example.indexreviewer.config.swagger;

import com.example.indexreviewer.dto.response.error.ErrorResponse;
import com.example.indexreviewer.dto.response.review.ReviewAuditResponse;
import com.example.indexreviewer.dto.response.review.ReviewTraceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface MonitorSwaggerDocs {

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Monitoring", description = "Various endpoints for traceability and auditability")
    @interface Api {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get the calculation trace details of a specific review ID execution",
        description = "The values used and decisions taken for every security, and all findings.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review execution id.")
    @ApiResponse(responseCode = "200", description = "Review trace.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReviewTraceResponse.class)))
    @ApiResponse(responseCode = "400", description = "The review id is not a UUID.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewTrace {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get the trace of one security in a review",
        description = "Explains why the security is in or out of the index and how its weight was derived.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review execution id.")
    @Parameter(name = "securityId", in = ParameterIn.PATH, description = "Security id.", example = "101")
    @ApiResponse(responseCode = "200", description = "The review trace with only this security and its findings. The "
        + "securities list is empty when the security has findings but no trace, e.g. duplicate rows of a security "
        + "outside the universe.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReviewTraceResponse.class)))
    @ApiResponse(responseCode = "400", description = "The review id is not a UUID or the security id not a number.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown review id, or the review has neither a trace nor a "
        + "finding for the security.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewSecurity {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get the audit data of a review",
        description = "Start time, application version and rules.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review execution id.")
    @ApiResponse(responseCode = "200", description = "Review audit data.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReviewAuditResponse.class)))
    @ApiResponse(responseCode = "400", description = "The review id is not a UUID.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewAudit {
    }
}
