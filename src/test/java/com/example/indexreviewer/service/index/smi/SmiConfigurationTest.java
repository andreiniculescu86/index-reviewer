package com.example.indexreviewer.service.index.smi;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmiConfigurationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validate_whenRanksInOrder_expectNoViolation() {
        SmiConfiguration configuration = configuration(18, 20, 22);

        Set<ConstraintViolation<SmiConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_whenBufferBelowComponents_expectViolation() {
        SmiConfiguration configuration = configuration(18, 20, 19);

        Set<ConstraintViolation<SmiConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("rankOrderValid"));
    }

    @Test
    void validate_whenDirectAboveComponents_expectViolation() {
        SmiConfiguration configuration = configuration(21, 20, 22);

        Set<ConstraintViolation<SmiConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("rankOrderValid"));
    }

    @Test
    void validate_whenCapTooLow_expectViolation() {
        SmiConfiguration configuration = new SmiConfiguration(20, 18, 22, new BigDecimal("4.99"),
            "SIX Swiss Index Methodology Rulebook 3.40, 08.06.2026");

        Set<ConstraintViolation<SmiConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).singleElement()
            .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("capFeasible"));
    }

    @Test
    void validate_whenCapAddsUpTo100_expectNoViolation() {
        SmiConfiguration configuration = new SmiConfiguration(20, 18, 22, new BigDecimal("5"),
            "SIX Swiss Index Methodology Rulebook 3.40, 08.06.2026");

        Set<ConstraintViolation<SmiConfiguration>> violations = validator.validate(configuration);

        assertThat(violations).isEmpty();
    }

    private static SmiConfiguration configuration(int directSelectionRank, int numberOfComponents, int bufferRank) {
        return new SmiConfiguration(numberOfComponents, directSelectionRank, bufferRank, new BigDecimal("18"),
            "SIX Swiss Index Methodology Rulebook 3.40, 08.06.2026");
    }
}
