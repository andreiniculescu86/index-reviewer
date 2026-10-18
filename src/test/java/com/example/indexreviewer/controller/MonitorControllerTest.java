package com.example.indexreviewer.controller;

import com.example.indexreviewer.controller.mapper.ReviewResponseMapperImpl;
import com.example.indexreviewer.exception.GlobalExceptionHandler;
import com.example.indexreviewer.exception.NotFoundException;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.example.indexreviewer.dto.response.error.ErrorCode.NOT_FOUND;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.APP_VERSION;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.COMPOSITION_SIZE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.ELIGIBLE_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.EXECUTED_AT;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.EXCLUDED_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.MISSING_REVIEW_DATA;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.audit;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.trace;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonitorControllerTest {

    private static final String REVIEW_PATH = "/v1/reviews/" + REVIEW_ID;

    private final ReviewService reviewService = mock(ReviewService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MonitorController controller = new MonitorController(reviewService, new ReviewResponseMapperImpl());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new JacksonJsonHttpMessageConverter())
            .build();
        when(reviewService.getAudit(REVIEW_ID)).thenReturn(audit(ReviewStatus.FAILED));
        when(reviewService.getTrace(REVIEW_ID)).thenReturn(trace(List.of(ELIGIBLE_SECURITY, EXCLUDED_SECURITY),
            List.of(COMPOSITION_SIZE, MISSING_REVIEW_DATA)));
    }

    @Test
    void audit_whenStored_expectAuditData() throws Exception {
        mockMvc.perform(get(REVIEW_PATH + "/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.executedAt").value(EXECUTED_AT.toString()))
            .andExpect(jsonPath("$.appVersion").value(APP_VERSION))
            .andExpect(jsonPath("$.rules.numberOfComponents").value(20))
            .andExpect(jsonPath("$.rules.bufferRank").value(22))
            .andExpect(jsonPath("$.composition", hasSize(0)))
            .andExpect(jsonPath("$.securities").doesNotExist())
            .andExpect(jsonPath("$.findings").doesNotExist());
    }

    @Test
    void audit_whenCompleted_expectComposition() throws Exception {
        when(reviewService.getAudit(REVIEW_ID)).thenReturn(audit(ReviewStatus.SUCCESS, List.of(155, 177)));

        mockMvc.perform(get(REVIEW_PATH + "/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.composition", contains(155, 177)));
    }

    @Test
    void trace_whenStored_expectAllTraces() throws Exception {
        mockMvc.perform(get(REVIEW_PATH + "/trace"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.appVersion").doesNotExist())
            .andExpect(jsonPath("$.securities[*].securityId", contains(10, 166)))
            .andExpect(jsonPath("$.securities[1].decision").value("NOT_ELIGIBLE"))
            .andExpect(jsonPath("$.findings[*].code", contains("COMPOSITION_SIZE", "MISSING_REVIEW_DATA")));
    }

    @Test
    void security_whenInReview_expectOnlyItsTrace() throws Exception {
        mockMvc.perform(get(REVIEW_PATH + "/securities/166"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.securities", hasSize(1)))
            .andExpect(jsonPath("$.securities[0].securityId").value(166))
            .andExpect(jsonPath("$.securities[0].price").value(0.13))
            .andExpect(jsonPath("$.findings[*].code", contains("MISSING_REVIEW_DATA")));
    }

    @Test
    void security_whenOnlyFindings_expectEmptyTrace() throws Exception {
        Finding duplicate = Finding.builder()
            .code(FindingCode.DUPLICATE_ROW)
            .securityId(999)
            .field("date")
            .message("Security 999 has duplicate security data rows for 2026-09-10, counted once")
            .build();
        when(reviewService.getTrace(REVIEW_ID)).thenReturn(trace(List.of(ELIGIBLE_SECURITY), List.of(duplicate)));

        mockMvc.perform(get(REVIEW_PATH + "/securities/999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.securities", hasSize(0)))
            .andExpect(jsonPath("$.findings[*].code", contains("DUPLICATE_ROW")));
    }

    @Test
    void security_whenNotInReview_expectNotFound() throws Exception {
        mockMvc.perform(get(REVIEW_PATH + "/securities/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0].code").value(NOT_FOUND.name()))
            .andExpect(jsonPath("$.errors[0].field").value("securityId"))
            .andExpect(jsonPath("$.errors[0].value").value("999"));
    }

    @Test
    void trace_whenReviewUnknown_expectNotFound() throws Exception {
        when(reviewService.getAudit(REVIEW_ID)).thenThrow(
            new NotFoundException("reviewId", REVIEW_ID.toString(), "Review " + REVIEW_ID + " does not exist"));

        mockMvc.perform(get(REVIEW_PATH + "/trace"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0].field").value("reviewId"));
    }
}
