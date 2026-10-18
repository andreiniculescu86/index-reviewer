package com.example.indexreviewer.repository;

import com.example.indexreviewer.model.audit.ReviewAudit;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores the audit records of review executions. A stored audit record is never changed or deleted.
 */
public interface ReviewAuditRepository {

    void save(ReviewAudit audit);

    Optional<ReviewAudit> findById(UUID reviewId);
}
