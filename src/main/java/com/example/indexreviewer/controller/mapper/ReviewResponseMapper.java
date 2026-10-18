package com.example.indexreviewer.controller.mapper;

import com.example.indexreviewer.config.MapStructConfig;
import com.example.indexreviewer.dto.response.review.FindingResponse;
import com.example.indexreviewer.dto.response.review.ReviewAuditResponse;
import com.example.indexreviewer.dto.response.review.ReviewResponse;
import com.example.indexreviewer.dto.response.review.ReviewTraceResponse;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.ReviewTrace;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapStructConfig.class)
public interface ReviewResponseMapper {

    ReviewResponse toReviewResponse(ReviewReport report);

    List<ReviewResponse> toReviewResponses(List<ReviewReport> reports);

    ReviewAuditResponse toAuditResponse(ReviewAudit audit);

    /**
     * Maps the identity and status of the audit record with the given trace, so that the trace of one security can be
     * returned as well.
     */
    @Mapping(target = "securities", source = "trace.securities")
    @Mapping(target = "findings", source = "trace.findings")
    ReviewTraceResponse toTraceResponse(ReviewAudit audit, ReviewTrace trace);

    FindingResponse toFindingResponse(Finding finding);
}
