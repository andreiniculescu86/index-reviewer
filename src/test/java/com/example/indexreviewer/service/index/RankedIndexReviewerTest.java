package com.example.indexreviewer.service.index;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SecurityTrace;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.model.trace.Severity;
import com.example.indexreviewer.repository.memory.InMemoryReviewAuditRepository;
import com.example.indexreviewer.repository.memory.InMemoryReviewTraceRepository;
import com.example.indexreviewer.service.review.ComponentSelectorStepImpl;
import com.example.indexreviewer.service.review.FfmcapCalculatorStep;
import com.example.indexreviewer.service.review.FfmcapCalculatorStepImpl;
import com.example.indexreviewer.service.review.ReviewDataValidator;
import com.example.indexreviewer.service.review.SecurityRecordValidator;
import com.example.indexreviewer.service.review.WeightCalculatorStep;
import com.example.indexreviewer.service.review.WeightCalculatorStepImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.APP_VERSION;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.SMI_RULES;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.assignmentCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class RankedIndexReviewerTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);
    private static final List<Integer> CURRENT_SMI = IntStream.rangeClosed(1, 20).boxed().toList();
    private static final IndexesConfiguration CONFIGURATION = smiConfiguration(22);

    private final InMemoryReviewAuditRepository auditRepository = new InMemoryReviewAuditRepository();
    private final InMemoryReviewTraceRepository traceRepository = new InMemoryReviewTraceRepository();
    private final ReviewDataValidator reviewDataValidator = new ReviewDataValidator();
    private final FfmcapCalculatorStep ffmcapCalculatorStep =
        new FfmcapCalculatorStepImpl(new SecurityRecordValidator());
    private final RankedIndexReviewer review = reviewer(ffmcapCalculatorStep, new WeightCalculatorStepImpl());

    @Test
    void indexTypes_whenSmiConfigured_expectSmi() {
        Set<IndexType> indexTypes = review.indexTypes();

        assertThat(indexTypes).containsExactly(IndexType.SMI);
    }

    @Test
    void review_whenCompositionHas19_expectError() {
        ReviewCommand command = command(validRows(25), CURRENT_SMI.subList(0, 19));

        ReviewReport report = review.review(command);

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(report.getConstituents()).isEmpty();

        assertThat(errors(report.getReviewId())).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.COMPOSITION_SIZE);
            assertThat(finding.getMessage()).contains("19");
        });
        assertThat(storedTrace(report.getReviewId()).getSecurities()).isEmpty();
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(storedAudit(report.getReviewId()).getRules()).isEqualTo(SMI_RULES);
        assertThat(storedAudit(report.getReviewId()).getComposition()).isEmpty();
    }

    @Test
    void review_whenCompositionHasDuplicate_expectError() {
        List<Integer> composition = new ArrayList<>(CURRENT_SMI.subList(0, 19));
        composition.add(1);
        ReviewCommand command = command(validRows(25), composition);

        ReviewReport report = review.review(command);

        assertThat(errors(report.getReviewId())).extracting(Finding::getCode)
            .containsExactly(FindingCode.COMPOSITION_SIZE);
    }

    @Test
    void review_whenNoReviewDateData_expectError() {
        List<Security> cutoffRowsOnly = validRows(25).stream()
            .filter(row -> CUTOFF_DATE.equals(row.getDate()))
            .toList();
        ReviewCommand command = command(cutoffRowsOnly, CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(errors(report.getReviewId())).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.MISSING_DATE);
            assertThat(finding.getMessage()).contains("2026-09-21");
        });
        assertThat(storedTrace(report.getReviewId()).getSecurities()).isEmpty();
    }

    @Test
    void review_whenOnly19Eligible_expectError() {
        ReviewCommand command = command(validRows(19), CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(errors(report.getReviewId())).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.TOO_FEW_ELIGIBLE);
            assertThat(finding.getMessage()).contains("19");
        });
        assertThat(storedTrace(report.getReviewId()).getSecurities()).hasSize(20);
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    void review_whenRowsInvalid_expectAllErrorsAndTrace() {
        List<Security> rows = new ArrayList<>(validRows(22));
        rows.add(cutoffRow(23, "-2080"));
        rows.add(reviewRow(23, "0.6766"));
        rows.add(cutoffRow(24, "898"));
        rows.add(reviewRow(24, "1.5"));
        ReviewCommand command = command(rows, CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(errors(report.getReviewId())).extracting(Finding::getSecurityId).containsExactly(23, 24);
        assertThat(storedTrace(report.getReviewId()).getSecurities()).hasSize(24);
        assertThat(storedTrace(report.getReviewId()).getSecurities()).filteredOn(SecurityTrace::isExistingComponent)
            .hasSize(20);
        assertThat(storedTrace(report.getReviewId()).getSecurities())
            .filteredOn(security -> security.getDecision() == SelectionDecision.NOT_ELIGIBLE)
            .extracting(SecurityTrace::getSecurityId)
            .containsExactly(23, 24);
    }

    @Test
    void review_whenRowsInvalidAndTooFew_expectBothErrors() {
        List<Security> rows = new ArrayList<>(validRows(19));
        rows.add(cutoffRow(23, "-2080"));
        rows.add(reviewRow(23, "0.6766"));
        ReviewCommand command = command(rows, CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(errors(report.getReviewId())).extracting(Finding::getCode)
            .containsExactly(FindingCode.INVALID_VALUE, FindingCode.TOO_FEW_ELIGIBLE);
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    void review_whenDataComplete_expectWeightsAndAudit() {
        ReviewCommand command = command(validRows(25), CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(report.getReviewId()).isNotNull();
        assertThat(report.getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(report.getConstituents()).extracting(Constituent::getSecurityId)
            .containsExactlyElementsOf(IntStream.iterate(25, id -> id >= 6, id -> id - 1).boxed().toList());
        assertThat(report.getJoiners()).containsExactly(21, 22, 23, 24, 25);
        assertThat(report.getLeavers()).containsExactly(1, 2, 3, 4, 5);

        ReviewAudit audit = storedAudit(report.getReviewId());
        assertThat(audit.getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(audit.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(audit.getCutoffDate()).isEqualTo(CUTOFF_DATE);
        assertThat(audit.getReviewDate()).isEqualTo(REVIEW_DATE);
        assertThat(audit.getAppVersion()).isEqualTo(APP_VERSION);
        assertThat(audit.getExecutedAt()).isNotNull();
        assertThat(audit.getComposition())
            .containsExactlyElementsOf(IntStream.iterate(25, id -> id >= 6, id -> id - 1).boxed().toList());
        List<SecurityTrace> selected = storedTrace(report.getReviewId()).getSecurities().stream()
            .filter(security -> security.getDecision() != null && security.getDecision().isSelected())
            .toList();
        assertThat(selected).extracting(SecurityTrace::getSecurityId)
            .containsExactlyElementsOf(IntStream.rangeClosed(6, 25).boxed().toList());
        assertThat(selected).extracting(SecurityTrace::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsOnly(BigDecimal.ONE);
        assertThat(selected.stream().map(SecurityTrace::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("100");
    }

    @Test
    void review_whenWeightAboveCap_expectCapped() {
        List<Security> rows = new ArrayList<>(validRows(24));
        rows.add(cutoffRow(25, "5000"));
        rows.add(reviewRow(25, "0.5"));
        ReviewCommand command = command(rows, CURRENT_SMI);

        ReviewReport report = review.review(command);

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(report.getConstituents().getFirst()).satisfies(constituent -> {
            assertThat(constituent.getSecurityId()).isEqualTo(25);
            assertThat(constituent.getWeight()).isEqualByComparingTo("18");
        });
        assertThat(report.getConstituents().stream().map(Constituent::getWeight).reduce(BigDecimal.ZERO,
            BigDecimal::add)).isEqualByComparingTo("100");
        assertThat(report.getConstituents().subList(1, 20)).extracting(Constituent::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsOnly(BigDecimal.ONE);
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(storedTrace(report.getReviewId()).forSecurity(25).getSecurities()).singleElement()
            .satisfies(security -> {
                assertThat(security.getRank()).isEqualTo(1);
                assertThat(security.getWeight()).isEqualByComparingTo("18");
                assertThat(security.getCappingFactor()).isLessThan(BigDecimal.ONE);
            });
        assertThat(storedTrace(report.getReviewId()).getFindings()).isEmpty();
    }


    @Test
    void review_whenWeightsHaveError_expectFailed() {
        WeightCalculatorStep failingWeights = new WeightCalculatorStepImpl() {
            @Override
            public WeightResult calculateWeights(List<Candidate> components, IndexRules rules) {
                return WeightResult.builder()
                    .constituents(List.of())
                    .traces(List.of())
                    .findings(List.of(Finding.builder()
                        .code(FindingCode.INVALID_VALUE)
                        .message("Weights cannot be calculated")
                        .build()))
                    .build();
            }
        };
        RankedIndexReviewer reviewer = reviewer(ffmcapCalculatorStep, failingWeights);
        ReviewCommand command = command(validRows(25), CURRENT_SMI);

        ReviewReport report = reviewer.review(command);

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(report.getConstituents()).isEmpty();
        assertThat(errors(report.getReviewId())).extracting(Finding::getMessage)
            .containsExactly("Weights cannot be calculated");
        assertThat(storedTrace(report.getReviewId()).getSecurities())
            .filteredOn(security -> security.getRank() != null).hasSize(25);
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    void review_whenSelectionTooSmall_expectFailedWithTrace() {
        RankedIndexReviewer reviewer = new RankedIndexReviewer(smiConfiguration(19), reviewDataValidator,
            ffmcapCalculatorStep, new ComponentSelectorStepImpl(), new WeightCalculatorStepImpl(), auditRepository,
            traceRepository, APP_VERSION);
        ReviewCommand command = command(validRows(25), CURRENT_SMI);

        ReviewReport report = reviewer.review(command);

        assertThat(report.getStatus()).isEqualTo(ReviewStatus.FAILED);
        assertThat(report.getConstituents()).isEmpty();
        assertThat(errors(report.getReviewId())).extracting(Finding::getCode)
            .containsExactly(FindingCode.SELECTION_SIZE);
        List<SecurityTrace> securities = storedTrace(report.getReviewId()).getSecurities();
        assertThat(securities).filteredOn(security -> security.getRank() != null).hasSize(25);
        assertThat(securities).allSatisfy(security -> assertThat(security.getWeight()).isNull());
        assertThat(storedAudit(report.getReviewId()).getStatus()).isEqualTo(ReviewStatus.FAILED);
    }

    @Test
    void review_whenAssignmentData_expectQ3Result() throws IOException {
        ReviewCommand command = assignmentCommand();

        ReviewReport report = review.review(command);

        assertThat(report.getStatus().isSuccessful()).isTrue();
        assertThat(report.getConstituents()).hasSize(20);
        assertThat(report.getJoiners()).containsExactly(177);
        assertThat(report.getLeavers()).containsExactly(103);
        List<SecurityTrace> securities = storedTrace(report.getReviewId()).getSecurities();
        assertThat(securities).extracting(SecurityTrace::getSecurityId, SecurityTrace::getRank,
                SecurityTrace::getDecision)
            .contains(
                tuple(177, 7, SelectionDecision.SELECTED_DIRECT),
                tuple(253, 18, SelectionDecision.SELECTED_DIRECT),
                tuple(249, 19, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(28, 20, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(160, 21, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(81, 22, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT));
        assertThat(securities).filteredOn(security -> security.getSecurityId() == 103).singleElement()
            .satisfies(security -> assertThat(security.getDecision())
                .isEqualTo(SelectionDecision.NOT_SELECTED_BELOW_BUFFER));
        assertThat(report.getConstituents())
            .filteredOn(constituent -> constituent.getCappingFactor().compareTo(BigDecimal.ONE) < 0)
            .extracting(Constituent::getSecurityId, Constituent::getWeight, Constituent::getCappingFactor)
            .containsExactlyInAnyOrder(
                tuple(155, new BigDecimal("18.0000"), new BigDecimal("0.5654")),
                tuple(205, new BigDecimal("18.0000"), new BigDecimal("0.6117")));
        assertThat(report.getConstituents()).filteredOn(constituent -> constituent.getSecurityId() == 63)
            .extracting(Constituent::getWeight).containsExactly(new BigDecimal("14.2591"));
        assertThat(report.getConstituents().getLast()).satisfies(constituent -> {
            assertThat(constituent.getSecurityId()).isEqualTo(81);
            assertThat(constituent.getWeight()).isEqualByComparingTo("0.8503");
        });
        assertThat(report.getConstituents().stream().map(Constituent::getWeight).reduce(BigDecimal.ZERO,
            BigDecimal::add)).isEqualByComparingTo("100");
    }

    private RankedIndexReviewer reviewer(FfmcapCalculatorStep ffmcapCalculatorStep,
                                         WeightCalculatorStep weightCalculatorStep) {
        return new RankedIndexReviewer(CONFIGURATION, reviewDataValidator, ffmcapCalculatorStep,
            new ComponentSelectorStepImpl(), weightCalculatorStep, auditRepository, traceRepository, APP_VERSION);
    }

    private ReviewAudit storedAudit(UUID reviewId) {
        return auditRepository.findById(reviewId).orElseThrow();
    }

    private ReviewTrace storedTrace(UUID reviewId) {
        return traceRepository.findByReviewId(reviewId);
    }

    private List<Finding> errors(UUID reviewId) {
        return storedTrace(reviewId).getFindings().stream()
            .filter(finding -> finding.getSeverity() == Severity.ERROR)
            .toList();
    }

    private static ReviewCommand command(List<Security> rows, List<Integer> composition) {
        Set<Integer> universe = new HashSet<>();
        rows.forEach(row -> universe.add(row.getSecurityId()));
        return ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, universe))
            .securities(rows)
            .composition(composition)
            .build();
    }

    private static List<Security> validRows(int count) {
        List<Security> rows = new ArrayList<>();
        for (int id = 1; id <= count; id++) {
            rows.add(cutoffRow(id, String.valueOf(100 + id)));
            rows.add(reviewRow(id, "0.5"));
        }
        return rows;
    }

    private static Security cutoffRow(int id, String price) {
        return Security.builder()
            .securityId(id)
            .date(CUTOFF_DATE)
            .price(new BigDecimal(price))
            .freeFloat(new BigDecimal("0.5"))
            .shares(new BigDecimal("1000000"))
            .build();
    }

    private static Security reviewRow(int id, String freeFloat) {
        return Security.builder()
            .securityId(id)
            .date(REVIEW_DATE)
            .freeFloat(new BigDecimal(freeFloat))
            .shares(new BigDecimal("1000000"))
            .build();
    }

    private static IndexesConfiguration smiConfiguration(int bufferRank) {
        return new IndexesConfiguration(Map.of(IndexType.SMI, new IndexConfiguration(20, 18, bufferRank,
            new BigDecimal("18"), "rulebook-v3.40, 08.06.2026")));
    }
}
