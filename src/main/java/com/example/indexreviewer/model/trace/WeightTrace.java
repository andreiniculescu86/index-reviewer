package com.example.indexreviewer.model.trace;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Trace record of the weighting step for one index component. The weight is a percentage of the index, 18 for 18%.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeightTrace {

    Integer securityId;
    BigDecimal cappingFactor;
    BigDecimal weight;
}
