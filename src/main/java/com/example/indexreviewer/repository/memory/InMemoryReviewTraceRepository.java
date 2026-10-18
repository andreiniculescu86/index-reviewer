package com.example.indexreviewer.repository.memory;

import com.example.indexreviewer.model.trace.FfmcapTrace;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SelectionTrace;
import com.example.indexreviewer.model.trace.WeightTrace;
import com.example.indexreviewer.repository.ReviewTraceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps trace records in memory, one list per record type and review. The records are lost when the application
 * stops.
 */
@Repository
public class InMemoryReviewTraceRepository implements ReviewTraceRepository {

    private final Map<UUID, List<Finding>> findings = new ConcurrentHashMap<>();
    private final Map<UUID, List<FfmcapTrace>> ffmcap = new ConcurrentHashMap<>();
    private final Map<UUID, List<SelectionTrace>> selection = new ConcurrentHashMap<>();
    private final Map<UUID, List<WeightTrace>> weights = new ConcurrentHashMap<>();

    @Override
    public void appendFindings(UUID reviewId, List<Finding> records) {
        append(findings, reviewId, records);
    }

    @Override
    public void appendFfmcap(UUID reviewId, List<FfmcapTrace> records) {
        append(ffmcap, reviewId, records);
    }

    @Override
    public void appendSelection(UUID reviewId, List<SelectionTrace> records) {
        append(selection, reviewId, records);
    }

    @Override
    public void appendWeights(UUID reviewId, List<WeightTrace> records) {
        append(weights, reviewId, records);
    }

    @Override
    public ReviewTrace findByReviewId(UUID reviewId) {
        return ReviewTrace.of(
            findings.getOrDefault(reviewId, List.of()),
            ffmcap.getOrDefault(reviewId, List.of()),
            selection.getOrDefault(reviewId, List.of()),
            weights.getOrDefault(reviewId, List.of()));
    }

    private static <T> void append(Map<UUID, List<T>> store, UUID reviewId, List<T> records) {
        store.merge(reviewId, List.copyOf(records),
            (stored, added) -> Stream.concat(stored.stream(), added.stream()).toList());
    }
}
