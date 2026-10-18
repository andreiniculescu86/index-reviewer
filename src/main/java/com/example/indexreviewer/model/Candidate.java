package com.example.indexreviewer.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Universe member with a valid FFMCAP, which the selection can rank (rulebook section 2.1, candidate).
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Candidate {

    Integer securityId;
    boolean existingComponent;
    BigDecimal ffmcap;
}
