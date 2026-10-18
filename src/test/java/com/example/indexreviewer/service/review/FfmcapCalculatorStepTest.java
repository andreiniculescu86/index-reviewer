package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.SecurityTrace;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.repository.memory.InMemoryReviewTraceRepository;
import com.example.indexreviewer.service.review.smi.FfmcapCalculatorStep;
import com.example.indexreviewer.service.review.smi.SecurityRecordValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.assignmentCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class FfmcapCalculatorStepTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);

    private final FfmcapCalculatorStep calculator = new FfmcapCalculatorStep(new SecurityRecordValidator());

    @Test
    void calculate_whenRowsComplete_expectReviewDateValues() {
        ReviewCommand command = command(Set.of(101), List.of(
            cutoffRow(101, "311.5", "0.8048", "5800000"),
            reviewRow(101, "0.8476", "5835000")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(securities(result)).singleElement().satisfies(security -> {
            assertThat(security.getSecurityId()).isEqualTo(101);
            assertThat(security.isExistingComponent()).isFalse();
            assertThat(security.getPrice()).isEqualByComparingTo("311.5");
            assertThat(security.getShares()).isEqualByComparingTo("5835000");
            assertThat(security.getFreeFloat()).isEqualByComparingTo("0.8476");
            assertThat(security.getFfmcap()).isEqualByComparingTo("1540599879");
            assertThat(security.getDecision()).isNull();
        });
        assertThat(result.getCandidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.getSecurityId()).isEqualTo(101);
            assertThat(candidate.getFfmcap()).isEqualByComparingTo("1540599879");
        });
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculate_whenNoReviewDateRow_expectNotEligible() {
        ReviewCommand command = command(Set.of(10, 166), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(166, "0.13", "0.087", "1139771344")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(security(result, 166)).satisfies(security -> {
            assertThat(security.getPrice()).isEqualByComparingTo("0.13");
            assertThat(security.getShares()).isNull();
            assertThat(security.getFfmcap()).isNull();
            assertThat(security.getDecision()).isEqualTo(SelectionDecision.NOT_ELIGIBLE);
        });
        assertThat(security(result, 10).getFfmcap()).isNotNull();
        assertThat(result.getCandidates()).extracting(Candidate::getSecurityId).containsExactly(10);
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.MISSING_REVIEW_DATA);
            assertThat(finding.getSecurityId()).isEqualTo(166);
        });
    }

    @Test
    void calculate_whenNoCutoffPrice_expectNotEligible() {
        ReviewCommand command = command(Set.of(10, 400), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            reviewRow(400, "0.5", "1000000")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(security(result, 400).getDecision()).isEqualTo(SelectionDecision.NOT_ELIGIBLE);
        assertThat(security(result, 400).getShares()).isEqualByComparingTo("1000000");
        assertThat(result.getFindings()).extracting(Finding::getCode, Finding::getSecurityId)
            .containsExactly(tuple(FindingCode.MISSING_CUTOFF_PRICE, 400));
    }

    @Test
    void calculate_whenNotInUniverse_expectIgnored() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(999, "50", "1", "1000"),
            reviewRow(999, "1", "1000")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(securities(result)).extracting(SecurityTrace::getSecurityId).containsExactly(10);
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.IGNORED_SECURITIES);
            assertThat(finding.getMessage()).startsWith("1 securities");
        });
    }

    @Test
    void calculate_whenUniverseOnOtherDate_expectNoResult() {
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(CUTOFF_DATE, Set.of(10)))
            .securities(List.of(cutoffRow(10, "2080", "0.6766", "309375"), reviewRow(10, "0.6766", "309375")))
            .composition(List.of())
            .build();

        FfmcapResult result = calculator.calculate(command);

        assertThat(securities(result)).isEmpty();
    }

    @Test
    void calculate_whenRowRepeated_expectCountedOnce() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(security(result, 10).getFfmcap()).isEqualByComparingTo("435392100");
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.DUPLICATE_ROW);
            assertThat(finding.getSecurityId()).isEqualTo(10);
        });
    }

    @Test
    void calculate_whenRowsConflict_expectError() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            cutoffRow(10, "2100", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.CONFLICTING_DUPLICATE);
            assertThat(finding.getSecurityId()).isEqualTo(10);
        });
        assertThat(security(result, 10).getFfmcap()).isNull();
        assertThat(security(result, 10).getDecision()).isEqualTo(SelectionDecision.NOT_ELIGIBLE);
    }

    @Test
    void calculate_whenSeveralRowsInvalid_expectAllErrors() {
        ReviewCommand command = command(Set.of(10, 11, 12), List.of(
            cutoffRow(10, "0", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(11, "898", "0.72", "1485000"),
            reviewRow(11, "1.5", "1485000"),
            cutoffRow(12, "131", "0.4172", "1960000"),
            reviewRow(12, "0.4172", "-1960000")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(result.getFindings()).extracting(Finding::getCode, Finding::getSecurityId, Finding::getField)
            .containsExactly(
                tuple(FindingCode.INVALID_VALUE, 10, "price"),
                tuple(FindingCode.INVALID_VALUE, 11, "freeFloat"),
                tuple(FindingCode.INVALID_VALUE, 12, "shares"));
        assertThat(securities(result)).extracting(SecurityTrace::getDecision)
            .containsOnly(SelectionDecision.NOT_ELIGIBLE);
        assertThat(security(result, 12).getShares()).isEqualByComparingTo("-1960000");
    }

    @Test
    void calculate_whenExistingComponentNotInUniverse_expectTrace() {
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, Set.of(10)))
            .securities(List.of(cutoffRow(10, "2080", "0.6766", "309375"), reviewRow(10, "0.6766", "309375")))
            .composition(List.of(20, 10))
            .build();

        FfmcapResult result = calculator.calculate(command);

        assertThat(securities(result)).extracting(SecurityTrace::getSecurityId, SecurityTrace::isExistingComponent,
                SecurityTrace::getDecision)
            .containsExactly(
                tuple(10, true, null),
                tuple(20, true, SelectionDecision.NOT_ELIGIBLE));
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.EXISTING_COMPONENT_NOT_IN_UNIVERSE);
            assertThat(finding.getSecurityId()).isEqualTo(20);
        });
    }

    @Test
    void calculate_whenAssignmentData_expect205Traces() throws IOException {
        ReviewCommand command = assignmentCommand();

        FfmcapResult result = calculator.calculate(command);

        assertThat(securities(result)).hasSize(205);
        assertThat(securities(result)).filteredOn(security -> security.getFfmcap() != null).hasSize(204);
        assertThat(securities(result)).filteredOn(SecurityTrace::isExistingComponent).hasSize(20);
        assertThat(security(result, 10).getFfmcap()).isEqualByComparingTo("435392100");
        assertThat(security(result, 166).getDecision()).isEqualTo(SelectionDecision.NOT_ELIGIBLE);
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.MISSING_REVIEW_DATA);
            assertThat(finding.getSecurityId()).isEqualTo(166);
        });
    }

    // Stores the step's trace records like the reviewer does, to read them combined per security
    private static List<SecurityTrace> securities(FfmcapResult result) {
        InMemoryReviewTraceRepository traceRepository = new InMemoryReviewTraceRepository();
        traceRepository.appendFfmcap(REVIEW_ID, result.getTraces());
        return traceRepository.findByReviewId(REVIEW_ID).getSecurities();
    }

    private static SecurityTrace security(FfmcapResult result, int securityId) {
        return securities(result).stream()
            .filter(security -> security.getSecurityId() == securityId)
            .findFirst()
            .orElseThrow();
    }

    private static ReviewCommand command(Set<Integer> universe, List<Security> rows) {
        return ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, universe))
            .securities(rows)
            .composition(List.of())
            .build();
    }

    private static Security cutoffRow(int id, String price, String freeFloat, String shares) {
        return Security.builder()
            .securityId(id)
            .date(CUTOFF_DATE)
            .price(new BigDecimal(price))
            .freeFloat(new BigDecimal(freeFloat))
            .shares(new BigDecimal(shares))
            .build();
    }

    private static Security reviewRow(int id, String freeFloat, String shares) {
        return Security.builder()
            .securityId(id)
            .date(REVIEW_DATE)
            .freeFloat(new BigDecimal(freeFloat))
            .shares(new BigDecimal(shares))
            .build();
    }
}
