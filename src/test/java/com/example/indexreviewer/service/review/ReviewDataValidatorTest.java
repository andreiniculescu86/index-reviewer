package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.RowValidation;
import com.example.indexreviewer.model.SecurityRecord;
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
    private static final SecurityRecord CUTOFF_ROW =
        new SecurityRecord(10, CUTOFF_DATE, new BigDecimal("2080"), null, null);
    private static final SecurityRecord REVIEW_ROW =
        new SecurityRecord(10, REVIEW_DATE, null, new BigDecimal("0.6766"), new BigDecimal("309375"));

    private final ReviewDataValidator validator = new ReviewDataValidator();

    @Test
    void validateDates_whenAllDatesPresent_expectNoIssues() {
        RowValidation validation = validator.validateDates(command(REVIEW_DATE, List.of(CUTOFF_ROW, REVIEW_ROW)));

        assertThat(validation.hasNoIssues()).isTrue();
    }

    @Test
    void validateDates_whenUniverseDateMissing_expectError() {
        LocalDate otherDate = LocalDate.of(2026, 9, 20);

        RowValidation validation = validator.validateDates(command(otherDate, List.of(CUTOFF_ROW, REVIEW_ROW)));

        assertThat(validation.getErrors()).containsExactly(
            "Universe has no rows for the review date 2026-09-21; available dates: [2026-09-20]");
    }

    @Test
    void validateDates_whenCutoffRowsMissing_expectError() {
        RowValidation validation = validator.validateDates(command(REVIEW_DATE, List.of(REVIEW_ROW)));

        assertThat(validation.getErrors()).containsExactly(
            "Security data has no rows for the cut-off date 2026-09-10; available dates: [2026-09-21]");
    }

    @Test
    void validateDates_whenReviewRowsMissing_expectError() {
        RowValidation validation = validator.validateDates(command(REVIEW_DATE, List.of(CUTOFF_ROW)));

        assertThat(validation.getErrors()).containsExactly(
            "Security data has no rows for the review date 2026-09-21; available dates: [2026-09-10]");
    }

    @Test
    void validateDates_whenUniverseEmptyOnDate_expectError() {
        ReviewCommand command = new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE,
            Map.of(REVIEW_DATE, Set.of()), List.of(CUTOFF_ROW, REVIEW_ROW), List.of(10));

        RowValidation validation = validator.validateDates(command);

        assertThat(validation.getErrors()).hasSize(1);
    }

    @Test
    void validateDates_whenAllInputEmpty_expectThreeErrors() {
        ReviewCommand command = new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE, Map.of(), List.of(),
            List.of());

        RowValidation validation = validator.validateDates(command);

        assertThat(validation.getErrors()).hasSize(3);
    }

    @Test
    void validateDates_whenExtraDates_expectNoIssues() {
        LocalDate otherDate = LocalDate.of(2026, 6, 19);
        SecurityRecord olderRow = new SecurityRecord(10, otherDate, new BigDecimal("1900"), null, null);
        ReviewCommand command = new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE,
            Map.of(REVIEW_DATE, Set.of(10), otherDate, Set.of(10, 20)), List.of(olderRow, CUTOFF_ROW, REVIEW_ROW),
            List.of(10));

        RowValidation validation = validator.validateDates(command);

        assertThat(validation.hasNoIssues()).isTrue();
    }

    @Test
    void validateDates_whenDatesEqual_expectNoIssues() {
        SecurityRecord fullRow =
            new SecurityRecord(10, REVIEW_DATE, new BigDecimal("2080"), new BigDecimal("0.6766"), new BigDecimal("1"));
        ReviewCommand command = new ReviewCommand(IndexType.SMI, REVIEW_DATE, REVIEW_DATE,
            Map.of(REVIEW_DATE, Set.of(10)), List.of(fullRow), List.of(10));

        RowValidation validation = validator.validateDates(command);

        assertThat(validation.hasNoIssues()).isTrue();
    }

    private static ReviewCommand command(LocalDate universeDate, List<SecurityRecord> rows) {
        return new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE, Map.of(universeDate, Set.of(10)), rows,
            List.of(10));
    }
}
