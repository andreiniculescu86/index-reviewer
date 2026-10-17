package com.example.indexreviewer.controller;

import com.example.indexreviewer.config.ConfigRequestPartConverter;
import com.example.indexreviewer.controller.mapper.ReviewCommandMapperImpl;
import com.example.indexreviewer.exception.GlobalExceptionHandler;
import com.example.indexreviewer.repository.csv.CsvFileReaderFactory;
import com.example.indexreviewer.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_DATE_RANGE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_FILE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.INVALID_VALUE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MALFORMED_JSON;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MISSING_FILE;
import static com.example.indexreviewer.dto.response.error.ErrorCode.MISSING_VALUE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private static final String REVIEWS_PATH = "/v1/reviews";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();
        ReviewController controller = new ReviewController(new CsvFileReaderFactory(),
            new ReviewCommandMapperImpl(), mock(ReviewService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(jsonConverter, new ConfigRequestPartConverter(jsonConverter.getMapper()))
            .build();
    }

    @Test
    void upload_whenAllPartsValid_expectCreated() throws Exception {
        uploadWithConfig(configRequest("""
            {"indexType": "SMI", "cutoffDate": "2026-09-10", "reviewDate": "2026-09-21"}
            """))
            .andExpect(status().isCreated());
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
    void list_whenFiltersValid_expectOk() throws Exception {
        mockMvc.perform(get(REVIEWS_PATH)
                .param("indexType", "SMI")
                .param("cutoffDate", "2026-09-10")
                .param("reviewDate", "2026-09-21"))
            .andExpect(status().isOk());
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
