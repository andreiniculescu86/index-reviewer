package com.example.indexreviewer.model;

import java.time.LocalDate;

public record ReviewResult(IndexType indexType, LocalDate cutoffDate, LocalDate reviewDate) {
}
