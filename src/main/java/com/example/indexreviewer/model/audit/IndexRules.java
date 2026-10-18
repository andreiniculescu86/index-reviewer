package com.example.indexreviewer.model.audit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Selection and weighting parameters of an index and the version of the methodology they come from. Every review
 * execution keeps a copy of the rules it used, so that later configuration changes do not change its audit data.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexRules {

    int numberOfComponents;
    int directSelectionRank;
    int bufferRank;
    BigDecimal capPercentage;
    String methodologyVersion;
}
