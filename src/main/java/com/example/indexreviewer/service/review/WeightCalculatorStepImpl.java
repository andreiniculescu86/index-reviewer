package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.WeightResult;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.report.Constituent;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import com.example.indexreviewer.model.trace.WeightTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Weights the index components by FFMCAP and limits every weight to the cap (rulebook section 5.12.4). The weight a
 * capped component loses goes to the components below the cap in proportion to their FFMCAP, in rounds until no
 * weight exceeds the cap. Weights are percentages of the index, 18 for 18%, with 4 decimals. They are rounded with the
 * largest remainder method, so every weight stays within 0.0001 of its exact value and the weights add up to exactly
 * 100. The capping factor is 1 for a component that is not capped and below 1 for a capped one, with 10 decimals, so
 * that FFMCAP x capping factor reproduces the weights. The rounds, the rounding and the capping factor
 * formula are assumptions, described in docs/methodology.md.
 */
@Slf4j
@Component
public class WeightCalculatorStepImpl implements WeightCalculatorStep {

    private static final BigDecimal FULL_INDEX_WEIGHT = BigDecimal.valueOf(100);
    private static final BigDecimal WEIGHT_UNIT = new BigDecimal("0.0001");
    private static final int EXACT_DECIMALS = 20;
    private static final int CAPPING_FACTOR_DECIMALS = 10;

