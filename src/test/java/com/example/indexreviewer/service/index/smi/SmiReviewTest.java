package com.example.indexreviewer.service.index.smi;

import com.example.indexreviewer.exception.InvalidReviewDataException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.SecurityRecord;
import com.example.indexreviewer.service.review.FfmcapCalculator;
import com.example.indexreviewer.service.review.ReviewDataValidator;
import com.example.indexreviewer.service.review.SecurityRecordValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmiReviewTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);
    private static final List<Integer> CURRENT_SMI = IntStream.rangeClosed(1, 20).boxed().toList();

    private final SmiReview review = new SmiReview(new SmiConfiguration(20, 18, 22, new BigDecimal("18")),
        new FfmcapCalculator(new SecurityRecordValidator()), new ReviewDataValidator());

    @Test
    void indexType_whenAsked_expectSmi() {
        IndexType indexType = review.indexType();

        assertThat(indexType).isEqualTo(IndexType.SMI);
    }

    @Test
    void review_whenCompositionHas19_expectError() {
        ReviewCommand command = command(validRows(25), CURRENT_SMI.subList(0, 19));

        assertThatThrownBy(() -> review.review(command))
            .isInstanceOfSatisfying(InvalidReviewDataException.class,
                exception -> assertThat(exception.getErrors()).singleElement().asString().contains("19"));
    }

    @Test
    void review_whenCompositionHasDuplicate_expectError() {
        List<Integer> composition = new ArrayList<>(CURRENT_SMI.subList(0, 19));
        composition.add(1);
        ReviewCommand command = command(validRows(25), composition);

        assertThatThrownBy(() -> review.review(command))
            .isInstanceOf(InvalidReviewDataException.class);
    }

    @Test
    void review_whenNoReviewDateData_expectError() {
        List<SecurityRecord> cutoffRowsOnly = validRows(25).stream()
            .filter(row -> CUTOFF_DATE.equals(row.date()))
            .toList();
        ReviewCommand command = command(cutoffRowsOnly, CURRENT_SMI);

        assertThatThrownBy(() -> review.review(command))
            .isInstanceOfSatisfying(InvalidReviewDataException.class,
                exception -> assertThat(exception.getErrors()).anyMatch(error -> error.contains("2026-09-21")));
    }

    @Test
    void review_whenOnly19Eligible_expectError() {
        ReviewCommand command = command(validRows(19), CURRENT_SMI);

        assertThatThrownBy(() -> review.review(command))
            .isInstanceOfSatisfying(InvalidReviewDataException.class,
                exception -> assertThat(exception.getErrors()).singleElement().asString().contains("19"));
    }

    @Test
    void review_whenRowsInvalid_expectAllErrors() {
        List<SecurityRecord> rows = new ArrayList<>(validRows(22));
        rows.add(cutoffRow(23, "-2080"));
        rows.add(reviewRow(23, "0.6766"));
        rows.add(cutoffRow(24, "898"));
        rows.add(reviewRow(24, "1.5"));
        ReviewCommand command = command(rows, CURRENT_SMI);

        assertThatThrownBy(() -> review.review(command))
            .isInstanceOfSatisfying(InvalidReviewDataException.class,
                exception -> assertThat(exception.getErrors()).hasSize(2));
    }

    private static ReviewCommand command(List<SecurityRecord> rows, List<Integer> composition) {
        Set<Integer> universe = new HashSet<>();
        rows.forEach(row -> universe.add(row.securityId()));
        return new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE, Map.of(REVIEW_DATE, universe), rows,
            composition);
    }

    private static List<SecurityRecord> validRows(int count) {
        List<SecurityRecord> rows = new ArrayList<>();
        for (int id = 1; id <= count; id++) {
            rows.add(cutoffRow(id, String.valueOf(100 + id)));
            rows.add(reviewRow(id, "0.5"));
        }
        return rows;
    }

    private static SecurityRecord cutoffRow(int id, String price) {
        return new SecurityRecord(id, CUTOFF_DATE, new BigDecimal(price), new BigDecimal("0.5"),
            new BigDecimal("1000000"));
    }

    private static SecurityRecord reviewRow(int id, String freeFloat) {
        return new SecurityRecord(id, REVIEW_DATE, null, new BigDecimal(freeFloat), new BigDecimal("1000000"));
    }
}
