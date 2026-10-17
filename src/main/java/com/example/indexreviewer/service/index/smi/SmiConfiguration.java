package com.example.indexreviewer.service.index.smi;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal capPercentage) {
}
