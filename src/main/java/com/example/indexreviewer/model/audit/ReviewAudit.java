package com.example.indexreviewer.model.audit;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Audit record of one review execution: what the review was, when it started, which application version and rules it
 * used and how it ended. It is stored once at the end of the review, also when the review failed or stopped on an
 * unexpected error, and holds no traces.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewAudit {

    UUID reviewId;
    IndexType indexType;
    LocalDate cutoffDate;
    LocalDate reviewDate;
    ReviewStatus status;
    Instant executedAt;
    String appVersion;
    IndexRules rules;
    List<Integer> composition;
}
