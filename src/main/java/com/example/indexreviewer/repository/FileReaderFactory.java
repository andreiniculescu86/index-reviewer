package com.example.indexreviewer.repository;

import com.example.indexreviewer.repository.csv.FileFormat;

public interface FileReaderFactory {

    FileReader create(FileFormat format);
}
