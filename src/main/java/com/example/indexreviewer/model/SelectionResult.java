package com.example.indexreviewer.model;

import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.SelectionTrace;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result of the selection step: the selected components in rank order, the rank and decision of every candidate, and
 * the findings of the selection.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SelectionResult {

    List<Candidate> components;
    List<SelectionTrace> traces;
    @Builder.Default
    List<Finding> findings = List.of();
}
