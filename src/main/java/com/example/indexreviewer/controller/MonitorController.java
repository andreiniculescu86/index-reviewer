package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.swagger.MonitorSwaggerDocs;
import com.example.indexreviewer.controller.mapper.ReviewResponseMapper;
import com.example.indexreviewer.dto.response.review.ReviewAuditResponse;
import com.example.indexreviewer.dto.response.review.ReviewTraceResponse;
import com.example.indexreviewer.exception.NotFoundException;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1")
@MonitorSwaggerDocs.Api
public class MonitorController {

    private final ReviewService reviewService;
    private final ReviewResponseMapper responseMapper;

    @GetMapping(path = "/reviews/{reviewId}/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    @MonitorSwaggerDocs.GetReviewAudit
    public ReviewAuditResponse getReviewAudit(@PathVariable UUID reviewId) {
        return responseMapper.toAuditResponse(reviewService.getAudit(reviewId));
    }

    @GetMapping(path = "/reviews/{reviewId}/trace", produces = MediaType.APPLICATION_JSON_VALUE)
    @MonitorSwaggerDocs.GetReviewTrace
    public ReviewTraceResponse getReviewTrace(@PathVariable UUID reviewId) {
        ReviewAudit audit = reviewService.getAudit(reviewId);
        return responseMapper.toTraceResponse(audit, reviewService.getTrace(reviewId));
    }

    @GetMapping(path = "/reviews/{reviewId}/securities/{securityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @MonitorSwaggerDocs.GetReviewSecurity
    public ReviewTraceResponse getReviewSecurity(@PathVariable UUID reviewId, @PathVariable Integer securityId) {
        ReviewAudit audit = reviewService.getAudit(reviewId);
        ReviewTrace securityTrace = reviewService.getTrace(reviewId).forSecurity(securityId);
        if (securityTrace.isEmpty()) {
            throw new NotFoundException("securityId", String.valueOf(securityId),
                "Security %s is not part of review %s".formatted(securityId, reviewId));
        }
        return responseMapper.toTraceResponse(audit, securityTrace);
    }

//    @GetMapping(path = "/reviews/{reviewId}/inputs/{role}", produces = TEXT_CSV)
//    @MonitorSwaggerDocs.GetReviewInput
//    public void getReviewInput(@PathVariable String reviewId, @PathVariable String role) {
//    }
}
