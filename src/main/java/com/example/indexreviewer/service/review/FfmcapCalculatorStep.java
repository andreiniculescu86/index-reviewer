package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.ReviewCommand;

/**
 * Review step that calculates the FFMCAP of the eligible universe members.
 */
public interface FfmcapCalculatorStep {

    /**
     * Returns the candidates ordered by security id, with the trace records and the findings of the data used.
     */
    FfmcapResult calculate(ReviewCommand command);
}
