package com.example.indexreviewer.service.index;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewResult;

public interface IndexReview {

    IndexType indexType();

    ReviewResult review(ReviewCommand command);
}
