package com.example.indexreviewer.repository;

import java.util.List;

public interface FileReader {

    <T> List<T> read(byte[] content, Class<T> type);
}
