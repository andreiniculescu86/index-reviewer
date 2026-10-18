package com.example.indexreviewer.service.index.smi;

import com.example.indexreviewer.model.audit.IndexRules;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties("indices.smi")
public record SmiConfiguration(
    @Positive int numberOfComponents,
    @Positive int directSelectionRank,
    @Positive int bufferRank,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal capPercentage,
    @NotBlank String methodologyVersion) {

    /**
     * The direct selection and the buffer must be able to fill the index: direct-selection-rank must not be greater
     * than number-of-components, and number-of-components must not be greater than buffer-rank.
     */
    @AssertTrue(message = "direct-selection-rank must not be greater than number-of-components, and "
        + "number-of-components must not be greater than buffer-rank")
    public boolean isRankOrderValid() {
        return directSelectionRank <= numberOfComponents && numberOfComponents <= bufferRank;
    }

    /**
     * The capped weights must be able to add up to 100%: number-of-components x cap-percentage must be at least 100. A
     * missing cap-percentage is reported by its own constraint.
     */
    @AssertTrue(message = "number-of-components x cap-percentage must be at least 100")
    public boolean isCapFeasible() {
        return capPercentage == null
            || capPercentage.multiply(BigDecimal.valueOf(numberOfComponents)).compareTo(BigDecimal.valueOf(100)) >= 0;
    }

    public IndexRules toRules() {
        return IndexRules.builder()
            .numberOfComponents(numberOfComponents)
            .directSelectionRank(directSelectionRank)
            .bufferRank(bufferRank)
            .capPercentage(capPercentage)
            .methodologyVersion(methodologyVersion)
            .build();
    }
}
