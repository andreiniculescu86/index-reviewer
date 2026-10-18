package com.example.indexreviewer.model.trace;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Trace of one security in a review: the input values used, the FFMCAP calculated from them and the decisions taken.
 * Values of review steps that did not run are null.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityTrace {

    Integer securityId;
    boolean existingComponent;
    BigDecimal price;
    BigDecimal shares;
    BigDecimal freeFloat;
    BigDecimal ffmcap;
    Integer rank;
    SelectionDecision decision;
    BigDecimal cappingFactor;
    BigDecimal weight;

    /**
     * Combines the step records of one security. The selection and weight records are null when their step did not
     * run for the security. A security without an FFMCAP cannot be ranked, so it is not eligible.
     */
    static SecurityTrace of(FfmcapTrace ffmcap, SelectionTrace selection, WeightTrace weight) {
        SecurityTraceBuilder trace = builder()
            .securityId(ffmcap.getSecurityId())
            .existingComponent(ffmcap.isExistingComponent())
            .price(ffmcap.getPrice())
            .shares(ffmcap.getShares())
            .freeFloat(ffmcap.getFreeFloat())
            .ffmcap(ffmcap.getFfmcap());
        if (!ffmcap.isEligible()) {
            trace.decision(SelectionDecision.NOT_ELIGIBLE);
        }
        if (selection != null) {
            trace.rank(selection.getRank()).decision(selection.getDecision());
        }
        if (weight != null) {
            trace.cappingFactor(weight.getCappingFactor()).weight(weight.getWeight());
        }
        return trace.build();
    }
}
