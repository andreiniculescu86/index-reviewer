package com.example.indexreviewer.model;

import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewStatusTest {

    private static final Finding ERROR = Finding.builder()
        .code(FindingCode.INVALID_VALUE)
        .securityId(10)
        .field("price")
        .message("invalid price")
        .build();
    private static final Finding WARNING = Finding.builder()
        .code(FindingCode.MISSING_REVIEW_DATA)
        .securityId(166)
        .message("excluded")
        .build();
    private static final Finding INFO = Finding.builder()
        .code(FindingCode.IGNORED_SECURITIES)
        .field("securityData")
        .message("ignored")
        .build();

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("findings")
    void of_whenFindingsGiven_expectStatus(List<Finding> findings, ReviewStatus expected) {
        ReviewStatus status = ReviewStatus.of(findings);

        assertThat(status).isEqualTo(expected);
    }

    static Stream<Arguments> findings() {
        return Stream.of(
            Arguments.argumentSet("no findings", List.of(), ReviewStatus.SUCCESS),
            Arguments.argumentSet("info only", List.of(INFO), ReviewStatus.SUCCESS),
            Arguments.argumentSet("warning", List.of(INFO, WARNING), ReviewStatus.SUCCESS_WITH_WARNINGS),
            Arguments.argumentSet("error and warning", List.of(WARNING, ERROR), ReviewStatus.FAILED));
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("statusPairs")
    void getMostSevere_whenTwoStatuses_expectWorse(ReviewStatus current, ReviewStatus other, ReviewStatus expected) {
        ReviewStatus status = current.getMostSevere(other);

        assertThat(status).isEqualTo(expected);
    }

    static Stream<Arguments> statusPairs() {
        return Stream.of(
            Arguments.argumentSet("same status", ReviewStatus.SUCCESS, ReviewStatus.SUCCESS, ReviewStatus.SUCCESS),
            Arguments.argumentSet("warning after success", ReviewStatus.SUCCESS, ReviewStatus.SUCCESS_WITH_WARNINGS,
                ReviewStatus.SUCCESS_WITH_WARNINGS),
            Arguments.argumentSet("success after warning", ReviewStatus.SUCCESS_WITH_WARNINGS, ReviewStatus.SUCCESS,
                ReviewStatus.SUCCESS_WITH_WARNINGS),
            Arguments.argumentSet("failed after warning", ReviewStatus.SUCCESS_WITH_WARNINGS, ReviewStatus.FAILED,
                ReviewStatus.FAILED));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ReviewStatus.class, names = {"SUCCESS", "SUCCESS_WITH_WARNINGS"})
    void isSuccessful_whenCompleted_expectTrue(ReviewStatus status) {
        boolean successful = status.isSuccessful();

        assertThat(successful).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = ReviewStatus.class, names = {"FAILED", "ERROR"})
    void isSuccessful_whenFailedOrError_expectFalse(ReviewStatus status) {
        boolean successful = status.isSuccessful();

        assertThat(successful).isFalse();
    }
}
