package com.example.indexreviewer.dto.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class SecurityData {

    @CsvBindByName(column = "id", required = true)
    private Integer id;

    @CsvDate("yyyy-MM-dd")
    @CsvBindByName(column = "date", required = true)
    private LocalDate date;

    @CsvBindByName(column = "price")
    private BigDecimal price;

    @CsvBindByName(column = "free_float", required = true)
    private BigDecimal freeFloat;

    @CsvBindByName(column = "shares", required = true)
    private BigDecimal shares;
}
