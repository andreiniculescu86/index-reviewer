package com.example.indexreviewer.model.trace;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Trace record of the FFMCAP step for one security: the input values used and the FFMCAP calculated from them. The
 * FFMCAP is null when the security is not eligible; its findings explain why.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FfmcapTrace {

    Integer securityId;
    boolean existingComponent;
    BigDecimal price;
    BigDecimal shares;
    BigDecimal freeFloat;
    BigDecimal ffmcap;

    public boolean isEligible() {
        return ffmcap != null;
    }
}
