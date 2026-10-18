package com.example.indexreviewer.config.swagger;

import com.example.indexreviewer.dto.response.error.ErrorResponse;
import com.example.indexreviewer.dto.response.review.ReviewResponse;
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

public interface ReviewSwaggerDocs {

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Reviews", description = "Run index reviews and inspect their results, traces and audit records.")
    @interface Api {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Run a review using uploaded CSV files",
        description = "Runs the review of the selected index for the given cut-off and review dates. "
            + "Currently, only the SMI index is allowed. Files are mandatory to be uploaded. A review that fails on "
            + "data errors is stored as well; its findings are available from the trace endpoint. A review that stops "
            + "on an unexpected error is stored with status ERROR, and the request ends with an internal error.")
    @ApiResponse(responseCode = "201", description = "Review executed and stored. The body holds the review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReviewResponse.class)))
    @ApiResponse(responseCode = "400", description = "A file part is missing, empty or not a .csv file, the request "
        + "part is malformed, or the cut-off date is after the review date.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "An uploaded file exceeds the configured size limit.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "The review failed on data errors: the execution is stored, the "
        + "body is the report with the review id, status FAILED and empty lists. When the index is not configured, "
        + "the body is an ErrorResponse and nothing is stored.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(oneOf = {ReviewResponse.class, ErrorResponse.class})))
    @interface RunReviewWithUpload {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "List review executions",
        description = "Returns a summary of stored review executions. Both filters are optional.")
    @Parameter(name = "indexId", in = ParameterIn.QUERY, description = "Only executions of this index.", example = "SMI")
    @Parameter(name = "cutoffDate", in = ParameterIn.QUERY, description = "Only executions with this cut-off date.",
        example = "2026-09-10")
    @Parameter(name = "reviewDate", in = ParameterIn.QUERY, description = "Only executions with this review date.",
        example = "2026-09-21")
    @ApiResponse(responseCode = "200", description = "Matching review executions, possibly empty.")
    @interface ListReviews {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get a review report",
        description = "Final constituents with their weighting factors, joiners, leavers and review status.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review execution id.")
    @ApiResponse(responseCode = "200", description = "Review report.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ReviewResponse.class)))
    @ApiResponse(responseCode = "400", description = "The review id is not a UUID.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReview {
    }
}
