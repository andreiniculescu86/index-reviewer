package com.example.indexreviewer.dto.response.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "One security of the final index composition with its weighting factors.")
public record ConstituentResponse(
    Integer securityId,
    @Schema(description = "Final weight in the index after capping, as a percentage: 18 for 18%.")
    BigDecimal weight,
    @Schema(description = "Factor applied to the FFMCAP to limit the weight; 1 when the security is not capped.")
    BigDecimal cappingFactor) {
}
