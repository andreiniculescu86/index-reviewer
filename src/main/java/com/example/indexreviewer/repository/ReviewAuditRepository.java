package com.example.indexreviewer.repository;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.audit.ReviewAudit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores the audit records of review executions. A stored audit record is never changed or deleted.
 */
public interface ReviewAuditRepository {

    void save(ReviewAudit audit);

    Optional<ReviewAudit> findById(UUID reviewId);

    /**
     * Returns the audit records matching all given filters, newest execution first. A null filter matches every record.
     */
    List<ReviewAudit> findAll(IndexType indexType, LocalDate cutoffDate, LocalDate reviewDate);
}
