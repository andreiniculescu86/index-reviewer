package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.SelectionResult;
import com.example.indexreviewer.model.audit.IndexRules;

import java.util.List;

/**
 * Review step that ranks the candidates and selects the index components.
 */
public interface ComponentSelectorStep {

    /**
     * Returns the selected candidates in rank order, the trace record of every candidate and a warning for every group
     * of equal FFMCAPs that the tie order split into selected and not selected candidates.
     */
    SelectionResult selectTopCandidates(List<Candidate> candidates, List<Integer> composition, IndexRules rules);
}
