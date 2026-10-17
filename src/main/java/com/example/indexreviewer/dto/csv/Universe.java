package com.example.indexreviewer.dto.csv;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class Universe {

    @CsvDate("yyyy-MM-dd")
    @CsvBindByName(column = "date", required = true)
    private LocalDate date;

    @CsvBindByName(column = "id", required = true)
    private Integer id;
}
