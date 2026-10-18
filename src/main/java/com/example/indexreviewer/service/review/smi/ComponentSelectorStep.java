package com.example.indexreviewer.service.review.smi;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.SelectionResult;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.model.trace.SelectionTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Ranks the candidates by FFMCAP and selects the index components with the selection buffer (rulebook section
 * 5.12.3.2). Equal FFMCAPs are ranked with existing components first, in the order of the current composition, then by
 * security id, which the rulebook does not define (see docs/methodology.md).
 */
@Slf4j
@Component
public class ComponentSelectorStep {

    /**
     * Returns the selected candidates in rank order, the trace record of every candidate and a warning for every group
     * of equal FFMCAPs that the tie order split into selected and not selected candidates.
     */
    public SelectionResult selectTopCandidates(List<Candidate> candidates, List<Integer> composition,
                                               IndexRules rules) {
        List<Candidate> rankedCandidates = candidates.stream().sorted(rankCandidates(composition)).toList();
        List<SelectionDecision> decisions = decideIfSelected(rankedCandidates, rules);

        List<Candidate> selected = new ArrayList<>();
        List<SelectionTrace> traces = new ArrayList<>();
        for (int index = 0; index < rankedCandidates.size(); index++) {
            Candidate candidate = rankedCandidates.get(index);
            SelectionDecision decision = decisions.get(index);
            if (decision.isSelected()) {
                selected.add(candidate);
            }
            traces.add(SelectionTrace.builder()
                .securityId(candidate.getSecurityId())
                .rank(index + 1)
                .decision(decision)
                .build());
        }
        var findings = new ArrayList<>(buildFindings(rankedCandidates, decisions));
        findings.addAll(checkSelectionSize(selected.size(), rules));

        return SelectionResult.builder()
            .components(selected)
            .traces(traces)
            .findings(findings)
            .build();
    }

    /**
     * Orders by FFMCAP, highest first. Equal FFMCAPs put existing components first, in the order they appear in the
     * current composition, and then the newcomers by security id.
     */
    private static Comparator<Candidate> rankCandidates(List<Integer> composition) {
        var originalCandidatesOrder = IntStream.range(0, composition.size()).boxed()
            .collect(Collectors.toMap(composition::get, Function.identity(), (a, b) -> a));

        return Comparator.comparing(Candidate::getFfmcap).reversed()
            .thenComparing(Candidate::isExistingComponent, Comparator.reverseOrder())
            .thenComparing(
                candidate -> originalCandidatesOrder.getOrDefault(candidate.getSecurityId(), Integer.MAX_VALUE))
            .thenComparing(Candidate::getSecurityId);
    }

    /**
     * Ranks up to the direct selection rank are selected. The remaining seats go to the existing components in the
     * buffer, then to the newcomers in the buffer, both in rank order. Candidates below the buffer are not selected.
     */
    private static List<SelectionDecision> decideIfSelected(List<Candidate> candidates, IndexRules rules) {
        int openSeats = rules.getNumberOfComponents() - Math.min(rules.getDirectSelectionRank(), candidates.size());
        int bufferEnd = Math.min(rules.getBufferRank(), candidates.size());

        List<SelectionDecision> decisions = IntStream.rangeClosed(1, candidates.size())
            .mapToObj(
                rank -> rank <= rules.getDirectSelectionRank() ? SelectionDecision.SELECTED_DIRECT
                    : SelectionDecision.NOT_SELECTED_BELOW_BUFFER)
            .collect(Collectors.toList());

        for (int index = rules.getDirectSelectionRank(); index < bufferEnd; index++) {
            decisions.set(index, SelectionDecision.NOT_SELECTED_BUFFER);
        }
        //fill with existing components first,
        openSeats = fillBufferSeats(candidates, decisions, rules.getDirectSelectionRank(), bufferEnd, openSeats,
            true);
        //fill with the newcomers
        fillBufferSeats(candidates, decisions, rules.getDirectSelectionRank(), bufferEnd, openSeats, false);
        return decisions;
    }

    private static int fillBufferSeats(List<Candidate> ranked, List<SelectionDecision> decisions, int bufferStart,
                                       int bufferEnd, int openSeats, boolean existingComponents) {
        int seats = openSeats;
        for (int index = bufferStart; index < bufferEnd && seats > 0; index++) {
            if (ranked.get(index).isExistingComponent() == existingComponents) {
                decisions.set(index, existingComponents ? SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT
                    : SelectionDecision.SELECTED_BUFFER_NEWCOMER);
                seats--;
            }
        }
        return seats;
    }

    /**
     * Returns a {@code SELECTION_SIZE} error when the selection does not have exactly the number of components the
     * index needs, otherwise no finding. The rank order check in the configuration and the candidate count check before
     * this step prevent it, so the error only shows a wrong configuration or a bug instead of returning a wrong index.
     */
    private static List<Finding> checkSelectionSize(int selectedCount, IndexRules rules) {
        if (selectedCount == rules.getNumberOfComponents()) {
            return List.of();
        }
        return List.of(Finding.builder()
            .code(FindingCode.SELECTION_SIZE)
            .message("Selected %d components but the index needs %d"
                .formatted(selectedCount, rules.getNumberOfComponents()))
            .build());
    }

    /**
     * Returns a warning for every group of equal FFMCAPs with selected and not selected candidates, because the tie
     * order and not the FFMCAP decided which of them joined the index.
     */
    private static List<Finding> buildFindings(List<Candidate> ranked, List<SelectionDecision> decisions) {
        Map<BigDecimal, List<Integer>> selectedByFfmcap = new TreeMap<>();
        Map<BigDecimal, List<Integer>> notSelectedByFfmcap = new TreeMap<>();
        for (int index = 0; index < ranked.size(); index++) {
            Candidate candidate = ranked.get(index);
            var group = decisions.get(index).isSelected() ? selectedByFfmcap : notSelectedByFfmcap;
            group.computeIfAbsent(candidate.getFfmcap().stripTrailingZeros(), ffmcap -> new ArrayList<>())
                .add(candidate.getSecurityId());
        }

        List<Finding> findings = new ArrayList<>();
        selectedByFfmcap.forEach((ffmcap, selectedIds) -> {
            List<Integer> notSelectedIds = notSelectedByFfmcap.get(ffmcap);
            if (notSelectedIds != null) {
                findings.add(Finding.builder()
                    .code(FindingCode.SELECTION_TIE)
                    .field("ffmcap")
                    .message(("Securities %s were selected and %s were not, although all have the same FFMCAP %s; "
                        + "the tie was decided by existing components first, in the order of the current "
                        + "composition, then by the lower security id")
                        .formatted(selectedIds, notSelectedIds, ffmcap.toPlainString()))
                    .build());
            }
        });
        return findings;
    }
}
