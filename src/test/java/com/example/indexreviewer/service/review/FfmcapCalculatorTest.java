package com.example.indexreviewer.service.review;

import com.example.indexreviewer.controller.mapper.ReviewCommandMapperImpl;
import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.SecurityRecord;
import com.example.indexreviewer.repository.FileReader;
import com.example.indexreviewer.repository.csv.CsvFileReaderFactory;
import com.example.indexreviewer.repository.csv.CsvFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FfmcapCalculatorTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);

    private final FfmcapCalculator calculator = new FfmcapCalculator(new SecurityRecordValidator());

    @Test
    void calculate_whenRowsComplete_expectReviewDateValues() {
        ReviewCommand command = command(Set.of(101), List.of(
            cutoffRow(101, "311.5", "0.8048", "5800000"),
            reviewRow(101, "0.8476", "5835000")));

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).containsOnlyKeys(101);
        assertThat(ffmcaps.get(101)).isEqualByComparingTo("1540599879");
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).isEmpty();
    }

    @Test
    void calculate_whenNoReviewDateRow_expectExcluded() {
        ReviewCommand command = command(Set.of(10, 166), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(166, "0.13", "0.087", "1139771344")));

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).containsOnlyKeys(10);
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).singleElement().asString().contains("166");
    }

    @Test
    void calculate_whenNoCutoffPrice_expectExcluded() {
        ReviewCommand command = command(Set.of(10, 400), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            reviewRow(400, "0.5", "1000000")));

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).containsOnlyKeys(10);
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).singleElement().asString().contains("400");
    }

    @Test
    void calculate_whenNotInUniverse_expectIgnored() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(999, "50", "1", "1000"),
            reviewRow(999, "1", "1000")));

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).containsOnlyKeys(10);
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).isEmpty();
        assertThat(result.validation().getInfos()).hasSize(1);
    }

    @Test
    void calculate_whenUniverseOnOtherDate_expectNoResult() {
        ReviewCommand command = new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE,
            Map.of(CUTOFF_DATE, Set.of(10)),
            List.of(cutoffRow(10, "2080", "0.6766", "309375"), reviewRow(10, "0.6766", "309375")),
            List.of());

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).isEmpty();
    }

    @Test
    void calculate_whenRowRepeated_expectCountedOnce() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            cutoffRow(10, "2080", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375")));

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps.get(10)).isEqualByComparingTo("435392100");
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).singleElement().asString().contains("10");
    }

    @Test
    void calculate_whenRowsConflict_expectError() {
        ReviewCommand command = command(Set.of(10), List.of(
            cutoffRow(10, "2080", "0.6766", "309375"),
            cutoffRow(10, "2100", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(result.validation().hasErrors()).isTrue();
        assertThat(result.validation().getErrors()).singleElement().asString().contains("10");
    }

    @Test
    void calculate_whenSeveralRowsInvalid_expectAllErrors() {
        ReviewCommand command = command(Set.of(10, 11, 12), List.of(
            cutoffRow(10, "0", "0.6766", "309375"),
            reviewRow(10, "0.6766", "309375"),
            cutoffRow(11, "898", "0.72", "1485000"),
            reviewRow(11, "1.5", "1485000"),
            cutoffRow(12, "131", "0.4172", "1960000"),
            reviewRow(12, "0.4172", "-1960000")));

        FfmcapResult result = calculator.calculate(command);

        assertThat(result.validation().getErrors()).hasSize(3);
    }

    @Test
    void calculate_whenAssignmentData_expect204Values() throws IOException {
        ReviewCommand command = assignmentCommand();

        FfmcapResult result = calculator.calculate(command);
        Map<Integer, BigDecimal> ffmcaps = result.ffmcaps();

        assertThat(ffmcaps).hasSize(204).doesNotContainKey(166);
        assertThat(ffmcaps.get(10)).isEqualByComparingTo("435392100");
        assertThat(result.validation().getErrors()).isEmpty();
        assertThat(result.validation().getWarnings()).singleElement().asString().contains("166");
    }

    private static ReviewCommand command(Set<Integer> universe, List<SecurityRecord> rows) {
        return new ReviewCommand(IndexType.SMI, CUTOFF_DATE, REVIEW_DATE, Map.of(REVIEW_DATE, universe), rows,
            List.of());
    }

    private static SecurityRecord cutoffRow(int id, String price, String freeFloat, String shares) {
        return new SecurityRecord(id, CUTOFF_DATE, new BigDecimal(price), new BigDecimal(freeFloat),
            new BigDecimal(shares));
    }

    private static SecurityRecord reviewRow(int id, String freeFloat, String shares) {
        return new SecurityRecord(id, REVIEW_DATE, null, new BigDecimal(freeFloat), new BigDecimal(shares));
    }

    private static ReviewCommand assignmentCommand() throws IOException {
        FileReader reader = new CsvFileReaderFactory().create(new CsvFormat(';'));
        return new ReviewCommandMapperImpl().toServiceModel(new ConfigRequest(),
            reader.read(resource("data/spi_universe.csv"), Universe.class),
            reader.read(resource("data/sec_data.csv"), SecurityData.class),
            reader.read(resource("data/composition.csv"), Composition.class));
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream stream = FfmcapCalculatorTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as(path).isNotNull();
            return stream.readAllBytes();
        }
    }
}
