package com.example.indexreviewer.dto.response.review;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Calculation trace of a stored review execution: how the result was reached.")
public record ReviewTraceResponse(
    UUID reviewId,
    IndexType indexType,
    LocalDate cutoffDate,
    LocalDate reviewDate,
    ReviewStatus status,
    @Schema(description = "One trace per universe member and per existing index component, ordered by security id.")
    List<SecurityTraceResponse> securities,
    List<FindingResponse> findings) {
}
