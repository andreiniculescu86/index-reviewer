package com.example.indexreviewer.service;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewResult;
import com.example.indexreviewer.service.index.IndexReview;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final Map<IndexType, IndexReview> reviews;

    public ReviewService(List<IndexReview> implementations) {
        reviews = implementations.stream().collect(Collectors.toMap(IndexReview::indexType, Function.identity()));
    }

    public ReviewResult runReview(ReviewCommand command) {
        IndexReview review = reviews.get(command.indexType());
        if (review == null) {
            throw new IndexNotConfiguredException(command.indexType());
        }
        return review.review(command);
    }
}
