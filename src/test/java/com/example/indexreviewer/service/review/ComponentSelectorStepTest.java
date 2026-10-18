package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.SelectionResult;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.SelectionDecision;
import com.example.indexreviewer.model.trace.SelectionTrace;
import com.example.indexreviewer.service.review.smi.ComponentSelectorStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.example.indexreviewer.fixture.ReviewExecutionFixtures.SMI_RULES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ComponentSelectorStepTest {

    private final ComponentSelectorStep selector = new ComponentSelectorStep();

    @Test
    void select_whenNoExistingComponents_expectTopRanked() {
        List<Candidate> candidates = IntStream.rangeClosed(1, 21)
            .mapToObj(id -> newcomer(id, String.valueOf(id * 10)))
            .toList();

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(), SMI_RULES);

        assertThat(result.getComponents()).hasSize(20);
        assertThat(result.getComponents().getFirst().getSecurityId()).isEqualTo(21);
        assertThat(result.getComponents().getLast().getSecurityId()).isEqualTo(2);
        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank,
                SelectionTrace::getDecision)
            .contains(
                tuple(21, 1, SelectionDecision.SELECTED_DIRECT),
                tuple(4, 18, SelectionDecision.SELECTED_DIRECT),
                tuple(3, 19, SelectionDecision.SELECTED_BUFFER_NEWCOMER),
                tuple(2, 20, SelectionDecision.SELECTED_BUFFER_NEWCOMER),
                tuple(1, 21, SelectionDecision.NOT_SELECTED_BUFFER));
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void select_whenComponentsInBuffer_expectThemFirst() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "40"));
        candidates.add(newcomer(20, "30"));
        candidates.add(existingComponent(21, "20"));
        candidates.add(existingComponent(22, "10"));
        candidates.add(existingComponent(23, "5"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(21, 22, 23), SMI_RULES);

        assertThat(result.getComponents()).hasSize(20);
        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank,
                SelectionTrace::getDecision)
            .contains(
                tuple(19, 19, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(20, 20, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(21, 21, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(22, 22, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(23, 23, SelectionDecision.NOT_SELECTED_BELOW_BUFFER));
    }

    @Test
    void select_whenOneSeatInBuffer_expectNewcomerByRank() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "40"));
        candidates.add(newcomer(20, "30"));
        candidates.add(existingComponent(21, "20"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(21), SMI_RULES);

        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getDecision)
            .contains(
                tuple(19, SelectionDecision.SELECTED_BUFFER_NEWCOMER),
                tuple(20, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(21, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT));
    }

    @Test
    void select_whenFfmcapEqual_expectComponentsFirst() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(40, "500"));
        candidates.add(existingComponent(30, "500"));
        candidates.add(newcomer(20, "500"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(30), SMI_RULES);

        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank)
            .contains(tuple(30, 19), tuple(20, 20), tuple(40, 21));
    }

    @Test
    void select_whenRulesCannotFillIndex_expectSizeError() {
        List<Candidate> candidates = IntStream.rangeClosed(1, 22)
            .mapToObj(id -> newcomer(id, String.valueOf(id * 10)))
            .toList();

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(), rulesWithBufferRank(19));

        assertThat(result.getComponents()).hasSize(19);
        assertThat(result.getTraces()).hasSize(22);
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.SELECTION_SIZE);
            assertThat(finding.getMessage()).isEqualTo("Selected 19 components but the index needs 20");
        });
    }

    @Test
    void select_whenTieAcrossBuffer_expectComponentKept() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "11"));
        candidates.add(newcomer(20, "10"));
        candidates.add(newcomer(21, "9"));
        candidates.add(newcomer(22, "8"));
        candidates.add(existingComponent(23, "8"));
        candidates.add(existingComponent(24, "8.00"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(23, 24), SMI_RULES);

        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank,
                SelectionTrace::getDecision)
            .contains(
                tuple(19, 19, SelectionDecision.SELECTED_BUFFER_NEWCOMER),
                tuple(23, 22, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(24, 23, SelectionDecision.NOT_SELECTED_BELOW_BUFFER),
                tuple(22, 24, SelectionDecision.NOT_SELECTED_BELOW_BUFFER));
        assertThat(result.getFindings()).extracting(Finding::getCode).containsExactly(FindingCode.SELECTION_TIE);
        assertThat(result.getFindings().getFirst().getMessage()).contains("[23]", "[24, 22]");
    }

    @Test
    void select_whenTiedComponents_expectCompositionOrder() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "11"));
        candidates.add(newcomer(20, "10"));
        IntStream.rangeClosed(21, 30)
            .mapToObj(id -> List.of(25, 28, 30).contains(id) ? existingComponent(id, "8") : newcomer(id, "8"))
            .forEach(candidates::add);

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(30, 25, 28), SMI_RULES);

        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank,
                SelectionTrace::getDecision)
            .contains(
                tuple(19, 19, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(20, 20, SelectionDecision.NOT_SELECTED_BUFFER),
                tuple(30, 21, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(25, 22, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT),
                tuple(28, 23, SelectionDecision.NOT_SELECTED_BELOW_BUFFER),
                tuple(21, 24, SelectionDecision.NOT_SELECTED_BELOW_BUFFER));
        assertThat(result.getFindings().getFirst().getMessage()).contains("[30, 25]");
    }

    @Test
    void select_whenTieAllSelected_expectNoWarning() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "40"));
        candidates.add(newcomer(20, "40"));
        candidates.add(newcomer(21, "10"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(), SMI_RULES);

        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void select_whenExactly20Candidates_expectAllSelected() {
        List<Candidate> candidates = new ArrayList<>(directCandidates());
        candidates.add(newcomer(19, "40"));
        candidates.add(existingComponent(20, "30"));

        SelectionResult result = selector.selectTopCandidates(candidates, List.of(20), SMI_RULES);

        assertThat(result.getComponents()).extracting(Candidate::getSecurityId)
            .containsExactlyElementsOf(IntStream.rangeClosed(1, 20).boxed().toList());
        assertThat(result.getTraces()).extracting(SelectionTrace::getSecurityId, SelectionTrace::getRank,
                SelectionTrace::getDecision)
            .contains(
                tuple(18, 18, SelectionDecision.SELECTED_DIRECT),
                tuple(19, 19, SelectionDecision.SELECTED_BUFFER_NEWCOMER),
                tuple(20, 20, SelectionDecision.SELECTED_BUFFER_EXISTING_COMPONENT));
        assertThat(result.getFindings()).isEmpty();
    }

    private static IndexRules rulesWithBufferRank(int bufferRank) {
        return IndexRules.builder()
            .numberOfComponents(20)
            .directSelectionRank(18)
            .bufferRank(bufferRank)
            .capPercentage(new BigDecimal("18"))
            .methodologyVersion("test")
            .build();
    }

    private static List<Candidate> directCandidates() {
        return IntStream.rangeClosed(1, 18)
            .mapToObj(id -> newcomer(id, String.valueOf(1000 - id)))
            .toList();
    }

    private static Candidate newcomer(int securityId, String ffmcap) {
        return Candidate.builder()
            .securityId(securityId)
            .ffmcap(new BigDecimal(ffmcap))
            .build();
    }

    private static Candidate existingComponent(int securityId, String ffmcap) {
        return Candidate.builder()
            .securityId(securityId)
            .existingComponent(true)
            .ffmcap(new BigDecimal(ffmcap))
            .build();
    }
}
