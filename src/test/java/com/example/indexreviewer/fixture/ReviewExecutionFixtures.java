package com.example.indexreviewer.fixture;

import com.example.indexreviewer.controller.mapper.ReviewCommandMapperImpl;
import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.ReviewStatus;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.audit.ReviewAudit;
import com.example.indexreviewer.model.report.ReviewReport;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.ReviewTrace;
import com.example.indexreviewer.model.trace.SecurityTrace;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.repository.FileReader;
import com.example.indexreviewer.repository.csv.CsvFileReaderFactory;
import com.example.indexreviewer.repository.csv.CsvFormat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReviewExecutionFixtures {

    public static final UUID REVIEW_ID = UUID.fromString("3f2b1c4e-8d6a-4b7e-9c1d-2a5e6f7a8b9c");
    public static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    public static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);
    public static final Instant EXECUTED_AT = Instant.parse("2026-10-17T08:00:00Z");
    public static final String APP_VERSION = "0.1.0-SNAPSHOT";
    public static final IndexRules SMI_RULES = IndexRules.builder()
        .numberOfComponents(20)
        .directSelectionRank(18)
        .bufferRank(22)
        .capPercentage(new BigDecimal("18"))
        .methodologyVersion("SIX Swiss Index Methodology Rulebook 3.40, 08.06.2026")
        .build();

    public static final SecurityTrace ELIGIBLE_SECURITY = SecurityTrace.builder()
        .securityId(10)
        .existingComponent(false)
        .price(new BigDecimal("2080"))
        .shares(new BigDecimal("309375"))
        .freeFloat(new BigDecimal("0.6766"))
        .ffmcap(new BigDecimal("435392100"))
        .build();
    public static final SecurityTrace EXCLUDED_SECURITY = SecurityTrace.builder()
        .securityId(166)
        .existingComponent(false)
        .price(new BigDecimal("0.13"))
        .decision(SelectionDecision.NOT_ELIGIBLE)
        .build();

    public static final Finding MISSING_REVIEW_DATA = Finding.builder()
        .code(FindingCode.MISSING_REVIEW_DATA)
        .securityId(166)
        .message("Security 166 excluded: no shares and free float on the review date")
        .build();
    public static final Finding COMPOSITION_SIZE = Finding.builder()
        .code(FindingCode.COMPOSITION_SIZE)
        .field("composition")
        .message("Composition has 19 securities but the SMI needs 20")
        .build();

    public static final SecurityTrace CAPPED_COMPONENT = SecurityTrace.builder()
        .securityId(155)
        .existingComponent(true)
        .rank(1)
        .decision(SelectionDecision.SELECTED_DIRECT)
        .cappingFactor(new BigDecimal("0.565353"))
        .weight(new BigDecimal("18"))
        .build();
    public static final SecurityTrace JOINER = SecurityTrace.builder()
        .securityId(177)
        .existingComponent(false)
        .rank(7)
        .decision(SelectionDecision.SELECTED_DIRECT)
        .cappingFactor(BigDecimal.ONE)
        .weight(new BigDecimal("4.6749"))
        .build();
    public static final SecurityTrace LEAVER = SecurityTrace.builder()
        .securityId(103)
        .existingComponent(true)
        .rank(27)
        .decision(SelectionDecision.NOT_SELECTED_BELOW_BUFFER)
        .build();

    private ReviewExecutionFixtures() {
    }

    public static ReviewTrace trace(List<SecurityTrace> securities, List<Finding> findings) {
        return ReviewTrace.builder()
            .securities(securities)
            .findings(findings)
            .build();
    }

    public static ReviewReport report(List<SecurityTrace> securities, List<Finding> findings) {
        return ReviewReport.of(audit(ReviewStatus.of(findings)), trace(securities, findings));
    }

    public static ReviewAudit audit(ReviewStatus status) {
        return audit(status, List.of());
    }

    public static ReviewAudit audit(ReviewStatus status, List<Integer> composition) {
        return ReviewAudit.builder()
            .reviewId(REVIEW_ID)
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .status(status)
            .executedAt(EXECUTED_AT)
            .appVersion(APP_VERSION)
            .rules(SMI_RULES)
            .composition(composition)
            .build();
    }

    /**
     * Review command built from the assignment files in src/test/resources/data, read like an upload.
     */
    public static ReviewCommand assignmentCommand() throws IOException {
        FileReader reader = new CsvFileReaderFactory().create(new CsvFormat(';'));
        return new ReviewCommandMapperImpl().toServiceModel(new ConfigRequest(),
            reader.read(resource("data/spi_universe.csv"), Universe.class),
            reader.read(resource("data/sec_data.csv"), SecurityData.class),
            reader.read(resource("data/composition.csv"), Composition.class));
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream stream = ReviewExecutionFixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource " + path);
            }
            return stream.readAllBytes();
        }
    }
}
