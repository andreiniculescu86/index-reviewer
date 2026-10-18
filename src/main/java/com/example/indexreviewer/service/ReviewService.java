package com.example.indexreviewer.service;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.exception.NotFoundException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.repository.ReviewAuditRepository;
import com.example.indexreviewer.repository.ReviewTraceRepository;
import com.example.indexreviewer.service.index.IndexReviewer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReviewService {

    private final Map<IndexType, IndexReviewer> reviews;
    private final ReviewAuditRepository auditRepository;
    private final ReviewTraceRepository traceRepository;

    public ReviewService(List<IndexReviewer> implementations, ReviewAuditRepository auditRepository,
                         ReviewTraceRepository traceRepository) {
        reviews = implementations.stream().collect(Collectors.toMap(IndexReviewer::indexType, Function.identity()));
        this.auditRepository = auditRepository;
        this.traceRepository = traceRepository;
    }

    /**
     * Runs the review of the command's index and returns its report. The index review creates the review id and
     * stores the trace and the audit record, also when the review fails on data errors.
     */
    public ReviewReport runReview(ReviewCommand command) {
        IndexReviewer indexerReviewer = reviews.get(command.getIndexType());
        if (indexerReviewer == null) {
            throw new IndexNotConfiguredException(command.getIndexType());
        }
        ReviewReport report = indexerReviewer.review(command);

        log.info("Finished review {} of {} with status {}, {} constituents, {} joiners, {} leavers",
            report.getReviewId(), command.getIndexType(), report.getStatus(), report.getConstituents().size(),
            report.getJoiners().size(), report.getLeavers().size());
        return report;
    }

    /**
     * Derives the report of a stored review from its audit record and trace.
     */
    public ReviewReport getReport(UUID reviewId) {
        return ReviewReport.of(getAudit(reviewId), getTrace(reviewId));
    }

    /**
     * Returns the audit record of a finished review. A review without an audit record is unknown, also while it is
     * still running.
     */
    public ReviewAudit getAudit(UUID reviewId) {
        return auditRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException("reviewId", reviewId.toString(),
                "Review %s does not exist".formatted(reviewId)));
    }

    /**
     * Returns the audit records of the stored reviews matching all given filters, newest first. A null filter matches
     * every review.
     */
    public List<ReviewAudit> listAudits(IndexType indexType, LocalDate cutoffDate, LocalDate reviewDate) {
        return auditRepository.findAll(indexType, cutoffDate, reviewDate);
    }

    /**
     * Returns the stored trace of a review; it is empty when nothing was stored for the id.
     */
    public ReviewTrace getTrace(UUID reviewId) {
        return traceRepository.findByReviewId(reviewId);
    }
}
