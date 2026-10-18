package com.example.indexreviewer.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One security data row. The price is null on review date rows, where the file leaves it empty.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Security {

    Integer securityId;
    LocalDate date;
    BigDecimal price;
    BigDecimal freeFloat;
    BigDecimal shares;
}
