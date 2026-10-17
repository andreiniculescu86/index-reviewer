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

public interface ReviewSwaggerDocs {

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Tag(name = "Reviews", description = "Run index reviews and inspect their results, traces and audit records.")
    @interface Api {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Run a review using uploaded CSV files",
        description = "Runs the review of the selected index for the given cut-off and review dates."
            + "Currently, only the SMI index is allowed. Files are mandatory to be uploaded.")
    @ApiResponse(responseCode = "201", description = "Review executed and stored. The Location header points to it.")
    @ApiResponse(responseCode = "400", description = "A file part is missing, empty or not a .csv file, the request "
        + "part is malformed, or the cut-off date is after the review date.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "An uploaded file exceeds the configured size limit.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Index is not configured.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface RunReviewWithUpload {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "List review runs",
        description = "Returns a summary of stored review runs. Both filters are optional.")
    @Parameter(name = "indexId", in = ParameterIn.QUERY, description = "Only runs of this index.", example = "SMI")
    @Parameter(name = "cutoffDate", in = ParameterIn.QUERY, description = "Only runs with this cut-off date.",
        example = "2026-09-10")
    @Parameter(name = "reviewDate", in = ParameterIn.QUERY, description = "Only runs with this review date.",
        example = "2026-09-21")
    @ApiResponse(responseCode = "200", description = "Matching review runs, possibly empty.")
    @interface ListReviews {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(summary = "Get a review report",
        description = "Final constituents, weighting factors, joiners, leavers, review status and findings.")
    @Parameter(name = "reviewId", in = ParameterIn.PATH, description = "Review run id.")
    @ApiResponse(responseCode = "200", description = "Review report.")
    @ApiResponse(responseCode = "404", description = "Unknown review id.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
    @interface GetReview {
    }
}
