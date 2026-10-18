package com.example.indexreviewer.dto.response.review;

import com.example.indexreviewer.model.trace.SelectionDecision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Values used and decisions taken for one security. Values of review steps that did not run "
    + "are null.")
public record SecurityTraceResponse(
    Integer securityId,
    @Schema(description = "Whether the security is in the current index composition.")
    boolean existingComponent,
    @Schema(description = "Price on the cut-off date.")
    BigDecimal price,
    @Schema(description = "Number of shares on the review date.")
    BigDecimal shares,
    @Schema(description = "Free float factor on the review date.")
    BigDecimal freeFloat,
    @Schema(description = "price x shares x freeFloat; null when the security is not eligible.")
    BigDecimal ffmcap,
    @Schema(description = "Rank by FFMCAP among the eligible securities.")
    Integer rank,
    @Schema(description = "Selection outcome; NOT_ELIGIBLE is explained by the findings of the security.")
    SelectionDecision decision,
    BigDecimal cappingFactor,
    BigDecimal weight) {
}
