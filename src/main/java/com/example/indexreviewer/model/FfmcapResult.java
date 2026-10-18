package com.example.indexreviewer.model;

import com.example.indexreviewer.model.trace.FfmcapTrace;
import com.example.indexreviewer.model.trace.Finding;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result of the FFMCAP step: the candidates the selection can rank, ordered by security id, the trace record of every
 * security with the values used, and the findings of the data used.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FfmcapResult {

    List<Candidate> candidates;
    List<FfmcapTrace> traces;
    List<Finding> findings;
}
