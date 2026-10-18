package com.example.indexreviewer.model.trace;

import lombok.Builder;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traceability part of a review execution: the trace of every security and all findings.
 */
@Value
public class ReviewTrace {

    List<SecurityTrace> securities;
    List<Finding> findings;

    /**
     * Copies the lists, so that a stored trace cannot change. A list that is not set is empty.
     */
    @Builder
    private ReviewTrace(List<SecurityTrace> securities, List<Finding> findings) {
        this.securities = securities == null ? List.of() : List.copyOf(securities);
        this.findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /**
     * Combines the records of the review steps into one trace per security, ordered by security id. Every security of
     * a review has an FFMCAP record; the records of steps that did not run for it are missing.
     */
    public static ReviewTrace of(List<Finding> findings, List<FfmcapTrace> ffmcap, List<SelectionTrace> selection,
                                 List<WeightTrace> weights) {
        Map<Integer, SelectionTrace> selectionById = selection.stream()
            .collect(Collectors.toMap(SelectionTrace::getSecurityId, Function.identity()));
        Map<Integer, WeightTrace> weightById = weights.stream()
            .collect(Collectors.toMap(WeightTrace::getSecurityId, Function.identity()));
        List<SecurityTrace> securities = ffmcap.stream()
            .sorted(Comparator.comparing(FfmcapTrace::getSecurityId))
            .map(security -> SecurityTrace.of(security, selectionById.get(security.getSecurityId()),
                weightById.get(security.getSecurityId())))
            .toList();
        return builder()
            .securities(securities)
            .findings(findings)
            .build();
    }

    /**
     * Returns the part of this trace about one security: its trace, if it has one, and its findings.
     */
    public ReviewTrace forSecurity(Integer securityId) {
        return ReviewTrace.builder()
            .securities(securities.stream().filter(security -> security.getSecurityId().equals(securityId)).toList())
            .findings(findings.stream().filter(finding -> securityId.equals(finding.getSecurityId())).toList())
            .build();
    }

    public boolean isEmpty() {
        return securities.isEmpty() && findings.isEmpty();
    }
}