    /**
     * Of the index rules, only the cap percentage is used.
     */
    @Override
    public WeightResult calculateWeights(List<Candidate> selectedComponents, IndexRules rules) {
        log.info("Calculating weights for components: {}",
            selectedComponents.stream().map(Candidate::getSecurityId).toList());
        BigDecimal capPercentage = rules.getCapPercentage();

        Set<Integer> cappedSecurityIds = findCappedComponents(selectedComponents, capPercentage);
        List<BigDecimal> exactWeights = calculateExactWeights(selectedComponents, cappedSecurityIds, capPercentage);
        List<BigDecimal> roundedWeights = roundToFullIndex(exactWeights);
        var weightedConstituents = buildConstituents(selectedComponents, cappedSecurityIds, capPercentage,
            roundedWeights);

        log.info("Weighted {} components, {} capped at {}%", weightedConstituents.size(), cappedSecurityIds.size(),
            capPercentage);
        return WeightResult.builder()
            .constituents(weightedConstituents)
            .traces(getWeightTraces(weightedConstituents))
            .findings(checkWeights(selectedComponents, weightedConstituents, capPercentage))
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
                .filter(component -> divideExactly(remainingWeight.multiply(component.getFfmcap()),
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
     * proportion to their FFMCAP, before rounding.
     */
    private static List<BigDecimal> calculateExactWeights(List<Candidate> selectedComponents,
                                                          Set<Integer> cappedSecurityIds, BigDecimal capPercentage) {
        BigDecimal remainingWeight = calculateRemainingWeight(capPercentage, cappedSecurityIds.size());
        BigDecimal uncappedTotalFfmcap = sumUncappedFfmcap(selectedComponents, cappedSecurityIds);
        return selectedComponents.stream()
            .map(component -> cappedSecurityIds.contains(component.getSecurityId())
                ? capPercentage
                : divideExactly(remainingWeight.multiply(component.getFfmcap()), uncappedTotalFfmcap))
            .toList();
    }

    /**
     * Rounds the weights with the largest remainder method: every weight is rounded down to 4 decimals, then the 0.0001
     * units still missing to 100 go one each to the weights with the largest rounding remainders, the higher rank first
     * on equal remainders. When the weights cannot reach 100, because the cap is too low for the number of components,
     * they are only rounded down and the check reports it.
     */
    private static List<BigDecimal> roundToFullIndex(List<BigDecimal> exactWeights) {
        List<BigDecimal> roundedWeights = new ArrayList<>(exactWeights.stream()
            .map(weight -> weight.setScale(WEIGHT_UNIT.scale(), RoundingMode.DOWN))
            .toList());
        int missingUnits = FULL_INDEX_WEIGHT.subtract(sumOf(roundedWeights)).divideToIntegralValue(WEIGHT_UNIT)
            .intValue();
        List<Integer> raisedIndexes = IntStream.range(0, exactWeights.size())
            .filter(index -> exactWeights.get(index).compareTo(roundedWeights.get(index)) > 0)
            .boxed()
            .sorted(Comparator.comparing(
                (Integer index) -> exactWeights.get(index).subtract(roundedWeights.get(index))).reversed())
            .limit(Math.max(missingUnits, 0))
            .toList();
        raisedIndexes.forEach(index -> roundedWeights.set(index, roundedWeights.get(index).add(WEIGHT_UNIT)));
        log.debug("Rounding gave 0.0001 to {} of {} weights", raisedIndexes.size(), exactWeights.size());
        return List.copyOf(roundedWeights);
    }

    /**
     * The capping factor of a capped component is the factor that brings its FFMCAP to the cap when the other
     * components keep their full FFMCAP.
     */
    private static List<Constituent> buildConstituents(List<Candidate> selectedComponents,
                                                       Set<Integer> cappedSecurityIds, BigDecimal capPercentage,
                                                       List<BigDecimal> roundedWeights) {
        BigDecimal remainingWeight = calculateRemainingWeight(capPercentage, cappedSecurityIds.size());
        BigDecimal uncappedTotalFfmcap = sumUncappedFfmcap(selectedComponents, cappedSecurityIds);
        return IntStream.range(0, selectedComponents.size())
            .mapToObj(index -> {
                Candidate component = selectedComponents.get(index);
                return Constituent.builder()
                    .securityId(component.getSecurityId())
                    .weight(roundedWeights.get(index))
                    .cappingFactor(cappedSecurityIds.contains(component.getSecurityId())
                        ? capPercentage.multiply(uncappedTotalFfmcap).divide(
                        remainingWeight.multiply(component.getFfmcap()), CAPPING_FACTOR_DECIMALS, RoundingMode.HALF_UP)
                        : BigDecimal.ONE.setScale(CAPPING_FACTOR_DECIMALS))
                    .build();
            })
            .toList();
    }

    /**
     * Sanity check of the final weights. Returns an {@code INVALID_WEIGHTS} error for each broken rule: a weight above
     * the cap, a negative weight, weights that do not add up to 100, or capping factors that do not reproduce the
     * weights within 0.0001. A weight above the cap or a total below 100 happens when the number of components x the
     * cap is below 100, which the index configuration rejects at startup.
     */
    private static List<Finding> checkWeights(List<Candidate> selectedComponents, List<Constituent> constituents,
                                              BigDecimal capPercentage) {
        List<Finding> findings = new ArrayList<>();
        List<BigDecimal> weights = constituents.stream().map(Constituent::getWeight).toList();
        BigDecimal largestWeight = weights.stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
        if (largestWeight.compareTo(capPercentage) > 0) {
            findings.add(invalidWeights("The largest weight is %s%%; none may exceed %s%%"
                .formatted(largestWeight.toPlainString(), capPercentage.toPlainString())));
        }
        BigDecimal smallestWeight = weights.stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
        if (smallestWeight.signum() < 0) {
            findings.add(invalidWeights("The smallest weight is %s%%; no weight may be negative"
                .formatted(smallestWeight.toPlainString())));
        }
        BigDecimal totalWeight = sumOf(weights);
        if (totalWeight.compareTo(FULL_INDEX_WEIGHT) != 0) {
            findings.add(invalidWeights("The weights add up to %s%% instead of 100%%"
                .formatted(totalWeight.toPlainString())));
        }
        List<Integer> unreproducedIds = findUnreproducedWeights(selectedComponents, constituents);
        if (!unreproducedIds.isEmpty()) {
            findings.add(invalidWeights("FFMCAP x capping factor does not reproduce the weights of securities %s"
                .formatted(unreproducedIds)));
        }
        return findings;
    }

    /**
     * Returns the ids whose weight, calculated again from FFMCAP x capping factor, differs from the rounded weight by
     * 0.0001 or more.
     */
    private static List<Integer> findUnreproducedWeights(List<Candidate> selectedComponents,
                                                         List<Constituent> constituents) {
        List<BigDecimal> cappedFfmcaps = IntStream.range(0, selectedComponents.size())
            .mapToObj(index -> selectedComponents.get(index).getFfmcap()
                .multiply(constituents.get(index).getCappingFactor()))
            .toList();
        BigDecimal cappedTotalFfmcap = sumOf(cappedFfmcaps);
        if (cappedTotalFfmcap.signum() == 0) {
            return List.of();
        }
        return IntStream.range(0, constituents.size())
            .filter(index -> divideExactly(FULL_INDEX_WEIGHT.multiply(cappedFfmcaps.get(index)), cappedTotalFfmcap)
                .subtract(constituents.get(index).getWeight()).abs().compareTo(WEIGHT_UNIT) >= 0)
            .mapToObj(index -> constituents.get(index).getSecurityId())
            .toList();
    }

    private static Finding invalidWeights(String message) {
        return Finding.builder()
            .code(FindingCode.INVALID_WEIGHTS)
            .field("weight")
            .message(message)
            .build();
    }

    private static BigDecimal divideExactly(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, EXACT_DECIMALS, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateRemainingWeight(BigDecimal capPercentage, int cappedCount) {
        return FULL_INDEX_WEIGHT.subtract(capPercentage.multiply(BigDecimal.valueOf(cappedCount)));
    }

    private static BigDecimal sumUncappedFfmcap(List<Candidate> selectedComponents, Set<Integer> cappedSecurityIds) {
        return sumFfmcap(selectedComponents.stream()
            .filter(component -> !cappedSecurityIds.contains(component.getSecurityId()))
            .toList());
    }

    private static BigDecimal sumFfmcap(List<Candidate> componentsToSum) {
        return sumOf(componentsToSum.stream().map(Candidate::getFfmcap).toList());
    }

    private static BigDecimal sumOf(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
