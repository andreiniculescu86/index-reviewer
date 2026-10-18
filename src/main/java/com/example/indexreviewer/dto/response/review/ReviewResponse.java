package com.example.indexreviewer.dto.response.review;

import com.example.indexreviewer.model.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Review report: final constituents with their weighting factors, joiners, leavers, status and "
    + "the findings behind the status. The constituent, joiner and leaver lists are empty when the status is FAILED or "
    + "ERROR.")
public record ReviewResponse(
    @Schema(description = "Id of the stored review execution, for the audit and trace endpoints.")
    UUID reviewId,
    @Schema(description = "FAILED when a finding is an error, SUCCESS_WITH_WARNINGS when a finding is a warning, "
        + "ERROR when the review stopped on an unexpected error.")
    ReviewStatus status,
    @Schema(description = "Final constituents, ordered by rank.")
    List<ConstituentResponse> constituents,
    @Schema(description = "Ids of the securities that enter the index.")
    List<Integer> joiners,
    @Schema(description = "Ids of the securities that leave the index.")
    List<Integer> leavers,
    @Schema(description = "Data problems found during the review: errors explain FAILED, warnings explain "
        + "SUCCESS_WITH_WARNINGS.")
    List<FindingResponse> findings) {
}
