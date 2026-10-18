package com.example.indexreviewer.model.report;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * One security of the final index composition with its weighting factors.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Constituent {

    Integer securityId;
    BigDecimal weight;
    BigDecimal cappingFactor;
}
