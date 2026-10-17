package com.example.indexreviewer.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One security data row. The price is null on review date rows, where the file leaves it empty.
 */
public record SecurityRecord(Integer securityId, LocalDate date, BigDecimal price, BigDecimal freeFloat,
                             BigDecimal shares) {
}
