package com.example.indexreviewer.e2e;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ReviewEndToEndIT {

    private static final BigDecimal CAP = new BigDecimal("18");
    private static final BigDecimal FULL_INDEX = new BigDecimal("100");
    private static final JsonMapper JSON = JsonMapper.builder()
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .build();

    @LocalServerPort
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void review_whenAssignmentData_expectQ3Result() {
        HttpResult result = postReview("scenario1", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertValidIndex(report);
        assertThat(ids(report.path("constituents"))).containsExactly(155, 205, 63, 64, 65, 54, 177, 176, 70, 87,
            280, 309, 317, 270, 95, 43, 311, 253, 160, 81);
        assertThat(ids(report.path("joiners"))).containsExactly(177);
        assertThat(ids(report.path("leavers"))).containsExactly(103);
        assertConstituent(report, 155, "18.0000", "0.5653526015");
        assertConstituent(report, 205, "18.0000", "0.6117362279");
        assertConstituent(report, 81, "0.8503", "1");

        String reviewId = report.path("reviewId").asString();
        assertThat(get("/v1/reviews/" + reviewId).body()).isEqualTo(report);
        assertThat(ids(get("/v1/reviews/" + reviewId + "/audit").body().path("composition")))
            .containsExactlyElementsOf(ids(report.path("constituents")));
        assertThat(get("/v1/reviews?indexType=SMI&cutoffDate=2026-09-10").body().findValues("reviewId"))
            .extracting(JsonNode::asString).contains(reviewId);

        JsonNode trace = get("/v1/reviews/" + reviewId + "/trace").body();
        assertThat(findings(trace)).containsExactly(tuple("MISSING_REVIEW_DATA", 166));
        assertThat(security(trace, 101).path("freeFloat").decimalValue()).isEqualByComparingTo("0.8476");
        assertThat(security(trace, 101).path("ffmcap").decimalValue())
            .isEqualByComparingTo(new BigDecimal("311.5").multiply(new BigDecimal("5835000"))
                .multiply(new BigDecimal("0.8476")));
        assertDecision(trace, 249, 19, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 160, 21, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 81, 22, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertThat(security(trace, 166).path("decision").asString()).isEqualTo("NOT_ELIGIBLE");
    }

    @Test
    void review_whenFourComponentsInBuffer_expectTwoKept() {
        HttpResult result = postReview("scenario2", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(117, 118);
        assertThat(ids(report.path("leavers"))).containsExactly(121, 122);
        JsonNode trace = traceOf(report);
        assertDecision(trace, 118, 18, "SELECTED_DIRECT");
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 120, 20, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 121, 21, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 122, 22, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 123, 23, "NOT_SELECTED_BELOW_BUFFER");
    }

    @Test
    void review_whenConcentrated_expectCappedInRounds() {
        HttpResult result = postReview("scenario3", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertValidIndex(report);
        assertConstituent(report, 201, "18.0000", "0.6974560976");
        assertConstituent(report, 202, "18.0000", "0.9693457626");
        assertConstituent(report, 203, "14.4777", "1");
        assertConstituent(report, 219, "0.3497", "1");
        assertConstituent(report, 220, "0.2098", "1");
        assertThat(report.path("constituents").valueStream()
            .filter(constituent -> constituent.path("cappingFactor").decimalValue().compareTo(BigDecimal.ONE) < 0)
            .map(constituent -> constituent.path("securityId").asInt())
            .toList()).containsExactly(201, 202);
    }

    @Test
    void review_whenCorporateActions_expectLeaversJoiners() {
        HttpResult result = postReview("scenario4", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertValidIndex(report);
        assertThat(ids(report.path("constituents"))).doesNotContain(999, 103, 106);
        assertThat(ids(report.path("joiners"))).containsExactly(121, 122);
        assertThat(ids(report.path("leavers"))).containsExactly(103, 106);
        JsonNode trace = traceOf(report);
        assertThat(findings(trace)).contains(
            tuple("EXISTING_COMPONENT_NOT_IN_UNIVERSE", 103),
            tuple("MISSING_REVIEW_DATA", 106),
            tuple("MISSING_CUTOFF_PRICE", 999));
        assertThat(security(trace, 999).path("decision").asString()).isEqualTo("NOT_ELIGIBLE");
    }

    @Test
    void review_whenFreeFloatInPercent_expectFailed() {
        HttpResult result = postReview("scenario5", null);

        assertFailed(result, "INVALID_VALUE", 108);
        assertThat(security(traceOf(result.body()), 108).path("freeFloat").decimalValue())
            .isEqualByComparingTo("85");
    }

    @Test
    void review_whenRowsLoadedTwice_expectFailed() {
        HttpResult result = postReview("scenario6", null);

        assertFailed(result, "CONFLICTING_DUPLICATE", 104);
        assertThat(findings(traceOf(result.body()))).contains(tuple("DUPLICATE_ROW", 110));
    }

    @Test
    void review_whenDatesNotInFiles_expectFailed() {
        HttpResult result = postReview("scenario7", """
            {"indexType": "SMI", "cutoffDate": "2026-12-10", "reviewDate": "2026-12-21"}
            """);

        assertFailed(result, "MISSING_DATE", null);
        JsonNode audit = get("/v1/reviews/" + result.body().path("reviewId").asString() + "/audit").body();
        assertThat(audit.path("cutoffDate").asString()).isEqualTo("2026-12-10");
        assertThat(audit.path("reviewDate").asString()).isEqualTo("2026-12-21");
    }

    @Test
    void review_whenCommaSeparatedExport_expectParsed() {
        HttpResult defaultSeparator = postReview("scenario8", null);
        HttpResult commaSeparator = postReview("scenario8", """
            {"csvSeparator": ","}
            """);

        assertThat(defaultSeparator.status()).isEqualTo(400);
        assertThat(defaultSeparator.body().path("errors").get(0).path("code").asString()).isEqualTo("INVALID_FILE");
        assertThat(commaSeparator.status()).isEqualTo(201);
        JsonNode report = commaSeparator.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("constituents"))).contains(119, 122).doesNotContain(120, 121);
        assertConstituent(report, 122, "4.3624", "1");
        assertDecision(traceOf(report), 122, 22, "SELECTED_BUFFER_EXISTING_COMPONENT");
    }

    @Test
    void review_whenUniverseTruncated_expectFailed() {
        HttpResult result = postReview("scenario9", null);

        assertFailed(result, "TOO_FEW_ELIGIBLE", null);
        assertThat(findings(traceOf(result.body()))).contains(
            tuple("MISSING_CUTOFF_PRICE", 105),
            tuple("MISSING_CUTOFF_PRICE", 112));
    }

    @Test
    void review_whenCompositionIdRepeated_expectFailed() {
        HttpResult result = postReview("scenario10", null);

        assertFailed(result, "COMPOSITION_SIZE", null);
    }

    @Test
    void review_whenValuesChangeByDate_expectRightDate() {
        HttpResult result = postReview("scenario11", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(123);
        assertThat(ids(report.path("leavers"))).containsExactly(102);
        assertConstituent(report, 123, "12.0125", "1");
        assertConstituent(report, 101, "5.1482", "1");
        assertConstituent(report, 120, "4.1601", "1");
        JsonNode trace = traceOf(report);
        assertThat(security(trace, 123).path("price").decimalValue()).isEqualByComparingTo("962.5");
        assertThat(security(trace, 123).path("shares").decimalValue()).isEqualByComparingTo("3000000");
        assertThat(security(trace, 102).path("freeFloat").decimalValue()).isEqualByComparingTo("0.1");
        assertDecision(trace, 123, 1, "SELECTED_DIRECT");
        assertDecision(trace, 102, 24, "NOT_SELECTED_BELOW_BUFFER");
    }

    @Test
    void review_whenTieWrittenDifferently_expectTieOrder() {
        HttpResult result = postReview("scenario12", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(119);
        assertThat(ids(report.path("leavers"))).containsExactly(123);
        assertConstituent(report, 124, "0.2915", "1");
        JsonNode trace = traceOf(report);
        assertThat(findings(trace)).containsExactly(tuple("SELECTION_TIE", null));
        assertDecision(trace, 124, 22, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 123, 23, "NOT_SELECTED_BELOW_BUFFER");
        assertDecision(trace, 122, 24, "NOT_SELECTED_BELOW_BUFFER");
        assertThat(security(trace, 123).path("ffmcap").decimalValue())
            .isEqualByComparingTo(security(trace, 122).path("ffmcap").decimalValue());
    }

    @Test
    void review_whenFfmcapsNearlyEqual_expectExactRanking() {
        HttpResult result = postReview("scenario13", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(301);
        assertThat(ids(report.path("leavers"))).containsExactly(119);
        assertConstituent(report, 301, "4.5987", "1");
        assertConstituent(report, 302, "4.5987", "1");
        JsonNode trace = traceOf(report);
        assertThat(security(trace, 301).path("ffmcap").decimalValue())
            .isEqualByComparingTo("825000000.27500000825000000275");
        assertThat(security(trace, 302).path("ffmcap").decimalValue()).isEqualByComparingTo("825000000.275");
        assertDecision(trace, 301, 18, "SELECTED_DIRECT");
        assertDecision(trace, 302, 19, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 118, 20, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 119, 21, "NOT_SELECTED_BUFFER");
    }

    @Test
    void review_whenCappingCascades_expectAllCapped() {
        HttpResult result = postReview("scenario14", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertConstituent(report, 401, "18.0000", "0.2965178571");
        assertConstituent(report, 402, "18.0000", "0.7412946429");
        assertConstituent(report, 403, "18.0000", "0.9266183036");
        assertConstituent(report, 404, "18.0000", "0.9642857143");
        assertConstituent(report, 405, "2.8835", "1");
        assertConstituent(report, 420, "0.6829", "1");
        JsonNode trace = traceOf(report);
        assertThat(security(trace, 403).path("weight").decimalValue()).isEqualByComparingTo("18.0000");
        assertThat(security(trace, 403).path("cappingFactor").decimalValue()).isEqualByComparingTo("0.9266183036");
    }

    @Test
    void review_whenVendorFileHasWholeMarket_expectIgnored() {
        HttpResult result = postReview("scenario15", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("constituents")))
            .containsExactlyElementsOf(IntStream.rangeClosed(101, 120).boxed().toList());
        assertThat(ids(report.path("joiners"))).isEmpty();
        assertThat(ids(report.path("leavers"))).isEmpty();
        assertConstituent(report, 101, "5.5307", "1");
        assertConstituent(report, 120, "4.4693", "1");
        JsonNode trace = traceOf(report);
        assertThat(findings(trace)).containsExactly(tuple("IGNORED_SECURITIES", null));
        assertThat(trace.path("securities").valueStream().map(node -> node.path("securityId").asInt()))
            .hasSize(22)
            .noneMatch(securityId -> securityId > 900);
    }

    @Test
    void review_whenAllComponentsReplaced_expectAllLeave() {
        HttpResult result = postReview("scenario16", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners")))
            .containsExactlyElementsOf(IntStream.rangeClosed(101, 120).boxed().toList());
        assertThat(ids(report.path("leavers")))
            .containsExactlyElementsOf(IntStream.rangeClosed(501, 520).boxed().toList());
        JsonNode trace = traceOf(report);
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_NEWCOMER");
        assertDecision(trace, 121, 21, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 501, 23, "NOT_SELECTED_BELOW_BUFFER");
        assertDecision(trace, 520, 42, "NOT_SELECTED_BELOW_BUFFER");
    }

    @Test
    void review_whenNoComponentInBuffer_expectNewcomers() {
        HttpResult result = postReview("scenario17", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(119, 120);
        assertThat(ids(report.path("leavers"))).containsExactly(124, 125);
        JsonNode trace = traceOf(report);
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_NEWCOMER");
        assertDecision(trace, 120, 20, "SELECTED_BUFFER_NEWCOMER");
        assertDecision(trace, 121, 21, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 124, 24, "NOT_SELECTED_BELOW_BUFFER");
    }

    @Test
    void review_whenBufferMixed_expectComponentThenNewcomer() {
        HttpResult result = postReview("scenario18", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(119);
        assertThat(ids(report.path("leavers"))).containsExactly(125);
        JsonNode trace = traceOf(report);
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_NEWCOMER");
        assertDecision(trace, 120, 20, "NOT_SELECTED_BUFFER");
        assertDecision(trace, 121, 21, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 122, 22, "NOT_SELECTED_BUFFER");
    }

    @Test
    void review_whenExactly20Eligible_expectAllSelected() {
        HttpResult result = postReview("scenario19", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("constituents")))
            .containsExactlyElementsOf(IntStream.rangeClosed(101, 120).boxed().toList());
        assertThat(ids(report.path("joiners"))).isEmpty();
        assertThat(ids(report.path("leavers"))).isEmpty();
        JsonNode trace = traceOf(report);
        assertThat(trace.path("securities").size()).isEqualTo(20);
        assertDecision(trace, 120, 20, "SELECTED_BUFFER_EXISTING_COMPONENT");
    }

    @Test
    void review_whenTieOnDirectRank_expectComponentDirect() {
        HttpResult result = postReview("scenario20", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(118);
        assertThat(ids(report.path("leavers"))).containsExactly(122);
        JsonNode trace = traceOf(report);
        assertDecision(trace, 130, 18, "SELECTED_DIRECT");
        assertDecision(trace, 118, 19, "SELECTED_BUFFER_NEWCOMER");
        assertDecision(trace, 121, 22, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 122, 23, "NOT_SELECTED_BELOW_BUFFER");
    }

    @Test
    void review_whenTieOnBufferEdge_expectComponentKept() {
        HttpResult result = postReview("scenario21", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(119);
        assertThat(ids(report.path("leavers"))).containsExactly(140);
        assertThat(report.path("findings").valueStream().map(finding -> finding.path("code").asString()))
            .containsExactly("SELECTION_TIE");
        JsonNode trace = traceOf(report);
        assertDecision(trace, 131, 22, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 122, 23, "NOT_SELECTED_BELOW_BUFFER");
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_NEWCOMER");
    }

    @Test
    void review_whenAllBelowCap_expectNoCapping() {
        HttpResult result = postReview("scenario22", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS");
        assertValidIndex(report);
        assertThat(report.path("constituents").valueStream()
            .map(constituent -> constituent.path("cappingFactor").decimalValue()))
            .allSatisfy(cappingFactor -> assertThat(cappingFactor).isEqualByComparingTo("1"));
        assertConstituent(report, 101, "6.3303", "1");
        assertConstituent(report, 120, "3.9177", "1");
        assertThat(ids(report.path("leavers"))).isEmpty();
        assertThat(security(traceOf(report), 101).path("freeFloat").decimalValue()).isEqualByComparingTo("1");
    }

    @Test
    void review_whenComponentHasNoData_expectLeaver() {
        HttpResult result = postReview("scenario23", null);

        assertThat(result.status()).isEqualTo(201);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("SUCCESS_WITH_WARNINGS");
        assertValidIndex(report);
        assertThat(ids(report.path("joiners"))).containsExactly(120);
        assertThat(ids(report.path("leavers"))).containsExactly(150);
        JsonNode trace = traceOf(report);
        assertThat(findings(trace)).containsExactlyInAnyOrder(
            tuple("MISSING_CUTOFF_PRICE", 150),
            tuple("MISSING_REVIEW_DATA", 150));
        assertThat(security(trace, 150).path("decision").asString()).isEqualTo("NOT_ELIGIBLE");
        assertThat(security(trace, 150).path("existingComponent").asBoolean()).isTrue();
        assertDecision(trace, 119, 19, "SELECTED_BUFFER_EXISTING_COMPONENT");
        assertDecision(trace, 120, 20, "SELECTED_BUFFER_NEWCOMER");
    }

    @Test
    void review_whenFreeFloatZero_expectFailed() {
        HttpResult result = postReview("scenario24", null);

        assertFailed(result, "INVALID_VALUE", 110);
    }

    private HttpResult postReview(String scenario, String configRequest) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        if (configRequest != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            parts.add("configRequest", new HttpEntity<>(configRequest, headers));
        }
        parts.add("universe", new ClassPathResource("e2e/" + scenario + "/spi_universe.csv"));
        parts.add("securityData", new ClassPathResource("e2e/" + scenario + "/sec_data.csv"));
        parts.add("composition", new ClassPathResource("e2e/" + scenario + "/composition.csv"));
        return client.post()
            .uri("/v1/reviews")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts)
            .exchange((request, response) -> new HttpResult(response.getStatusCode().value(),
                readJson(response.getBody())));
    }

    private HttpResult get(String path) {
        return client.get()
            .uri(path)
            .exchange((request, response) -> new HttpResult(response.getStatusCode().value(),
                readJson(response.getBody())));
    }

    private JsonNode traceOf(JsonNode report) {
        HttpResult trace = get("/v1/reviews/" + report.path("reviewId").asString() + "/trace");
        assertThat(trace.status()).isEqualTo(200);
        return trace.body();
    }

    private void assertFailed(HttpResult result, String errorCode, Integer securityId) {
        assertThat(result.status()).isEqualTo(422);
        JsonNode report = result.body();
        assertThat(report.path("status").asString()).isEqualTo("FAILED");
        assertThat(report.path("constituents").size()).isZero();
        assertThat(report.path("joiners").size()).isZero();
        assertThat(report.path("leavers").size()).isZero();
        String reviewId = report.path("reviewId").asString();
        assertThat(get("/v1/reviews/" + reviewId).body().path("status").asString()).isEqualTo("FAILED");
        assertThat(get("/v1/reviews/" + reviewId + "/audit").body().path("composition").size()).isZero();
        assertThat(findings(traceOf(report))).contains(tuple(errorCode, securityId));
    }

    private static void assertValidIndex(JsonNode report) {
        List<JsonNode> constituents = report.path("constituents").valueStream().toList();
        assertThat(constituents).hasSize(20);
        assertThat(constituents.stream().map(constituent -> constituent.path("securityId").asInt()).distinct())
            .hasSize(20);
        assertThat(constituents.stream().map(constituent -> constituent.path("weight").decimalValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(FULL_INDEX);
        assertThat(constituents).allSatisfy(constituent ->
            assertThat(constituent.path("weight").decimalValue()).isLessThanOrEqualTo(CAP));
    }

    private static void assertConstituent(JsonNode report, int securityId, String weight, String cappingFactor) {
        JsonNode constituent = report.path("constituents").valueStream()
            .filter(node -> node.path("securityId").asInt() == securityId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Security %d is not a constituent".formatted(securityId)));
        assertThat(constituent.path("weight").decimalValue()).isEqualByComparingTo(weight);
        assertThat(constituent.path("cappingFactor").decimalValue()).isEqualByComparingTo(cappingFactor);
    }

    private static void assertDecision(JsonNode trace, int securityId, int rank, String decision) {
        JsonNode security = security(trace, securityId);
        assertThat(security.path("rank").asInt()).isEqualTo(rank);
        assertThat(security.path("decision").asString()).isEqualTo(decision);
    }

    private static JsonNode security(JsonNode trace, int securityId) {
        return trace.path("securities").valueStream()
            .filter(node -> node.path("securityId").asInt() == securityId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Security %d has no trace".formatted(securityId)));
    }

    private static List<Tuple> findings(JsonNode trace) {
        return trace.path("findings").valueStream()
            .map(finding -> tuple(finding.path("code").asString(),
                finding.path("securityId").isNull() ? null : finding.path("securityId").asInt()))
            .toList();
    }

    private static List<Integer> ids(JsonNode nodes) {
        return nodes.valueStream()
            .map(node -> node.isObject() ? node.path("securityId").asInt() : node.asInt())
            .toList();
    }

    private static JsonNode readJson(InputStream body) throws IOException {
        return JSON.readTree(body.readAllBytes());
    }

    private record HttpResult(int status, JsonNode body) {
    }
}
