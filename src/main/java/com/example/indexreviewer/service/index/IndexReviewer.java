package com.example.indexreviewer.service.index;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.report.ReviewReport;

public interface IndexReviewer {

    IndexType indexType();

    /**
     * Runs the review under a new review id, created by the implementation, and returns its report. The trace is stored
     * step by step while the review runs and the audit record at its end, also when the review fails or throws.
     */
    ReviewReport review(ReviewCommand command);
}
