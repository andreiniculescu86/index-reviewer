package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.WeightTrace;
import com.example.indexreviewer.service.review.smi.WeightCalculatorStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeightCalculatorStepTest {

    private static final BigDecimal HALF = new BigDecimal("50");

    private final WeightCalculatorStep calculator = new WeightCalculatorStep();

    @Test
    void calculateWeights_whenAboveCap_expectRedistributed() {
        List<Candidate> components = List.of(component(1, "600"), component(2, "300"), component(3, "100"));

        WeightResult result = calculator.calculateWeights(components, HALF);

        assertThat(result.getConstituents()).extracting(Constituent::getSecurityId).containsExactly(1, 2, 3);
        assertThat(result.getTraces()).extracting(WeightTrace::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("50"), new BigDecimal("37.5"), new BigDecimal("12.5"));
        assertThat(result.getTraces()).extracting(WeightTrace::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("0.6667"), BigDecimal.ONE, BigDecimal.ONE);
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculateWeights_whenCappedInRounds_expectCap() {
        List<Candidate> components = List.of(component(1, "8928.00"), component(2, "8438.75"),
            component(3, "7860.93"), component(4, "7235.20"), component(5, "6562.50"));

        WeightResult result = calculator.calculateWeights(components, new BigDecimal("21"));

        assertThat(result.getConstituents()).extracting(Constituent::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("21"), new BigDecimal("21"), new BigDecimal("21"),
                new BigDecimal("19.4020"), new BigDecimal("17.5980"));
        assertThat(result.getConstituents()).extracting(Constituent::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("0.8771"), new BigDecimal("0.9280"),
                new BigDecimal("0.9962"), BigDecimal.ONE, BigDecimal.ONE);
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculateWeights_whenAllBelowCap_expectFfmcapWeights() {
        List<Candidate> components = List.of(component(1, "400"), component(2, "350"), component(3, "250"));

        WeightResult result = calculator.calculateWeights(components, HALF);

        assertThat(result.getTraces()).extracting(WeightTrace::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("40"), new BigDecimal("35"), new BigDecimal("25"));
        assertThat(result.getTraces()).extracting(WeightTrace::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsOnly(BigDecimal.ONE);
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculateWeights_whenCapAddsUpTo100_expectCap() {
        List<Candidate> components = List.of(component(1, "400"), component(2, "300"), component(3, "200"),
            component(4, "100"));

        WeightResult result = calculator.calculateWeights(components, new BigDecimal("25"));

        assertThat(result.getConstituents()).extracting(Constituent::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsOnly(new BigDecimal("25"));
        assertThat(result.getConstituents()).extracting(Constituent::getCappingFactor)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("0.25"), new BigDecimal("0.3333"), new BigDecimal("0.5"),
                BigDecimal.ONE);
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculateWeights_whenCapTooLow_expectInvalid() {
        List<Candidate> components = List.of(component(1, "400"), component(2, "350"), component(3, "250"));

        WeightResult result = calculator.calculateWeights(components, new BigDecimal("30"));

        assertThat(result.getConstituents()).extracting(Constituent::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("30"), new BigDecimal("30"), new BigDecimal("40"));
        assertThat(result.getFindings()).singleElement().satisfies(finding -> {
            assertThat(finding.getCode()).isEqualTo(FindingCode.INVALID_WEIGHTS);
            assertThat(finding.getMessage()).contains("The largest weight is 40.0000%");
        });
    }

    @Test
    void calculateWeights_whenRoundedDown_expectLastRaised() {
        List<Candidate> components = List.of(component(1, "100"), component(2, "100"), component(3, "100"));

        WeightResult result = calculator.calculateWeights(components, HALF);

        assertThat(result.getConstituents()).extracting(Constituent::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("33.3333"), new BigDecimal("33.3333"), new BigDecimal("33.3334"));
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void calculateWeights_whenRoundedUp_expectLastLowered() {
        List<Candidate> components = List.of(component(1, "100"), component(2, "100"), component(3, "100"),
            component(4, "100"), component(5, "100"), component(6, "100"));

        WeightResult result = calculator.calculateWeights(components, HALF);

        assertThat(result.getConstituents()).extracting(Constituent::getWeight)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("16.6667"), new BigDecimal("16.6667"), new BigDecimal("16.6667"),
                new BigDecimal("16.6667"), new BigDecimal("16.6667"), new BigDecimal("16.6665"));
        assertThat(result.getFindings()).isEmpty();
    }

    private static Candidate component(int securityId, String ffmcap) {
        return Candidate.builder()
            .securityId(securityId)
            .ffmcap(new BigDecimal(ffmcap))
            .build();
    }
}
