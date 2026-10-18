package com.example.indexreviewer.repository.csv;

import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.exception.InvalidCsvFileException;
import com.example.indexreviewer.repository.FileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFileReaderTest {

    private final FileReader reader = new CsvFileReaderFactory().create(new CsvFormat(';'));

    @Test
    void read_whenAssignmentFiles_expectAllRows() throws IOException {
        List<Universe> universe = reader.read(resource("data/spi_universe.csv"), Universe.class);
        List<SecurityData> securityData = reader.read(resource("data/sec_data.csv"), SecurityData.class);
        List<Composition> composition = reader.read(resource("data/composition.csv"), Composition.class);

        assertThat(universe).hasSize(409);
        assertThat(securityData).hasSize(409);
        assertThat(composition).hasSize(20);
        assertThat(composition.getFirst().getId()).isEqualTo(155);
    }

    @Test
    void read_whenByteOrderMarkAndCrlf_expectCleanValues() {
        byte[] content = "﻿id\r\n155\r\n205\r\n".getBytes(StandardCharsets.UTF_8);

        List<Composition> composition = reader.read(content, Composition.class);

        assertThat(composition).extracting(Composition::getId).containsExactly(155, 205);
    }

    @Test
    void read_whenReviewDateRow_expectNullPrice() {
        byte[] content = "id;date;price;free_float;shares\n10;2026-09-21;;0.6766;309375\n"
            .getBytes(StandardCharsets.UTF_8);

        SecurityData row = reader.read(content, SecurityData.class).getFirst();

        assertThat(row.getId()).isEqualTo(10);
        assertThat(row.getDate()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(row.getPrice()).isNull();
        assertThat(row.getFreeFloat()).isEqualByComparingTo(new BigDecimal("0.6766"));
        assertThat(row.getShares()).isEqualByComparingTo(new BigDecimal("309375"));
    }

    @Test
    void read_whenHeaderOnly_expectNoRows() {
        byte[] content = "date;id\n".getBytes(StandardCharsets.UTF_8);

        List<Universe> universe = reader.read(content, Universe.class);

        assertThat(universe).isEmpty();
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("invalidSecurityData")
    void read_whenValueInvalid_expectLineInError(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> reader.read(bytes, SecurityData.class))
            .isInstanceOf(InvalidCsvFileException.class)
            .hasMessageContaining("line 2");
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("missingColumns")
    void read_whenColumnMissing_expectHeaderError(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> reader.read(bytes, SecurityData.class))
            .isInstanceOf(InvalidCsvFileException.class)
            .hasMessageContaining("header");
    }

    @ParameterizedTest(name = "{argumentSetName}")
    @MethodSource("optionalValues")
    void read_whenSharesAndFreeFloatMissing_expectNull(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        SecurityData row = reader.read(bytes, SecurityData.class).getFirst();

        assertThat(row.getPrice()).isEqualByComparingTo(new BigDecimal("2080"));
        assertThat(row.getFreeFloat()).isNull();
        assertThat(row.getShares()).isNull();
    }

    @Test
    void read_whenSeparatorDiffers_expectError() {
        byte[] content = "id,date,price,free_float,shares\n10,2026-09-10,2080,0.6766,309375\n"
            .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> reader.read(content, SecurityData.class))
            .isInstanceOf(InvalidCsvFileException.class);
    }

    @Test
    void read_whenCommaSeparatorConfigured_expectRows() {
        FileReader commaReader = new CsvFileReaderFactory().create(new CsvFormat(','));
        byte[] content = "date,id\n2026-09-21,10\n".getBytes(StandardCharsets.UTF_8);

        List<Universe> universe = commaReader.read(content, Universe.class);

        assertThat(universe).singleElement()
            .satisfies(row -> {
                assertThat(row.getId()).isEqualTo(10);
                assertThat(row.getDate()).isEqualTo(LocalDate.of(2026, 9, 21));
            });
    }

    static Stream<Arguments> invalidSecurityData() {
        String header = "id;date;price;free_float;shares\n";
        return Stream.of(
            Arguments.argumentSet("id not a number", header + "abc;2026-09-10;2080;0.6766;309375\n"),
            Arguments.argumentSet("id with decimals", header + "10.5;2026-09-10;2080;0.6766;309375\n"),
            Arguments.argumentSet("id empty", header + ";2026-09-10;2080;0.6766;309375\n"),
            Arguments.argumentSet("date not ISO", header + "10;2026/09/10;2080;0.6766;309375\n"),
            Arguments.argumentSet("price with comma", header + "10;2026-09-10;20,80;0.6766;309375\n"),
            Arguments.argumentSet("shares not a number", header + "10;2026-09-10;2080;0.6766;many\n"),
            Arguments.argumentSet("date empty", header + "10;;2080;0.6766;309375\n"));
    }

    static Stream<Arguments> missingColumns() {
        return Stream.of(
            Arguments.argumentSet("no id column", "date;price;free_float;shares\n2026-09-10;2080;0.6766;309375\n"),
            Arguments.argumentSet("no date column", "id;price;free_float;shares\n10;2080;0.6766;309375\n"));
    }

    static Stream<Arguments> optionalValues() {
        return Stream.of(
            Arguments.argumentSet("cut-off row with price only", "id;date;price;free_float;shares\n"
                + "10;2026-09-10;2080;;\n"),
            Arguments.argumentSet("no free float and shares columns", "id;date;price\n10;2026-09-10;2080\n"));
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream stream = CsvFileReaderTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as(path).isNotNull();
            return stream.readAllBytes();
        }
    }
}
