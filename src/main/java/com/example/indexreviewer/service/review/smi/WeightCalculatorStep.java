package com.example.indexreviewer.service.review.smi;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.WeightTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Weights the index components by FFMCAP and limits every weight to the cap (rulebook section 5.12.4). The weight a
 * capped component loses goes to the components below the cap in proportion to their FFMCAP, in rounds until no
 * weight exceeds the cap. Weights are fractions of the index, 0.18 for 18%. The capping factor is 1 for a component
 * that is not capped and below 1 for a capped one, so that FFMCAP x capping factor gives the final weights. The
 * rounds and the capping factor formula are assumptions, described in docs/methodology.md.
 */
@Slf4j
@Component
public class WeightCalculatorStep {

    private static final MathContext PRECISION = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TOLERANCE = new BigDecimal("1E-12");

    /**
     * Returns the components in the given order with their weighting factors, the trace records and the findings of
     * the weighting. When the cap is too low for the weights to add up to 100%, it returns no components and a
     * {@code CAP_NOT_FEASIBLE} error.
     */
    public WeightResult calculateWeights(List<Candidate> components, BigDecimal capPercentage) {
        BigDecimal cap = capPercentage.divide(HUNDRED, PRECISION);
        if (cap.multiply(BigDecimal.valueOf(components.size())).compareTo(BigDecimal.ONE) < 0) {
            return WeightResult.builder()
                .constituents(List.of())
                .traces(List.of())
                .findings(List.of(Finding.builder()
                    .code(FindingCode.CAP_NOT_FEASIBLE)
                    .message("%d components with a cap of %s%% cannot add up to 100%%"
                        .formatted(components.size(), capPercentage.toPlainString()))
                    .build()))
                .build();
        }

        Set<Integer> capped = findCappedComponents(components, cap);
        List<Constituent> constituents = weighComponents(components, capped, cap);
        List<WeightTrace> traces = constituents.stream()
            .map(constituent -> WeightTrace.builder()
                .securityId(constituent.getSecurityId())
                .cappingFactor(constituent.getCappingFactor())
                .weight(constituent.getWeight())
                .build())
            .toList();

        log.info("Weighted {} components, {} capped at {}%", constituents.size(), capped.size(), capPercentage);
        return WeightResult.builder()
            .constituents(constituents)
            .traces(traces)
            .findings(checkWeights(constituents, cap))
            .build();
    }

    /**
     * Caps in rounds. Each round gives the weight that the capped components leave to the other components, in
     * proportion to their FFMCAP, and caps every component that then exceeds the cap. A component below the cap at
     * first can exceed it once it receives weight. The rounds stop when a round caps no further component.
     */
    private static Set<Integer> findCappedComponents(List<Candidate> components, BigDecimal cap) {
        Set<Integer> capped = new HashSet<>();
        List<Integer> newlyCapped;
        int round = 0;
        do {
            BigDecimal remainingWeight = calculateRemainingWeight(cap, capped.size());
            List<Candidate> uncapped = components.stream()
                .filter(component -> !capped.contains(component.getSecurityId()))
                .toList();
            BigDecimal uncappedFfmcap = sumFfmcap(uncapped);
            newlyCapped = uncapped.stream()
                .filter(component -> remainingWeight.multiply(component.getFfmcap())
                    .divide(uncappedFfmcap, PRECISION)
                    .compareTo(cap) > 0)
                .map(Candidate::getSecurityId)
                .toList();
            capped.addAll(newlyCapped);
            if (!newlyCapped.isEmpty()) {
                round++;
                log.debug("Capping round {} capped {}", round, newlyCapped);
            }
        } while (!newlyCapped.isEmpty());
        return capped;
    }

    /**
     * Gives every capped component exactly the cap and shares the remaining weight among the other components in
     * proportion to their FFMCAP. The capping factor of a capped component is the factor that brings its FFMCAP to the
     * cap when the other components keep their full FFMCAP.
     */
    private static List<Constituent> weighComponents(List<Candidate> components, Set<Integer> capped,
                                                     BigDecimal cap) {
        BigDecimal remainingWeight = calculateRemainingWeight(cap, capped.size());
        // Never zero: with components x cap >= 1, at least one component always stays at or below the cap
        BigDecimal uncappedFfmcap = sumFfmcap(components.stream()
            .filter(component -> !capped.contains(component.getSecurityId()))
            .toList());

        return components.stream()
            .map(component -> {
                boolean isCapped = capped.contains(component.getSecurityId());
                return Constituent.builder()
                    .securityId(component.getSecurityId())
                    .weight(isCapped ? cap
                        : remainingWeight.multiply(component.getFfmcap()).divide(uncappedFfmcap, PRECISION))
                    .cappingFactor(isCapped
                        ? cap.multiply(uncappedFfmcap)
                        .divide(remainingWeight.multiply(component.getFfmcap()), PRECISION)
                        : BigDecimal.ONE)
                    .build();
            })
            .toList();
    }

    /**
     * Returns an {@code INVALID_WEIGHTS} error when the weights do not add up to 1 or one exceeds the cap, otherwise no
     * finding. The rounds prevent both, so the error only shows a bug instead of returning a wrong index.
     */
    private static List<Finding> checkWeights(List<Constituent> constituents, BigDecimal cap) {
        BigDecimal sum = constituents.stream()
            .map(Constituent::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal largest = constituents.stream()
            .map(Constituent::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::max);
        if (sum.subtract(BigDecimal.ONE).abs().compareTo(TOLERANCE) <= 0 && largest.compareTo(cap) <= 0) {
            return List.of();
        }
        return List.of(Finding.builder()
            .code(FindingCode.INVALID_WEIGHTS)
            .field("weight")
            .message("Weights add up to %s and the largest is %s; they must add up to 1 and none may exceed %s"
                .formatted(sum.toPlainString(), largest.toPlainString(), cap.toPlainString()))
            .build());
    }

    private static BigDecimal calculateRemainingWeight(BigDecimal cap, int cappedCount) {
        return BigDecimal.ONE.subtract(cap.multiply(BigDecimal.valueOf(cappedCount)));
    }

    private static BigDecimal sumFfmcap(List<Candidate> components) {
        return components.stream()
            .map(Candidate::getFfmcap)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
