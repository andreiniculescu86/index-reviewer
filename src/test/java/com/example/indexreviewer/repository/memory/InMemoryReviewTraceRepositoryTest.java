package com.example.indexreviewer.repository.memory;

import com.example.indexreviewer.model.trace.FfmcapTrace;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SecurityTrace;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.model.trace.SelectionTrace;
import com.example.indexreviewer.model.trace.WeightTrace;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.COMPOSITION_SIZE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.MISSING_REVIEW_DATA;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class InMemoryReviewTraceRepositoryTest {

    private static final FfmcapTrace ELIGIBLE = FfmcapTrace.builder()
        .securityId(10)
        .price(new BigDecimal("2080"))
        .shares(new BigDecimal("309375"))
        .freeFloat(new BigDecimal("0.6766"))
        .ffmcap(new BigDecimal("435392100"))
        .build();
    private static final FfmcapTrace NOT_ELIGIBLE = FfmcapTrace.builder()
        .securityId(166)
        .price(new BigDecimal("0.13"))
        .build();

    private final InMemoryReviewTraceRepository repository = new InMemoryReviewTraceRepository();

    @Test
    void findByReviewId_whenStepsAppended_expectCombined() {
        repository.appendFindings(REVIEW_ID, List.of(COMPOSITION_SIZE));
        repository.appendFfmcap(REVIEW_ID, List.of(NOT_ELIGIBLE, ELIGIBLE));
        repository.appendFindings(REVIEW_ID, List.of(MISSING_REVIEW_DATA));
        repository.appendSelection(REVIEW_ID, List.of(SelectionTrace.builder()
            .securityId(10)
            .rank(1)
            .decision(SelectionDecision.SELECTED_DIRECT)
            .build()));
        repository.appendWeights(REVIEW_ID, List.of(WeightTrace.builder()
            .securityId(10)
            .cappingFactor(BigDecimal.ONE)
            .weight(BigDecimal.ONE)
            .build()));

        ReviewTrace trace = repository.findByReviewId(REVIEW_ID);

        assertThat(trace.getSecurities())
            .extracting(SecurityTrace::getSecurityId, SecurityTrace::getRank, SecurityTrace::getDecision,
                SecurityTrace::getWeight)
            .containsExactly(
                tuple(10, 1, SelectionDecision.SELECTED_DIRECT, BigDecimal.ONE),
                tuple(166, null, SelectionDecision.NOT_ELIGIBLE, null));
        assertThat(trace.getFindings()).containsExactly(COMPOSITION_SIZE, MISSING_REVIEW_DATA);
    }

    @Test
    void findByReviewId_whenOtherReviewStored_expectEmpty() {
        repository.appendFfmcap(REVIEW_ID, List.of(ELIGIBLE));
        repository.appendFindings(REVIEW_ID, List.of(MISSING_REVIEW_DATA));

        ReviewTrace trace = repository.findByReviewId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(trace.isEmpty()).isTrue();
    }

    @Test
    void appendFindings_whenListChanged_expectStoredKept() {
        List<Finding> findings = new ArrayList<>(List.of(MISSING_REVIEW_DATA));
        repository.appendFindings(REVIEW_ID, findings);

        findings.add(COMPOSITION_SIZE);

        assertThat(repository.findByReviewId(REVIEW_ID).getFindings()).containsExactly(MISSING_REVIEW_DATA);
    }
}
