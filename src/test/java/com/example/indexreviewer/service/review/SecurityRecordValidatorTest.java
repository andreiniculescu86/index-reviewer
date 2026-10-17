package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.RowValidation;
import com.example.indexreviewer.model.SecurityRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRecordValidatorTest {

    private static final int SECURITY_ID = 7;
    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);

    private final SecurityRecordValidator validator = new SecurityRecordValidator();

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("validPrices")
    void validateCutoff_whenPriceValid_expectNoIssues(String price) {
        RowValidation validation = validator.validateCutoff(SECURITY_ID, cutoffRow(price));

        assertThat(validation.hasNoIssues()).isTrue();
    }

    static Stream<Arguments> validPrices() {
        return Stream.of(
            Arguments.argumentSet("regular price", "2080"),
            Arguments.argumentSet("price below 1", "0.048"),
            Arguments.argumentSet("high price", "126800"),
            Arguments.argumentSet("scientific notation", "2.08E+3"),
            Arguments.argumentSet("20 decimal places", "0.00000000000000000001"),
            Arguments.argumentSet("20 integer digits", "99999999999999999999"));
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidPrices")
    void validateCutoff_whenPriceInvalid_expectError(String price) {
        RowValidation validation = validator.validateCutoff(SECURITY_ID, cutoffRow(price));

        assertThat(validation.getErrors()).hasSize(1);
        assertThat(validation.getWarnings()).isEmpty();
    }

    static Stream<Arguments> invalidPrices() {
        return Stream.of(
            Arguments.argumentSet("zero", "0"),
            Arguments.argumentSet("zero with decimals", "0.00"),
            Arguments.argumentSet("negative", "-1"),
            Arguments.argumentSet("small negative", "-0.01"),
            Arguments.argumentSet("21 decimal places", "0.000000000000000000001"),
            Arguments.argumentSet("21 integer digits", "123456789012345678901"),
            Arguments.argumentSet("huge exponent", "1E+25"),
            Arguments.argumentSet("tiny exponent", "1E-1500000000"));
    }

    @Test
    void validateCutoff_whenRowMissing_expectWarningOnly() {
        RowValidation validation = validator.validateCutoff(SECURITY_ID, null);

        assertThat(validation.getErrors()).isEmpty();
        assertThat(validation.getWarnings()).hasSize(1);
    }

    @Test
    void validateCutoff_whenPriceBlank_expectWarningOnly() {
        RowValidation validation = validator.validateCutoff(SECURITY_ID,
            new SecurityRecord(SECURITY_ID, CUTOFF_DATE, null, new BigDecimal("0.5"), new BigDecimal("1000")));

        assertThat(validation.getErrors()).isEmpty();
        assertThat(validation.getWarnings()).hasSize(1);
    }

    @Test
    void validateCutoff_whenUnusedInvalid_expectNoIssues() {
        SecurityRecord row = new SecurityRecord(SECURITY_ID, CUTOFF_DATE, new BigDecimal("2080"),
            new BigDecimal("1.5"), BigDecimal.ZERO);

        RowValidation validation = validator.validateCutoff(SECURITY_ID, row);

        assertThat(validation.hasNoIssues()).isTrue();
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("validReviewValues")
    void validateReview_whenValuesValid_expectNoIssues(String freeFloat, String shares) {
        RowValidation validation = validator.validateReview(SECURITY_ID, reviewRow(null, freeFloat, shares));

        assertThat(validation.hasNoIssues()).isTrue();
    }

    static Stream<Arguments> validReviewValues() {
        return Stream.of(
            Arguments.argumentSet("regular values", "0.6766", "309375"),
            Arguments.argumentSet("free float exactly 1", "1", "45867891"),
            Arguments.argumentSet("free float 1 with decimals", "1.0000", "45867891"),
            Arguments.argumentSet("free float below 20 percent", "0.0289", "7730394"),
            Arguments.argumentSet("single share", "0.5", "1"),
            Arguments.argumentSet("largest real share count", "0.918", "3341581714"));
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidReviewValues")
    void validateReview_whenValuesInvalid_expectError(String freeFloat, String shares) {
        RowValidation validation = validator.validateReview(SECURITY_ID, reviewRow(null, freeFloat, shares));

        assertThat(validation.getErrors()).hasSize(1);
        assertThat(validation.getWarnings()).isEmpty();
    }

    static Stream<Arguments> invalidReviewValues() {
        return Stream.of(
            Arguments.argumentSet("free float zero", "0", "1000"),
            Arguments.argumentSet("free float negative", "-0.5", "1000"),
            Arguments.argumentSet("free float just above 1", "1.0001", "1000"),
            Arguments.argumentSet("free float as percent", "67.66", "1000"),
            Arguments.argumentSet("free float missing", null, "1000"),
            Arguments.argumentSet("shares zero", "0.5", "0"),
            Arguments.argumentSet("shares negative", "0.5", "-100"),
            Arguments.argumentSet("shares missing", "0.5", null),
            Arguments.argumentSet("shares huge exponent", "0.5", "1E+25"),
            Arguments.argumentSet("free float 21 decimal places", "0.000000000000000000001", "1000"));
    }

    @Test
    void validateReview_whenBothMissing_expectTwoErrors() {
        RowValidation validation = validator.validateReview(SECURITY_ID, reviewRow(null, null, null));

        assertThat(validation.getErrors()).hasSize(2);
    }

    @Test
    void validateReview_whenRowMissing_expectWarningOnly() {
        RowValidation validation = validator.validateReview(SECURITY_ID, null);

        assertThat(validation.getErrors()).isEmpty();
        assertThat(validation.getWarnings()).hasSize(1);
    }

    @Test
    void validateReview_whenPriceFilled_expectNoIssues() {
        RowValidation validation = validator.validateReview(SECURITY_ID, reviewRow("2080", "0.6766", "309375"));

        assertThat(validation.hasNoIssues()).isTrue();
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("identicalRows")
    void checkDuplicate_whenRowsIdentical_expectWarning(SecurityRecord first, SecurityRecord second) {
        RowValidation validation = validator.checkDuplicate(first, second, first.date());

        assertThat(validation.getErrors()).isEmpty();
        assertThat(validation.getWarnings()).hasSize(1);
    }

    static Stream<Arguments> identicalRows() {
        return Stream.of(
            Arguments.argumentSet("same cut-off rows",
                cutoffRow("2080"), cutoffRow("2080")),
            Arguments.argumentSet("same review rows without price",
                reviewRow(null, "0.6766", "309375"), reviewRow(null, "0.6766", "309375")),
            Arguments.argumentSet("same values with different scale",
                reviewRow(null, "0.5", "1000"), reviewRow(null, "0.50", "1000.0")));
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("conflictingRows")
    void checkDuplicate_whenRowsConflict_expectError(SecurityRecord first, SecurityRecord second) {
        RowValidation validation = validator.checkDuplicate(first, second, first.date());

        assertThat(validation.getErrors()).hasSize(1);
        assertThat(validation.getWarnings()).isEmpty();
    }

    static Stream<Arguments> conflictingRows() {
        return Stream.of(
            Arguments.argumentSet("different prices",
                cutoffRow("2080"), cutoffRow("2090")),
            Arguments.argumentSet("price only in one row",
                cutoffRow("2080"), new SecurityRecord(SECURITY_ID, CUTOFF_DATE, null, null, null)),
            Arguments.argumentSet("different shares",
                reviewRow(null, "0.5", "1000"), reviewRow(null, "0.5", "1001")),
            Arguments.argumentSet("different free floats",
                reviewRow(null, "0.5", "1000"), reviewRow(null, "0.6", "1000")));
    }

    private static SecurityRecord cutoffRow(String price) {
        return new SecurityRecord(SECURITY_ID, CUTOFF_DATE, new BigDecimal(price), null, null);
    }

    private static SecurityRecord reviewRow(String price, String freeFloat, String shares) {
        return new SecurityRecord(SECURITY_ID, REVIEW_DATE, decimal(price), decimal(freeFloat), decimal(shares));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
