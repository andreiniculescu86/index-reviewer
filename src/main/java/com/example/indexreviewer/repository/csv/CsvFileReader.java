package com.example.indexreviewer.repository.csv;

import com.example.indexreviewer.exception.InvalidCsvFileException;
import com.example.indexreviewer.repository.FileReader;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.processor.RowProcessor;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class CsvFileReader implements FileReader {

    private static final char BYTE_ORDER_MARK = 0xFEFF;
    private static final RowProcessor TRIMMING_PROCESSOR = new TrimmingRowProcessor();

    private final CSVParser parser;

    CsvFileReader(char separator) {
        this.parser = new CSVParserBuilder().withSeparator(separator).build();
    }

    @Override
    public <T> List<T> read(byte[] content, Class<T> type) {
        String text = new String(content, StandardCharsets.UTF_8);
        text = removeMarkerIfPresent(text);
        try (CSVReader reader = new CSVReaderBuilder(new StringReader(text))
            .withCSVParser(parser)
            .withRowProcessor(TRIMMING_PROCESSOR)
            .build()) {
            List<T> rows = new ArrayList<>();

            new CsvToBeanBuilder<T>(reader).withType(type).build()
                .iterator()
                .forEachRemaining(rows::add);
            return rows;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof CsvException csvException) {
                throw new InvalidCsvFileException(errorMessage(csvException.getLineNumber(), csvException), exception);
            }
            if (exception.getCause() instanceof CsvMalformedLineException malformed) {
                throw new InvalidCsvFileException(errorMessage(malformed.getLineNumber(), malformed), exception);
            }
            throw exception;
        }
    }

    private static String errorMessage(long lineNumber, Exception exception) {
        String location = lineNumber > 0 ? "line " + lineNumber : "header";
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return "Invalid CSV at %s: %s".formatted(location, rootCause.getMessage());
    }

    private static String removeMarkerIfPresent(String text) {
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    }

    /**
     * Strips leading and trailing whitespace from every value, including the header names.
     */
    private static final class TrimmingRowProcessor implements RowProcessor {

        @Override
        public String processColumnItem(String column) {
            return column == null ? null : column.strip();
        }

        @Override
        public void processRow(String[] row) {
            for (int i = 0; i < row.length; i++) {
                row[i] = processColumnItem(row[i]);
            }
        }
    }
}
