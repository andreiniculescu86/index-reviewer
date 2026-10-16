package com.example.indexreviewer.dto.request;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigRequestTest {

    private static final LocalDate DEFAULT_CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate DEFAULT_REVIEW_DATE = LocalDate.of(2026, 9, 21);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void read_whenJsonEmpty_expectAllDefaults() {
        ConfigRequest request = jsonMapper.readValue("{}", ConfigRequest.class);

        assertThat(request.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(request.getCutoffDate()).isEqualTo(DEFAULT_CUTOFF_DATE);
        assertThat(request.getReviewDate()).isEqualTo(DEFAULT_REVIEW_DATE);
    }

    @Test
    void read_whenOnlyReviewDate_expectOtherDefaults() {
        ConfigRequest request = jsonMapper.readValue("""
            {"reviewDate": "2026-09-25"}
            """, ConfigRequest.class);

        assertThat(request.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(request.getCutoffDate()).isEqualTo(DEFAULT_CUTOFF_DATE);
        assertThat(request.getReviewDate()).isEqualTo(LocalDate.of(2026, 9, 25));
    }

    @Test
    void create_whenConstructorUsed_expectDefaults() {
        ConfigRequest request = new ConfigRequest();

        assertThat(request.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(request.getCutoffDate()).isEqualTo(DEFAULT_CUTOFF_DATE);
        assertThat(request.getReviewDate()).isEqualTo(DEFAULT_REVIEW_DATE);
    }

    @Test
    void create_whenBuilderUsed_expectDefaults() {
        ConfigRequest request = ConfigRequest.builder().build();

        assertThat(request.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(request.getCutoffDate()).isEqualTo(DEFAULT_CUTOFF_DATE);
        assertThat(request.getReviewDate()).isEqualTo(DEFAULT_REVIEW_DATE);
    }
}
