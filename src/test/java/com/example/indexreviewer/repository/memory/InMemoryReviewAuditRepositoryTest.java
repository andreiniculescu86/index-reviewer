package com.example.indexreviewer.repository.memory;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.audit.ReviewAudit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.APP_VERSION;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CUTOFF_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.SMI_RULES;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.audit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryReviewAuditRepositoryTest {

    private final InMemoryReviewAuditRepository repository = new InMemoryReviewAuditRepository();

    @Test
    void findById_whenSaved_expectAudit() {
        ReviewAudit audit = audit(ReviewStatus.SUCCESS);
        repository.save(audit);

        Optional<ReviewAudit> found = repository.findById(REVIEW_ID);

        assertThat(found).containsSame(audit);
    }

    @Test
    void findById_whenUnknown_expectEmpty() {
        Optional<ReviewAudit> found = repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(found).isEmpty();
    }

    @Test
    void save_whenIdStoredTwice_expectFirstKept() {
        ReviewAudit first = audit(ReviewStatus.SUCCESS);
        ReviewAudit second = audit(ReviewStatus.ERROR);
        repository.save(first);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(IllegalStateException.class);
        assertThat(repository.findById(REVIEW_ID)).containsSame(first);
    }

    @Test
    void findAll_whenNoFilter_expectAllNewestFirst() {
        ReviewAudit older = auditAt("2026-10-17T08:00:00Z", CUTOFF_DATE);
        ReviewAudit newer = auditAt("2026-10-17T09:00:00Z", CUTOFF_DATE);
        repository.save(older);
        repository.save(newer);

        List<ReviewAudit> found = repository.findAll(null, null, null);

        assertThat(found).containsExactly(newer, older);
    }

    @Test
    void findAll_whenFiltered_expectOnlyMatching() {
        ReviewAudit q3 = auditAt("2026-10-17T08:00:00Z", CUTOFF_DATE);
        ReviewAudit q2 = auditAt("2026-10-17T09:00:00Z", LocalDate.of(2026, 6, 10));
        repository.save(q3);
        repository.save(q2);

        List<ReviewAudit> found = repository.findAll(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE);

        assertThat(found).containsExactly(q3);
    }

    @Test
    void findAll_whenNothingMatches_expectEmpty() {
        repository.save(audit(ReviewStatus.SUCCESS));

        List<ReviewAudit> found = repository.findAll(null, null, LocalDate.of(2026, 12, 21));

        assertThat(found).isEmpty();
    }

    private static ReviewAudit auditAt(String executedAt, LocalDate cutoffDate) {
        return ReviewAudit.builder()
            .reviewId(UUID.randomUUID())
            .indexType(IndexType.SMI)
            .cutoffDate(cutoffDate)
            .reviewDate(REVIEW_DATE)
            .status(ReviewStatus.SUCCESS)
            .executedAt(Instant.parse(executedAt))
            .appVersion(APP_VERSION)
            .rules(SMI_RULES)
            .composition(List.of())
            .build();
    }
}
