package com.example.indexreviewer.service.review.smi;

import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
        List<Finding> findings = validator.validateCutoff(SECURITY_ID, cutoffRow(price));

        assertThat(findings).isEmpty();
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
        List<Finding> findings = validator.validateCutoff(SECURITY_ID, cutoffRow(price));

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.INVALID_VALUE);
            assertThat(finding.getSeverity()).isEqualTo(Severity.ERROR);
            assertThat(finding.getSecurityId()).isEqualTo(SECURITY_ID);
            assertThat(finding.getField()).isEqualTo("price");
        });
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
    void validateCutoff_whenPriceNegative_expectValue() {
        List<Finding> findings = validator.validateCutoff(SECURITY_ID, cutoffRow("-2080"));

        assertThat(findings).containsExactly(Finding.builder()
            .code(FindingCode.INVALID_VALUE)
            .securityId(SECURITY_ID)
            .field("price")
            .message("Security 7: Price must be positive but is -2080")
            .build());
    }

    @Test
    void validateCutoff_whenRowMissing_expectWarningOnly() {
        List<Finding> findings = validator.validateCutoff(SECURITY_ID, null);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.MISSING_CUTOFF_PRICE);
            assertThat(finding.getSeverity()).isEqualTo(Severity.WARNING);
            assertThat(finding.getSecurityId()).isEqualTo(SECURITY_ID);
        });
    }

    @Test
    void validateCutoff_whenPriceBlank_expectWarningOnly() {
        List<Finding> findings = validator.validateCutoff(SECURITY_ID,
            Security.builder()
                .securityId(SECURITY_ID)
                .date(CUTOFF_DATE)
                .freeFloat(new BigDecimal("0.5"))
                .shares(new BigDecimal("1000"))
                .build());

        assertThat(findings).extracting(Finding::getCode).containsExactly(FindingCode.MISSING_CUTOFF_PRICE);
    }

    @Test
    void validateCutoff_whenUnusedInvalid_expectNoIssues() {
        Security row = Security.builder()
            .securityId(SECURITY_ID)
            .date(CUTOFF_DATE)
            .price(new BigDecimal("2080"))
            .freeFloat(new BigDecimal("1.5"))
            .shares(BigDecimal.ZERO)
            .build();

        List<Finding> findings = validator.validateCutoff(SECURITY_ID, row);

        assertThat(findings).isEmpty();
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("validReviewValues")
    void validateReview_whenValuesValid_expectNoIssues(String freeFloat, String shares) {
        List<Finding> findings = validator.validateReview(SECURITY_ID, reviewRow(null, freeFloat, shares));

        assertThat(findings).isEmpty();
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
    void validateReview_whenValuesInvalid_expectError(String freeFloat, String shares, String field) {
        List<Finding> findings = validator.validateReview(SECURITY_ID, reviewRow(null, freeFloat, shares));

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.INVALID_VALUE);
            assertThat(finding.getField()).isEqualTo(field);
        });
    }

    static Stream<Arguments> invalidReviewValues() {
        return Stream.of(
            Arguments.argumentSet("free float zero", "0", "1000", "freeFloat"),
            Arguments.argumentSet("free float negative", "-0.5", "1000", "freeFloat"),
            Arguments.argumentSet("free float just above 1", "1.0001", "1000", "freeFloat"),
            Arguments.argumentSet("free float as percent", "67.66", "1000", "freeFloat"),
            Arguments.argumentSet("free float missing", null, "1000", "freeFloat"),
            Arguments.argumentSet("shares zero", "0.5", "0", "shares"),
            Arguments.argumentSet("shares negative", "0.5", "-100", "shares"),
            Arguments.argumentSet("shares missing", "0.5", null, "shares"),
            Arguments.argumentSet("shares huge exponent", "0.5", "1E+25", "shares"),
            Arguments.argumentSet("free float 21 decimal places", "0.000000000000000000001", "1000", "freeFloat"));
    }

    @Test
    void validateReview_whenBothMissing_expectTwoErrors() {
        List<Finding> findings = validator.validateReview(SECURITY_ID, reviewRow(null, null, null));

        assertThat(findings).extracting(Finding::getField).containsExactly("shares", "freeFloat");
    }

    @Test
    void validateReview_whenRowMissing_expectWarningOnly() {
        List<Finding> findings = validator.validateReview(SECURITY_ID, null);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.MISSING_REVIEW_DATA);
            assertThat(finding.getSeverity()).isEqualTo(Severity.WARNING);
            assertThat(finding.getSecurityId()).isEqualTo(SECURITY_ID);
        });
    }

    @Test
    void validateReview_whenPriceFilled_expectNoIssues() {
        List<Finding> findings = validator.validateReview(SECURITY_ID, reviewRow("2080", "0.6766", "309375"));

        assertThat(findings).isEmpty();
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("identicalRows")
    void checkDuplicate_whenRowsIdentical_expectWarning(Security first, Security second) {
        Finding finding = validator.checkDuplicate(first, second, first.getDate());

        assertThat(finding.getCode()).isEqualTo(FindingCode.DUPLICATE_ROW);
        assertThat(finding.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(finding.getMessage()).contains(first.getDate().toString());
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
    void checkDuplicate_whenRowsConflict_expectError(Security first, Security second) {
        Finding finding = validator.checkDuplicate(first, second, first.getDate());

        assertThat(finding.getCode()).isEqualTo(FindingCode.CONFLICTING_DUPLICATE);
        assertThat(finding.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(finding.getSecurityId()).isEqualTo(SECURITY_ID);
    }

    static Stream<Arguments> conflictingRows() {
        return Stream.of(
            Arguments.argumentSet("different prices",
                cutoffRow("2080"), cutoffRow("2090")),
            Arguments.argumentSet("price only in one row",
                cutoffRow("2080"), Security.builder()
                    .securityId(SECURITY_ID)
                    .date(CUTOFF_DATE)
                    .build()),
            Arguments.argumentSet("different shares",
                reviewRow(null, "0.5", "1000"), reviewRow(null, "0.5", "1001")),
            Arguments.argumentSet("different free floats",
                reviewRow(null, "0.5", "1000"), reviewRow(null, "0.6", "1000")));
    }

    private static Security cutoffRow(String price) {
        return Security.builder()
            .securityId(SECURITY_ID)
            .date(CUTOFF_DATE)
            .price(new BigDecimal(price))
            .build();
    }

    private static Security reviewRow(String price, String freeFloat, String shares) {
        return Security.builder()
            .securityId(SECURITY_ID)
            .date(REVIEW_DATE)
            .price(decimal(price))
            .freeFloat(decimal(freeFloat))
            .shares(decimal(shares))
            .build();
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
