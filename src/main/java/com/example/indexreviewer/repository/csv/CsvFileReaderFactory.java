package com.example.indexreviewer.repository.csv;

import com.example.indexreviewer.repository.FileReader;
import com.example.indexreviewer.repository.FileReaderFactory;
import org.springframework.stereotype.Component;

@Component
public class CsvFileReaderFactory implements FileReaderFactory {

    @Override
    public FileReader create(FileFormat format) {
        return switch (format) {
            case CsvFormat csv -> new CsvFileReader(csv.separator());
        };
    }
}
