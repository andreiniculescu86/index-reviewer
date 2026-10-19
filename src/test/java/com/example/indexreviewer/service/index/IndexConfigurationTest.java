package com.example.indexreviewer.service.index;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndexConfigurationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenRanksInOrder_expectNoViolation() {
        IndexConfiguration configuration = configuration(18, 20, 22);

        Set<ConstraintViolation<IndexConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_whenBufferBelowComponents_expectViolation() {
        IndexConfiguration configuration = configuration(18, 20, 19);

        Set<ConstraintViolation<IndexConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("rankOrderValid"));
    }

    @Test
    void validate_whenDirectAboveComponents_expectViolation() {
        IndexConfiguration configuration = configuration(21, 20, 22);

        Set<ConstraintViolation<IndexConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("rankOrderValid"));
    }

    @Test
    void validate_whenCapTooLow_expectViolation() {
        IndexConfiguration configuration = new IndexConfiguration(20, 18, 22, new BigDecimal("4.99"),
            "rulebook-v3.40, 08.06.2026");

        Set<ConstraintViolation<IndexConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("capFeasible"));
    }

    @Test
    void validate_whenCapAddsUpTo100_expectNoViolation() {
        IndexConfiguration configuration = new IndexConfiguration(20, 18, 22, new BigDecimal("5"),
            "rulebook-v3.40, 08.06.2026");

        Set<ConstraintViolation<IndexConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).isEmpty();
    }

    private static IndexConfiguration configuration(int directSelectionRank, int numberOfComponents, int bufferRank) {
        return new IndexConfiguration(numberOfComponents, directSelectionRank, bufferRank, new BigDecimal("18"),
            "rulebook-v3.40, 08.06.2026");
    }
}
