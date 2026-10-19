package com.example.indexreviewer.config.swagger;

import com.example.indexreviewer.dto.response.error.ErrorResponse;
import com.example.indexreviewer.dto.response.review.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
            + "Only indices configured in application.yml are allowed, currently SMI. Files are mandatory to be "
            + "uploaded. A review that fails on "
            + "data errors is stored as well. The response lists the review's findings. A review that stops on an "
            + "unexpected error is stored with status ERROR, and the request ends with an internal error.")
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
        + "body is the report with the review id, status FAILED, the findings and empty composition lists. When the "
        + "index is not configured, the body is an ErrorResponse and nothing is stored.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(oneOf = {ReviewResponse.class, ErrorResponse.class})))
    @interface RunReviewWithUpload {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "List review executions",
        description = "Returns the reports of the stored review executions, newest first, in the same model as "
            + "getting one review. All filters are optional and combined.")
    @Parameter(name = "indexType", in = ParameterIn.QUERY, description = "Only executions of this index.",
        example = "SMI")
    @Parameter(name = "cutoffDate", in = ParameterIn.QUERY, description = "Only executions with this cut-off date.",
        example = "2026-09-10")
    @Parameter(name = "reviewDate", in = ParameterIn.QUERY, description = "Only executions with this review date.",
        example = "2026-09-21")
    @ApiResponse(responseCode = "200", description = "Matching review executions, possibly empty.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class))))
    @ApiResponse(responseCode = "400", description = "A filter value is invalid: unknown index or a date that is not "
        + "ISO (yyyy-MM-dd).",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface ListReviews {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get a review report",
        description = "Final constituents with their weighting factors, joiners, leavers, review status and findings.")
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
