package com.example.indexreviewer.model.trace;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.COMPOSITION_SIZE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.ELIGIBLE_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.EXCLUDED_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.MISSING_REVIEW_DATA;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.trace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ReviewTraceTest {

    @Test
    void forSecurity_whenTraced_expectItsTraceAndFindings() {
        ReviewTrace trace = trace(List.of(ELIGIBLE_SECURITY, EXCLUDED_SECURITY),
            List.of(COMPOSITION_SIZE, MISSING_REVIEW_DATA));

        ReviewTrace securityTrace = trace.forSecurity(166);

        assertThat(securityTrace.getSecurities()).containsExactly(EXCLUDED_SECURITY);
        assertThat(securityTrace.getFindings()).containsExactly(MISSING_REVIEW_DATA);
    }

    @Test
    void forSecurity_whenUnknown_expectEmpty() {
        ReviewTrace trace = trace(List.of(ELIGIBLE_SECURITY), List.of(COMPOSITION_SIZE));

        ReviewTrace securityTrace = trace.forSecurity(166);

        assertThat(securityTrace.isEmpty()).isTrue();
    }

    @Test
    void build_whenListsChangedLater_expectTraceUnchanged() {
        List<Finding> findings = new ArrayList<>(List.of(MISSING_REVIEW_DATA));
        ReviewTrace trace = trace(List.of(EXCLUDED_SECURITY), findings);

        findings.add(COMPOSITION_SIZE);

        assertThat(trace.getFindings()).containsExactly(MISSING_REVIEW_DATA);
        assertThatThrownBy(() -> trace.getFindings().add(COMPOSITION_SIZE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void of_whenStepRecordsGiven_expectOneTracePerSecurity() {
        List<FfmcapTrace> ffmcap = List.of(
            FfmcapTrace.builder().securityId(30).ffmcap(new BigDecimal("100")).build(),
            FfmcapTrace.builder().securityId(20).existingComponent(true).build(),
            FfmcapTrace.builder().securityId(10).ffmcap(new BigDecimal("300")).build());
        List<SelectionTrace> selection = List.of(
            SelectionTrace.builder().securityId(10).rank(1).decision(SelectionDecision.SELECTED_DIRECT).build(),
            SelectionTrace.builder().securityId(30).rank(2).decision(SelectionDecision.NOT_SELECTED_BELOW_BUFFER)
                .build());
        List<WeightTrace> weights = List.of(
            WeightTrace.builder().securityId(10).cappingFactor(BigDecimal.ONE).weight(BigDecimal.ONE).build());

        ReviewTrace trace = ReviewTrace.of(List.of(COMPOSITION_SIZE), ffmcap, selection, weights);

        assertThat(trace.getSecurities())
            .extracting(SecurityTrace::getSecurityId, SecurityTrace::isExistingComponent, SecurityTrace::getRank,
                SecurityTrace::getDecision, SecurityTrace::getWeight)
            .containsExactly(
                tuple(10, false, 1, SelectionDecision.SELECTED_DIRECT, BigDecimal.ONE),
                tuple(20, true, null, SelectionDecision.NOT_ELIGIBLE, null),
                tuple(30, false, 2, SelectionDecision.NOT_SELECTED_BELOW_BUFFER, null));
        assertThat(trace.getFindings()).containsExactly(COMPOSITION_SIZE);
    }

    @Test
    void build_whenListsNotSet_expectEmptyLists() {
        ReviewTrace trace = ReviewTrace.builder().build();

        assertThat(trace.getSecurities()).isEmpty();
        assertThat(trace.getFindings()).isEmpty();
    }
}
