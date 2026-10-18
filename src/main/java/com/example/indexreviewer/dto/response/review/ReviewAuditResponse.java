package com.example.indexreviewer.dto.response.review;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Audit data of a stored review execution: what the review used.")
public record ReviewAuditResponse(
    UUID reviewId,
    IndexType indexType,
    LocalDate cutoffDate,
    LocalDate reviewDate,
    ReviewStatus status,
    @Schema(description = "When the review started.")
    Instant executedAt,
    @Schema(description = "Application version that ran the review.")
    String appVersion,
    @Schema(description = "Selection and weighting parameters the review used.")
    IndexRulesResponse rules,
    @Schema(description = "Security ids of the new index composition in rank order; empty when the status is FAILED "
        + "or ERROR.")
    List<Integer> composition) {
}
