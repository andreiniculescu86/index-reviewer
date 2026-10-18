package com.example.indexreviewer.model.report;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SecurityTrace;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Review report as the assignment defines it: the final constituents with their weighting factors, the joiners, the
 * leavers and the review status, plus the findings that explain the status. The review id only locates the stored
 * audit record and trace; it is not part of the report. A review with status FAILED or ERROR has no composition, so
 * its constituent, joiner and leaver lists are empty.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewReport {

    UUID reviewId;
    ReviewStatus status;
    @Singular
    List<Constituent> constituents;
    @Singular
    List<Integer> joiners;
    @Singular
    List<Integer> leavers;
    @With
    @Singular
    List<Finding> findings;

    public static ReviewReport failed(UUID reviewId) {
        return builder()
            .reviewId(reviewId)
            .status(ReviewStatus.FAILED)
            .build();
    }

    /**
     * Builds the report of a review that ran all steps. The constituents keep the given order. Selected candidates that
     * are not existing components join, existing components that were not selected leave.
     */
    public static ReviewReport completed(UUID reviewId, ReviewStatus status, List<Constituent> constituents,
                                         List<Candidate> selected, List<Integer> composition) {
        Set<Integer> selectedIds = selected.stream().map(Candidate::getSecurityId).collect(Collectors.toSet());
        return builder()
            .reviewId(reviewId)
            .status(status)
            .constituents(constituents)
            .joiners(selected.stream()
                .filter(candidate -> !candidate.isExistingComponent())
                .map(Candidate::getSecurityId)
                .sorted()
                .toList())
            .leavers(composition.stream()
                .distinct()
                .filter(securityId -> !selectedIds.contains(securityId))
                .sorted()
                .toList())
            .build();
    }

    /**
     * Derives the report of a stored review from its audit record and trace.
     */
    public static ReviewReport of(ReviewAudit audit, ReviewTrace trace) {
        ReviewReportBuilder report = builder()
            .reviewId(audit.getReviewId())
            .status(audit.getStatus())
            .findings(trace.getFindings());
        if (!audit.getStatus().isSuccessful()) {
            return report.build();
        }
        List<SecurityTrace> securities = trace.getSecurities();
        securities.stream()
            .filter(ReviewReport::isSelected)
            .sorted(Comparator.comparing(SecurityTrace::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(security -> report.constituent(Constituent.builder()
                .securityId(security.getSecurityId())
                .weight(security.getWeight())
                .cappingFactor(security.getCappingFactor())
                .build()));
        securities.stream()
            .filter(security -> isSelected(security) && !security.isExistingComponent())
            .forEach(security -> report.joiner(security.getSecurityId()));
        securities.stream()
            .filter(security -> security.isExistingComponent() && !isSelected(security))
            .forEach(security -> report.leaver(security.getSecurityId()));
        return report.build();
    }

    private static boolean isSelected(SecurityTrace security) {
        return security.getDecision() != null && security.getDecision().isSelected();
    }
}
