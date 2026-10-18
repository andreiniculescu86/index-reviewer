package com.example.indexreviewer.repository.memory;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.repository.ReviewAuditRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps audit records in memory. The records are lost when the application stops.
 */
@Repository
public class InMemoryReviewAuditRepository implements ReviewAuditRepository {

    private final Map<UUID, ReviewAudit> audits = new ConcurrentHashMap<>();

    @Override
    public void save(ReviewAudit audit) {
        if (audits.putIfAbsent(audit.getReviewId(), audit) != null) {
            throw new IllegalStateException(
                "Audit record of review %s is already stored".formatted(audit.getReviewId()));
        }
    }

    @Override
    public Optional<ReviewAudit> findById(UUID reviewId) {
        return Optional.ofNullable(audits.get(reviewId));
    }

    @Override
    public List<ReviewAudit> findAll(IndexType indexType, LocalDate cutoffDate, LocalDate reviewDate) {
        return audits.values().stream()
            .filter(audit -> matches(indexType, audit.getIndexType()))
            .filter(audit -> matches(cutoffDate, audit.getCutoffDate()))
            .filter(audit -> matches(reviewDate, audit.getReviewDate()))
            .sorted(Comparator.comparing(ReviewAudit::getExecutedAt).reversed())
            .toList();
    }

    private static boolean matches(Object filter, Object value) {
        return filter == null || Objects.equals(filter, value);
    }
}
