package com.example.indexreviewer.model.trace;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Trace record of the selection step for one ranked candidate.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SelectionTrace {

    Integer securityId;
    int rank;
    SelectionDecision decision;
}
