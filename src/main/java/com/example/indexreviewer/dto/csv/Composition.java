package com.example.indexreviewer.dto.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Composition {

    @CsvBindByName(column = "id", required = true)
    private Integer id;
}
