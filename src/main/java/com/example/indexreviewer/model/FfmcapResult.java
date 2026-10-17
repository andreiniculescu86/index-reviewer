package com.example.indexreviewer.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * FFMCAP per security id, together with the validation result of the data used to calculate it.
 */
public record FfmcapResult(Map<Integer, BigDecimal> ffmcaps, ValidationResult validation) {
}
