package com.example.indexreviewer.config.swagger;

import com.example.indexreviewer.dto.response.error.ErrorResponse;
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
    @Operation(summary = "Get the calculation trace details of a specific review ID execution", description = "")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review run id.")
    @ApiResponse(responseCode = "200", description = "Review trace.")
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewTrace {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get the trace of one security in a review",
        description = "Explains why the security is in or out of the index and how its weight was derived.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review run id.")
    @Parameter(name = "securityId", in = ParameterIn.PATH, description = "Security id.", example = "101")
    @ApiResponse(responseCode = "200", description = "Security trace.")
    @ApiResponse(responseCode = "404", description = "Unknown review id, or the security is not part of the review.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewSecurity {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get the audit manifest of a review",
        description = "Application version, dates and their source, index definition snapshot, numeric settings "
            + "and input files with SHA-256 hashes.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review run id.")
    @ApiResponse(responseCode = "200", description = "Audit manifest.")
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewManifest {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Download an input file of a review",
        description = "Returns the stored copy of the CSV file the review was calculated from.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review run id.")
    @Parameter(name = "role", in = ParameterIn.PATH, description = "Input file role.",
        schema = @Schema(allowableValues = {"universe", "securityData", "composition"}))
    @ApiResponse(responseCode = "200", description = "Stored input file.")
    @ApiResponse(responseCode = "404", description = "Unknown review id or input role.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReviewInput {
    }
}
