package com.example.indexreviewer.exception;

import com.example.indexreviewer.model.IndexType;
import lombok.Getter;

@Getter
public class IndexNotConfiguredException extends RuntimeException {

    private final IndexType indexType;

    public IndexNotConfiguredException(IndexType indexType) {
        super("Index is not configured: " + indexType);
        this.indexType = indexType;
    }
}
