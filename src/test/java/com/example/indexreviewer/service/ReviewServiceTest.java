package com.example.indexreviewer.service;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewResult;
import com.example.indexreviewer.service.index.IndexReview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);
    private static final ReviewCommand SMI_COMMAND = new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE,
        Map.of(), List.of(), List.of());

    @Mock
    private IndexReview smiReview;

    @Test
    void runReview_whenIndexConfigured_expectItsResult() {
        ReviewResult expected = new ReviewResult(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE);
        when(smiReview.indexType()).thenReturn(IndexType.SMI);
        when(smiReview.review(SMI_COMMAND)).thenReturn(expected);
        ReviewService service = new ReviewService(List.of(smiReview));

        ReviewResult result = service.runReview(SMI_COMMAND);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void runReview_whenIndexNotConfigured_expectError() {
        ReviewService service = new ReviewService(List.of());

        assertThatThrownBy(() -> service.runReview(SMI_COMMAND))
            .isInstanceOfSatisfying(IndexNotConfiguredException.class,
                exception -> assertThat(exception.getIndexType()).isEqualTo(IndexType.SMI));
    }
}
