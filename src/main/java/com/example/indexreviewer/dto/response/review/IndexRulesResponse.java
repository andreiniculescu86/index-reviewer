package com.example.indexreviewer.dto.response.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Selection and weighting parameters the review used.")
public record IndexRulesResponse(int numberOfComponents, int directSelectionRank, int bufferRank,
                                 BigDecimal capPercentage,
                                 @Schema(description = "Version of the index methodology the rules come from.",
                                     example = "rulebook-v3.40, 08.06.2026")
                                 String methodologyVersion) {
}
