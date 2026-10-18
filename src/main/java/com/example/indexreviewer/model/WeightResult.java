package com.example.indexreviewer.model;

import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.WeightTrace;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result of the weighting step: the index components in rank order with their weighting factors, the trace record of
 * every component, and the findings of the weighting.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeightResult {

    List<Constituent> constituents;
    List<WeightTrace> traces;
    List<Finding> findings;
}
