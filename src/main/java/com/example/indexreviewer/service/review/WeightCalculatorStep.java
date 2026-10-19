package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.audit.IndexRules;

import java.util.List;

/**
 * Review step that weights the selected components.
 */
public interface WeightCalculatorStep {

    /**
     * Returns the components in the given order with their weighting factors, the trace records and the findings of
     * the weighting.
     */
    WeightResult calculateWeights(List<Candidate> selectedComponents, IndexRules rules);
}
