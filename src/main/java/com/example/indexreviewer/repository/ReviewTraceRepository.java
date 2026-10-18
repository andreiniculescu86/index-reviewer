package com.example.indexreviewer.repository;

import com.example.indexreviewer.model.trace.FfmcapTrace;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SelectionTrace;
import com.example.indexreviewer.model.trace.WeightTrace;

import java.util.List;
import java.util.UUID;

/**
 * Stores the trace of review executions while the review runs. Each review step appends its records as one batch;
 * stored records are never changed or deleted.
 */
public interface ReviewTraceRepository {

    void appendFindings(UUID reviewId, List<Finding> findings);

    void appendFfmcap(UUID reviewId, List<FfmcapTrace> securities);

    void appendSelection(UUID reviewId, List<SelectionTrace> securities);

    void appendWeights(UUID reviewId, List<WeightTrace> securities);

    /**
     * Returns the trace combined from all stored records; it is empty when nothing was stored for the review.
     */
    ReviewTrace findByReviewId(UUID reviewId);
}
