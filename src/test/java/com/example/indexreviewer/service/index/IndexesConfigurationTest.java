package com.example.indexreviewer.service.index;

import com.example.indexreviewer.exception.IndexNotConfiguredException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.audit.IndexRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexesConfigurationTest {

    @Test
    void findRules_whenIndexConfigured_expectItsRules() {
        IndexesConfiguration configuration = new IndexesConfiguration(Map.of(IndexType.SMI,
            new IndexConfiguration(20, 18, 22, new BigDecimal("18"), "rulebook-v3.40")));

        IndexRules rules = configuration.findRules(IndexType.SMI);

        assertThat(rules.getNumberOfComponents()).isEqualTo(20);
        assertThat(rules.getDirectSelectionRank()).isEqualTo(18);
        assertThat(rules.getBufferRank()).isEqualTo(22);
        assertThat(rules.getCapPercentage()).isEqualByComparingTo("18");
        assertThat(rules.getMethodologyVersion()).isEqualTo("rulebook-v3.40");
    }

    @Test
    void findRules_whenIndexNotConfigured_expectError() {
        IndexesConfiguration configuration = new IndexesConfiguration(Map.of());

        assertThatThrownBy(() -> configuration.findRules(IndexType.SMI))
            .isInstanceOfSatisfying(IndexNotConfiguredException.class,
                exception -> assertThat(exception.getIndexType()).isEqualTo(IndexType.SMI));
    }
}
