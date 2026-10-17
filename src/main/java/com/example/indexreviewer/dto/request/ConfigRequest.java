package com.example.indexreviewer.dto.request;

import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.validation.ValidReviewDates;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ValidReviewDates
@Schema(description = "Optional. Provide custom index type and dates of the review. If not set, falls back to defaults")
public class ConfigRequest {

    @NotNull
    @Builder.Default
    @Schema(description = "Index identifier. Defaults to SMI.", example = "SMI",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private IndexType indexType = IndexType.SMI;

    @NotNull
    @Builder.Default
    @Schema(description = "Cut-off date. Defaults to 2026-09-10.", example = "2026-09-10",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate cutoffDate = LocalDate.of(2026, 9, 10);

    @NotNull
    @Builder.Default
    @Schema(description = "Review date. Universe, shares, free float and all other review data are taken from this "
        + "date. Must not be before the cut-off date. Defaults to 2026-09-21.",
        example = "2026-09-21", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate reviewDate = LocalDate.of(2026, 9, 21);

    @NotNull
    @Builder.Default
    @Schema(description = "Column separator of the uploaded CSV files. Defaults to ';'.", example = ";",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Character csvSeparator = ';';
}
