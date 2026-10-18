package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.service.review.smi.ReviewDataValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDataValidatorTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);
    private static final Security CUTOFF_ROW =
        Security.builder()
            .securityId(10)
            .date(CUTOFF_DATE)
            .price(new BigDecimal("2080"))
            .build();
    private static final Security REVIEW_ROW =
        Security.builder()
            .securityId(10)
            .date(REVIEW_DATE)
            .freeFloat(new BigDecimal("0.6766"))
            .shares(new BigDecimal("309375"))
            .build();

    private final ReviewDataValidator validator = new ReviewDataValidator();

    @Test
    void validateComposition_whenTooFewMembers_expectError() {
        ReviewCommand command = command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW));

        List<Finding> findings = validator.validateComposition(command, rules(2));

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.COMPOSITION_SIZE);
            assertThat(finding.getMessage()).isEqualTo("Composition has 1 securities but the SMI needs 2");
        });
    }

    @Test
    void validateInput_whenCompositionValid_expectNoFindings() {
        ReviewCommand command = command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW));

        List<Finding> findings = validator.validateComposition(command, rules(1));

        assertThat(findings).isEmpty();
    }

    @Test
    void validateCandidates_whenTooFew_expectError() {
        ReviewCommand command = command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW));
        List<Candidate> candidates = List.of(candidate(10));

        List<Finding> findings = validator.validateCandidates(command, candidates, rules(2));

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.TOO_FEW_ELIGIBLE);
            assertThat(finding.getMessage()).isEqualTo("Only 1 securities have a valid FFMCAP but the SMI needs 2");
        });
    }

    @Test
    void validateCandidates_whenEnough_expectNoFindings() {
        ReviewCommand command = command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW));
        List<Candidate> candidates = List.of(candidate(10), candidate(11));

        List<Finding> findings = validator.validateCandidates(command, candidates, rules(2));

        assertThat(findings).isEmpty();
    }

    @Test
    void validateDates_whenAllDatesPresent_expectNoIssues() {
        List<Finding> findings = validator.validateDates(command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW)));

        assertThat(findings).isEmpty();
    }

    @Test
    void validateDates_whenUniverseDateMissing_expectError() {
        LocalDate otherDate = LocalDate.of(2026, 9, 20);

        List<Finding> findings = validator.validateDates(command(otherDate, List.of(CUTOFF_ROW, REVIEW_ROW)));

        assertThat(findings).containsExactly(Finding.builder()
            .code(FindingCode.MISSING_DATE)
            .field("universe")
            .message("Universe has no rows for the review date 2026-09-21; available dates: [2026-09-20]")
            .build());
    }

    @Test
    void validateDates_whenCutoffRowsMissing_expectError() {
        List<Finding> findings = validator.validateDates(command(REVIEW_DATE, List.of(REVIEW_ROW)));

        assertThat(findings).containsExactly(Finding.builder()
            .code(FindingCode.MISSING_DATE)
            .field("securityData")
            .message("Security data has no rows for the cut-off date 2026-09-10; available dates: [2026-09-21]")
            .build());
    }

    @Test
    void validateDates_whenReviewRowsMissing_expectError() {
        List<Finding> findings = validator.validateDates(command(REVIEW_DATE, List.of(CUTOFF_ROW)));

        assertThat(findings).containsExactly(Finding.builder()
            .code(FindingCode.MISSING_DATE)
            .field("securityData")
            .message("Security data has no rows for the review date 2026-09-21; available dates: [2026-09-10]")
            .build());
    }

    @Test
    void validateDates_whenUniverseEmptyOnDate_expectError() {
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, Set.of()))
            .securities(List.of(CUTOFF_ROW, REVIEW_ROW))
            .composition(List.of(10))
            .build();

        List<Finding> findings = validator.validateDates(command);

        assertThat(findings).extracting(Finding::getField).containsExactly("universe");
    }

    @Test
    void validateDates_whenAllInputEmpty_expectThreeErrors() {
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of())
            .securities(List.of())
            .composition(List.of())
            .build();

        List<Finding> findings = validator.validateDates(command);

        assertThat(findings).hasSize(3).extracting(Finding::getCode).containsOnly(FindingCode.MISSING_DATE);
    }

    @Test
    void validateDates_whenExtraDates_expectNoIssues() {
        LocalDate otherDate = LocalDate.of(2026, 6, 19);
        Security olderRow = Security.builder()
            .securityId(10)
            .date(otherDate)
            .price(new BigDecimal("1900"))
            .build();
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, Set.of(10), otherDate, Set.of(10, 20)))
            .securities(List.of(olderRow, CUTOFF_ROW, REVIEW_ROW))
            .composition(List.of(10))
            .build();

        List<Finding> findings = validator.validateDates(command);

        assertThat(findings).isEmpty();
    }

    @Test
    void validateDates_whenDatesEqual_expectNoIssues() {
        Security fullRow =
            Security.builder()
                .securityId(10)
                .date(REVIEW_DATE)
                .price(new BigDecimal("2080"))
                .freeFloat(new BigDecimal("0.6766"))
                .shares(new BigDecimal("1"))
                .build();
        ReviewCommand command = ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(REVIEW_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(REVIEW_DATE, Set.of(10)))
            .securities(List.of(fullRow))
            .composition(List.of(10))
            .build();

        List<Finding> findings = validator.validateDates(command);

        assertThat(findings).isEmpty();
    }

    private static IndexRules rules(int numberOfComponents) {
        return IndexRules.builder()
            .numberOfComponents(numberOfComponents)
            .directSelectionRank(numberOfComponents)
            .bufferRank(numberOfComponents)
            .capPercentage(new BigDecimal("18"))
            .build();
    }

    private static Candidate candidate(int securityId) {
        return Candidate.builder()
            .securityId(securityId)
            .ffmcap(new BigDecimal("1000"))
            .build();
    }

    private static ReviewCommand command(LocalDate universeDate, List<Security> rows) {
        return ReviewCommand.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .universe(Map.of(universeDate, Set.of(10)))
            .securities(rows)
            .composition(List.of(10))
            .build();
    }
}
