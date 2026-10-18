package com.example.indexreviewer.model.report;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CAPPED_COMPONENT;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.COMPOSITION_SIZE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.EXCLUDED_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.JOINER;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.LEAVER;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.MISSING_REVIEW_DATA;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.audit;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.trace;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewReportTest {

    @Test
    void of_whenSelectionDone_expectComposition() {
        ReviewReport report = ReviewReport.of(audit(ReviewStatus.SUCCESS_WITH_WARNINGS),
            trace(List.of(JOINER, LEAVER, CAPPED_COMPONENT, EXCLUDED_SECURITY), List.of(MISSING_REVIEW_DATA)));

        assertThat(report.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(report.getStatus()).isEqualTo(ReviewStatus.SUCCESS_WITH_WARNINGS);
        assertThat(report.getConstituents()).extracting(Constituent::getSecurityId).containsExactly(155, 177);
        assertThat(report.getConstituents().getFirst().getWeight()).isEqualByComparingTo("18");
        assertThat(report.getConstituents().getFirst().getCappingFactor()).isEqualByComparingTo("0.565353");
        assertThat(report.getJoiners()).containsExactly(177);
        assertThat(report.getLeavers()).containsExactly(103);
    }

    @Test
    void of_whenReviewFailed_expectEmptyLists() {
        ReviewReport report = ReviewReport.of(audit(ReviewStatus.FAILED),
            trace(List.of(CAPPED_COMPONENT, LEAVER), List.of(COMPOSITION_SIZE)));

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(report.getConstituents()).isEmpty();
        assertThat(report.getJoiners()).isEmpty();
        assertThat(report.getLeavers()).isEmpty();
    }

    @Test
    void of_whenReviewError_expectEmptyLists() {
        ReviewReport report = ReviewReport.of(audit(ReviewStatus.ERROR),
            trace(List.of(CAPPED_COMPONENT, JOINER, LEAVER), List.of()));

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.ERROR);
        assertThat(report.getConstituents()).isEmpty();
        assertThat(report.getJoiners()).isEmpty();
        assertThat(report.getLeavers()).isEmpty();
    }

    @Test
    void completed_whenComponentsChanged_expectJoinersAndLeavers() {
        List<Candidate> selected = List.of(candidate(30, false), candidate(10, true), candidate(20, false));
        List<Constituent> constituents = List.of(constituent(30), constituent(10), constituent(20));

        ReviewReport report = ReviewReport.completed(REVIEW_ID, ReviewStatus.SUCCESS, constituents, selected,
            List.of(40, 10, 50, 40));

        assertThat(report.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(report.getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(report.getConstituents()).extracting(Constituent::getSecurityId).containsExactly(30, 10, 20);
        assertThat(report.getJoiners()).containsExactly(20, 30);
        assertThat(report.getLeavers()).containsExactly(40, 50);
    }

    @Test
    void failed_whenCalled_expectFailedWithEmptyLists() {
        ReviewReport report = ReviewReport.failed(REVIEW_ID);

        assertThat(report.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(report.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(report.getConstituents()).isEmpty();
        assertThat(report.getJoiners()).isEmpty();
        assertThat(report.getLeavers()).isEmpty();
    }

    private static Candidate candidate(int securityId, boolean existingComponent) {
        return Candidate.builder()
            .securityId(securityId)
            .existingComponent(existingComponent)
            .ffmcap(new BigDecimal("1000"))
            .build();
    }

    private static Constituent constituent(int securityId) {
        return Constituent.builder()
            .securityId(securityId)
            .weight(new BigDecimal("10"))
            .cappingFactor(BigDecimal.ONE)
            .build();
    }
}
