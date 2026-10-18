package com.example.indexreviewer.dto.response.review;

import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.Severity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A data problem found during the review.")
public record FindingResponse(
    FindingCode code,
    @Schema(description = "ERROR fails the review, WARNING is handled and recorded, INFO does not affect the review.")
    Severity severity,
    @Schema(description = "Security the finding is about; null when it is about the whole input.")
    Integer securityId,
    @Schema(description = "Data field or input file that caused the finding, when there is one.")
    String field,
    String message) {
}
