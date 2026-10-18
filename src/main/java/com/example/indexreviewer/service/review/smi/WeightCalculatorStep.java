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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Weights the index components by FFMCAP and limits every weight to the cap (rulebook section 5.12.4). The weight a
 * capped component loses goes to the components below the cap in proportion to their FFMCAP, in rounds until no
 * weight exceeds the cap. Weights are percentages of the index, 18 for 18%. Every division is rounded to 4 decimals,
 * and what the rounding leaves short of or above 100 goes to the last component, so that the weights add up to exactly
 * 100. The capping factor is 1 for a component that is not capped and below 1 for a capped one. The rounds, the
 * rounding and the capping factor formula are assumptions, described in docs/methodology.md.
 */
@Slf4j
@Component
public class WeightCalculatorStep {

    private static final BigDecimal FULL_INDEX_WEIGHT = BigDecimal.valueOf(100);
    private static final int WEIGHT_DECIMALS = 4;

    /**
     * Returns the components in the given order with their weighting factors, the trace records and the findings of
     * the weighting.
     */
    public WeightResult calculateWeights(List<Candidate> selectedComponents, BigDecimal capPercentage) {
        log.info("Calculating weights for components: {}",
            selectedComponents.stream().map(Candidate::getSecurityId).toList());

        Set<Integer> cappedSecurityIds = findCappedComponents(selectedComponents, capPercentage);

        var intermediateConstituents = weighComponents(selectedComponents, cappedSecurityIds, capPercentage);
        var weightedConstituents = addLeftoverToLast(intermediateConstituents);

        log.info("Weighted {} components, {} capped at {}%", weightedConstituents.size(), cappedSecurityIds.size(),
            capPercentage);
        return WeightResult.builder()
            .constituents(weightedConstituents)
            .traces(getWeightTraces(weightedConstituents))
            .findings(checkWeights(weightedConstituents, capPercentage))
            .build();
    }

    private static List<WeightTrace> getWeightTraces(List<Constituent> weightedConstituents) {
        return weightedConstituents.stream()
            .map(constituent -> WeightTrace.builder()
                .securityId(constituent.getSecurityId())
                .cappingFactor(constituent.getCappingFactor())
                .weight(constituent.getWeight())
                .build())
            .toList();
    }

    /**
     * Calculates which ids must be capped. Each round gives the weight that the capped components leave to the other
     * components, in proportion to their FFMCAP, and caps every component that then exceeds the cap. A component below
     * the cap at first can exceed it once it receives weight.
     */
    private static Set<Integer> findCappedComponents(List<Candidate> selectedComponents, BigDecimal capPercentage) {
        Set<Integer> cappedSecurityIds = new HashSet<>();
        List<Integer> newlyCappedIds;
        int roundNumber = 0;
        do {
            BigDecimal remainingWeight = calculateRemainingWeight(capPercentage, cappedSecurityIds.size());
            List<Candidate> uncappedComponents = selectedComponents.stream()
                .filter(component -> !cappedSecurityIds.contains(component.getSecurityId()))
                .toList();
            BigDecimal uncappedTotalFfmcap = sumFfmcap(uncappedComponents);
            newlyCappedIds = uncappedComponents.stream()
                .filter(component -> divideToWeightDecimals(remainingWeight.multiply(component.getFfmcap()),
                    uncappedTotalFfmcap).compareTo(capPercentage) > 0)
                .map(Candidate::getSecurityId)
                .toList();
            cappedSecurityIds.addAll(newlyCappedIds);
            if (!newlyCappedIds.isEmpty()) {
                roundNumber++;
                log.debug("Capping round {} capped {}", roundNumber, newlyCappedIds);
            }
        } while (!newlyCappedIds.isEmpty());

        log.debug("Components to be capped {}", cappedSecurityIds);
        return cappedSecurityIds;
    }

