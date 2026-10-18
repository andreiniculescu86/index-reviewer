package com.example.indexreviewer.model.trace;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * One data problem found during a review. The security id is null when the finding is about the whole input. The
 * field names the data that caused the finding, when there is one.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Finding {

    FindingCode code;
    Integer securityId;
    String field;
    String message;

    public Severity getSeverity() {
        return code.getSeverity();
    }
}
