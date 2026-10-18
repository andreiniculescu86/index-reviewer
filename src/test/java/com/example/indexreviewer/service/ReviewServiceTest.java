package com.example.indexreviewer.service;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.exception.NotFoundException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.repository.ReviewAuditRepository;
import com.example.indexreviewer.repository.ReviewTraceRepository;
import com.example.indexreviewer.service.index.IndexReviewer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CAPPED_COMPONENT;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CUTOFF_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.ELIGIBLE_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.audit;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.trace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final ReviewCommand SMI_COMMAND = ReviewCommand.builder()
        .indexType(IndexType.SMI)
        .cutoffDate(CUTOFF_DATE)
        .reviewDate(REVIEW_DATE)
        .universe(Map.of())
        .securities(List.of())
        .composition(List.of())
        .build();

    @Mock
    private IndexReviewer smiReview;

    @Mock
    private ReviewAuditRepository auditRepository;

    @Mock
    private ReviewTraceRepository traceRepository;

    @Test
    void runReview_whenIndexConfigured_expectReviewReport() {
        ReviewReport reviewed = ReviewReport.failed(REVIEW_ID);
        when(smiReview.indexType()).thenReturn(IndexType.SMI);
        when(smiReview.review(SMI_COMMAND)).thenReturn(reviewed);
        ReviewService service = new ReviewService(List.of(smiReview), auditRepository, traceRepository);

        ReviewReport report = service.runReview(SMI_COMMAND);

        assertThat(report).isSameAs(reviewed);
        verify(smiReview).review(SMI_COMMAND);
        verifyNoInteractions(auditRepository, traceRepository);
    }

    @Test
    void runReview_whenReviewThrows_expectException() {
        when(smiReview.indexType()).thenReturn(IndexType.SMI);
        when(smiReview.review(SMI_COMMAND)).thenThrow(new IllegalStateException("step failed"));
        ReviewService service = new ReviewService(List.of(smiReview), auditRepository, traceRepository);

        assertThatThrownBy(() -> service.runReview(SMI_COMMAND)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(auditRepository, traceRepository);
    }

    @Test
    void runReview_whenIndexNotConfigured_expectError() {
        ReviewService service = new ReviewService(List.of(), auditRepository, traceRepository);

        assertThatThrownBy(() -> service.runReview(SMI_COMMAND))
            .isInstanceOfSatisfying(IndexNotConfiguredException.class,
                exception -> assertThat(exception.getIndexType()).isEqualTo(IndexType.SMI));
        verifyNoInteractions(auditRepository, traceRepository);
    }

    @Test
    void getReport_whenStored_expectDerivedReport() {
        when(auditRepository.findById(REVIEW_ID)).thenReturn(Optional.of(audit(ReviewStatus.SUCCESS)));
        when(traceRepository.findByReviewId(REVIEW_ID)).thenReturn(trace(List.of(CAPPED_COMPONENT), List.of()));
        ReviewService service = new ReviewService(List.of(), auditRepository, traceRepository);

        ReviewReport report = service.getReport(REVIEW_ID);

        assertThat(report.getReviewId()).isEqualTo(REVIEW_ID);
        assertThat(report.getStatus()).isEqualTo(ReviewStatus.SUCCESS);
        assertThat(report.getConstituents()).extracting(Constituent::getSecurityId).containsExactly(155);
    }

    @Test
    void getAudit_whenStored_expectAudit() {
        ReviewAudit audit = audit(ReviewStatus.SUCCESS);
        when(auditRepository.findById(REVIEW_ID)).thenReturn(Optional.of(audit));
        ReviewService service = new ReviewService(List.of(), auditRepository, traceRepository);

        ReviewAudit found = service.getAudit(REVIEW_ID);

        assertThat(found).isSameAs(audit);
        verifyNoInteractions(traceRepository);
    }

    @Test
    void getTrace_whenStored_expectTrace() {
        ReviewTrace trace = trace(List.of(ELIGIBLE_SECURITY), List.of());
        when(traceRepository.findByReviewId(REVIEW_ID)).thenReturn(trace);
        ReviewService service = new ReviewService(List.of(), auditRepository, traceRepository);

        ReviewTrace found = service.getTrace(REVIEW_ID);

        assertThat(found).isSameAs(trace);
    }

    @Test
    void getAudit_whenUnknown_expectNotFound() {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(auditRepository.findById(unknownId)).thenReturn(Optional.empty());
        ReviewService service = new ReviewService(List.of(), auditRepository, traceRepository);

        assertThatThrownBy(() -> service.getAudit(unknownId))
            .isInstanceOfSatisfying(NotFoundException.class, exception -> {
                assertThat(exception.getField()).isEqualTo("reviewId");
                assertThat(exception.getValue()).isEqualTo(unknownId.toString());
            });
        verifyNoInteractions(traceRepository);
    }
}