    /**
     * Gives every capped component exactly the cap and shares the remaining weight among the other components in
     * proportion to their FFMCAP. The capping factor of a capped component is the factor that brings its FFMCAP to the
     * cap when the other components keep their full FFMCAP.
     */
    private static List<Constituent> weighComponents(List<Candidate> selectedComponents,
                                                     Set<Integer> cappedSecurityIds, BigDecimal capPercentage) {
        BigDecimal remainingWeight = calculateRemainingWeight(capPercentage, cappedSecurityIds.size());
        BigDecimal uncappedTotalFfmcap = sumFfmcap(selectedComponents.stream()
            .filter(component -> !cappedSecurityIds.contains(component.getSecurityId()))
            .toList());

        return selectedComponents.stream()
            .map(component -> {
                boolean isComponentCapped = cappedSecurityIds.contains(component.getSecurityId());
                return Constituent.builder()
                    .securityId(component.getSecurityId())
                    .weight(isComponentCapped ? capPercentage.setScale(WEIGHT_DECIMALS, RoundingMode.DOWN)
                        : divideToWeightDecimals(remainingWeight.multiply(component.getFfmcap()), uncappedTotalFfmcap))
                    .cappingFactor(isComponentCapped
                        ? divideToWeightDecimals(capPercentage.multiply(uncappedTotalFfmcap),
                        remainingWeight.multiply(component.getFfmcap()))
                        : BigDecimal.ONE)
                    .build();
            })
            .toList();
    }

    /**
     * Adds what the rounding left short of 100, or subtracts what it left above, to the weight of the last component,
     * which has the lowest rank. The difference is at most a few units of 0.0001.
     */
    private static List<Constituent> addLeftoverToLast(List<Constituent> roundedConstituents) {
        BigDecimal leftoverWeight = FULL_INDEX_WEIGHT.subtract(sumWeights(roundedConstituents));
        if (leftoverWeight.signum() == 0) {
            return roundedConstituents;
        }
        var adjustedConstituents = new ArrayList<>(roundedConstituents);
        var lastConstituent = adjustedConstituents.getLast();
        adjustedConstituents.set(adjustedConstituents.size() - 1, Constituent.builder()
            .securityId(lastConstituent.getSecurityId())
            .weight(lastConstituent.getWeight().add(leftoverWeight))
            .cappingFactor(lastConstituent.getCappingFactor())
            .build());
        log.debug("Added the rounding leftover of {} to security {}", leftoverWeight,
            lastConstituent.getSecurityId());
        return List.copyOf(adjustedConstituents);
    }

    /**
     * Sanity check. Returns an {@code INVALID_WEIGHTS} error when a weight exceeds the cap, otherwise no finding. This
     * happens when
     * number of components x cap is below 100, which the SMI configuration rejects at startup.
     */
    private static List<Finding> checkWeights(List<Constituent> weightedConstituents, BigDecimal capPercentage) {
        BigDecimal largestWeight = weightedConstituents.stream()
            .map(Constituent::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::max);
        if (largestWeight.compareTo(capPercentage) <= 0) {
            return List.of();
        }
        return List.of(Finding.builder()
            .code(FindingCode.INVALID_WEIGHTS)
            .field("weight")
            .message("The largest weight is %s%%; none may exceed %s%%".formatted(largestWeight.toPlainString(),
                capPercentage.toPlainString()))
            .build());
    }

    private static BigDecimal divideToWeightDecimals(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, WEIGHT_DECIMALS, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateRemainingWeight(BigDecimal capPercentage, int cappedCount) {
        return FULL_INDEX_WEIGHT.subtract(capPercentage.multiply(BigDecimal.valueOf(cappedCount)));
    }

    private static BigDecimal sumFfmcap(List<Candidate> componentsToSum) {
        return componentsToSum.stream()
            .map(Candidate::getFfmcap)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumWeights(List<Constituent> constituents) {
        return constituents.stream()
            .map(Constituent::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
