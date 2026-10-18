package com.example.indexreviewer.repository.memory;

import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.audit.ReviewAudit;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
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
}
