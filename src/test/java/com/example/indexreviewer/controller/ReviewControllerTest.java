package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.ConfigRequestPartConverter;
import com.example.indexreviewer.controller.mapper.ReviewCommandMapperImpl;
import com.example.indexreviewer.controller.mapper.ReviewResponseMapperImpl;
import com.example.indexreviewer.exception.GlobalExceptionHandler;
import com.example.indexreviewer.exception.NotFoundException;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.repository.csv.CsvFileReaderFactory;
import com.example.indexreviewer.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_DATE_RANGE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_FILE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_VALUE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MALFORMED_JSON;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MISSING_FILE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MISSING_VALUE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.NOT_FOUND;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CAPPED_COMPONENT;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.COMPOSITION_SIZE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.CUTOFF_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.ELIGIBLE_SECURITY;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.JOINER;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.LEAVER;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.MISSING_REVIEW_DATA;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_DATE;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.REVIEW_ID;
import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.report;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private static final String REVIEWS_PATH = "/v1/reviews";

    private final ReviewService reviewService = mock(ReviewService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();
        ReviewController controller = new ReviewController(new CsvFileReaderFactory(),
            new ReviewCommandMapperImpl(), new ReviewResponseMapperImpl(), reviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(jsonConverter, new ConfigRequestPartConverter(jsonConverter.getMapper()))
            .build();
        when(reviewService.runReview(any())).thenReturn(report(List.of(ELIGIBLE_SECURITY), List.of()));
    }

    @Test
    void upload_whenAllPartsValid_expectCreated() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": "SMI", "cutoffDate": "2026-09-10", "reviewDate": "2026-09-21"}
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()));
    }

    @Test
    void upload_whenReviewFails_expectUnprocessable() throws Exception {
        when(reviewService.runReview(any())).thenReturn(report(List.of(), List.of(COMPOSITION_SIZE)));

        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(universe())
                .file(securityData())
                .file(composition()))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.constituents", hasSize(0)))
            .andExpect(jsonPath("$.joiners", hasSize(0)))
            .andExpect(jsonPath("$.leavers", hasSize(0)))
            .andExpect(jsonPath("$.findings", hasSize(1)))
            .andExpect(jsonPath("$.findings[0].code").value("COMPOSITION_SIZE"))
            .andExpect(jsonPath("$.findings[0].field").value("composition"));
    }

    @Test
    void upload_whenConfigOmitted_expectDefaults() throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(universe())
                .file(securityData())
                .file(composition()))
            .andExpect(status().isCreated());
    }

    @Test
    void upload_whenConfigUntyped_expectValidated() throws Exception {
        MockMultipartFile untypedConfigRequest = new MockMultipartFile("configRequest", "", null,
            """
                {"cutoffDate": "2026-09-22", "reviewDate": "2026-09-21"}
                """.getBytes(StandardCharsets.UTF_8));

        uploadWithConfig(untypedConfigRequest)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_DATE_RANGE.name()));
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidCompositionFiles")
    void upload_whenFileInvalid_expectFileError(MockMultipartFile invalidComposition) throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(universe())
                .file(securityData())
                .file(invalidComposition))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_FILE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("composition"));
    }

    static Stream<Arguments> invalidCompositionFiles() {
        return Stream.of(
            Arguments.argumentSet("file extension is not .csv",
                csvPart("composition", "composition.txt", "id\n155\n")),
            Arguments.argumentSet("file is empty",
                csvPart("composition", "composition.csv", "")),
            Arguments.argumentSet("file has no extension",
                csvPart("composition", "composition", "id\n155\n")));
    }

    @Test
    void upload_whenFilesInvalid_expectAllErrors() throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(csvPart("universe", "spi_universe.txt", "date;id\n"))
                .file(csvPart("securityData", "sec_data.csv", ""))
                .file(composition()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("universe", "securityData")));

        verify(reviewService, never()).runReview(any());
    }

    @Test
    void upload_whenPartMissing_expectMissingFile() throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(universe())
                .file(securityData()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(MISSING_FILE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("composition"));
    }

    @Test
    void upload_whenInvalid_expectRequestInfo() throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .file(universe())
                .file(securityData()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.instance").value(REVIEWS_PATH));
    }

    @Test
    void upload_whenConfigValuesNull_expectAllErrors() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": null, "cutoffDate": null}
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                "configRequest.indexType", "configRequest.cutoffDate")));
    }

    @Test
    void upload_whenFilesSentAsText_expectMissingFile() throws Exception {
        mockMvc.perform(multipart(REVIEWS_PATH)
                .param("universe", "string")
                .param("securityData", "string")
                .param("composition", "string"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(MISSING_FILE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("universe"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\\"", "\\\\", "\\u0000", "\\n"})
    void upload_whenSeparatorUnsupported_expectInvalid(String separator) throws Exception {
        uploadWithConfig(configRequest("""
            {"csvSeparator": "%s"}
            """.formatted(separator)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("configRequest.csvSeparator"));

        verify(reviewService, never()).runReview(any());
    }

    @Test
    void upload_whenCutoffAfterReview_expectDateError() throws Exception {
        uploadWithConfig(configRequest("""
            {"cutoffDate": "2026-09-22", "reviewDate": "2026-09-21"}
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_DATE_RANGE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("configRequest"));
    }

    @Test
    void upload_whenIndexTypeNull_expectMissingValue() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": null}
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(MISSING_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("configRequest.indexType"));
    }

    @Test
    void upload_whenIndexTypeUnknown_expectInvalidValue() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": "XYZ"}
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("configRequest.indexType"))
            .andExpect(jsonPath("$.errors[0].value").value("XYZ"));
    }

    @Test
    void upload_whenDateInvalid_expectInvalidValue() throws Exception {
        uploadWithConfig(configRequest("""
            {"cutoffDate": "2026-13-01"}
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("configRequest.cutoffDate"));
    }

    @Test
    void upload_whenJsonMalformed_expectMalformedJson() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": "SMI",
            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(MALFORMED_JSON.name()));
    }

    @Test
    void list_whenIndexTypeUnknown_expectInvalidValue() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param("indexType", "XYZ"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("indexType"));
    }

    @Test
    void list_whenDateNotIso_expectInvalidValue() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH).param("cutoffDate", "10.09.2026"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("cutoffDate"));
    }

    @Test
    void list_whenFiltersValid_expectMatchingReviews() throws Exception {
        when(reviewService.listReports(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE))
            .thenReturn(List.of(report(List.of(CAPPED_COMPONENT, JOINER, LEAVER), List.of(MISSING_REVIEW_DATA))));

        mockMvc.perform(get(REVIEWS_PATH)
                .param("indexType", "SMI")
                .param("cutoffDate", "2026-09-10")
                .param("reviewDate", "2026-09-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$[0].status").value("SUCCESS_WITH_WARNINGS"))
            .andExpect(jsonPath("$[0].constituents[*].securityId", contains(155, 177)))
            .andExpect(jsonPath("$[0].joiners", contains(177)))
            .andExpect(jsonPath("$[0].leavers", contains(103)))
            .andExpect(jsonPath("$[0].findings[0].code").value("MISSING_REVIEW_DATA"))
            .andExpect(jsonPath("$[0].findings[0].securityId").value(166));
    }

    @Test
    void list_whenNoReviews_expectEmptyList() throws Exception {
        when(reviewService.listReports(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(REVIEWS_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getReview_whenStored_expectReport() throws Exception {
        when(reviewService.getReport(REVIEW_ID))
            .thenReturn(report(List.of(CAPPED_COMPONENT, JOINER, LEAVER), List.of()));

        mockMvc.perform(get(REVIEWS_PATH + "/" + REVIEW_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(REVIEW_ID.toString()))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.constituents[*].securityId", contains(155, 177)))
            .andExpect(jsonPath("$.constituents[0].weight").value(18))
            .andExpect(jsonPath("$.constituents[0].cappingFactor").value(0.565353))
            .andExpect(jsonPath("$.joiners", contains(177)))
            .andExpect(jsonPath("$.leavers", contains(103)));
    }

    @Test
    void getReview_whenUnknown_expectNotFound() throws Exception {
        when(reviewService.getReport(REVIEW_ID)).thenThrow(
            new NotFoundException("reviewId", REVIEW_ID.toString(), "Review " + REVIEW_ID + " does not exist"));

        mockMvc.perform(get(REVIEWS_PATH + "/" + REVIEW_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0].code").value(NOT_FOUND.name()))
            .andExpect(jsonPath("$.errors[0].field").value("reviewId"))
            .andExpect(jsonPath("$.errors[0].value").value(REVIEW_ID.toString()));
    }

    @Test
    void getReview_whenIdNotUuid_expectInvalidValue() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH + "/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].code").value(INVALID_VALUE.name()))
            .andExpect(jsonPath("$.errors[0].field").value("reviewId"));
    }

    private ResultActions uploadWithConfig(MockMultipartFile configRequest) throws Exception {
        return mockMvc.perform(multipart(REVIEWS_PATH)
            .file(configRequest)
            .file(universe())
            .file(securityData())
            .file(composition()));
    }

    private static MockMultipartFile configRequest(String json) {
        return new MockMultipartFile("configRequest", "", MediaType.APPLICATION_JSON_VALUE,
            json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile universe() {
        return csvPart("universe", "spi_universe.csv", "date;id\n2026-09-21;10\n");
    }

    private static MockMultipartFile securityData() {
        return csvPart("securityData", "sec_data.csv",
            "id;date;price;free_float;shares\n10;2026-09-10;2080;0.6766;309375\n");
    }

    private static MockMultipartFile composition() {
        return csvPart("composition", "composition.csv", "id\n155\n");
    }

    private static MockMultipartFile csvPart(String name, String fileName, String content) {
        return new MockMultipartFile(name, fileName, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
