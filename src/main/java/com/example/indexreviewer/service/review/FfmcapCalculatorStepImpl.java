package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.trace.FfmcapTrace;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Calculates FFMCAP = Price(cut-off date) x Shares(review date) x Free Float(review date) for every eligible universe
 * member on the review date. Members without complete data are not eligible and get a warning. After the calculation,
 * one trace record per security is built from the values used and the calculated FFMCAP. Existing index components
 * that are not in the universe get a trace record as well, so that every leaver can be explained. All findings are
 * returned with the result, and the caller decides whether the review continues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FfmcapCalculatorStepImpl implements FfmcapCalculatorStep {

    private final SecurityRecordValidator validator;

    @Override
    public FfmcapResult calculate(ReviewCommand command) {
        Set<Integer> universeIds = command.getUniverse().getOrDefault(command.getReviewDate(), Set.of());
        Set<Integer> existingComponents = Set.copyOf(command.getComposition());
        List<Security> availableSecurities = command.getSecurities();
        log.info("Starting calculation of FFMCAP for {}, cut-off: {}, review: {}, universe count {}, security count {}",
            command.getIndexType(), command.getCutoffDate(), command.getReviewDate(), universeIds.size(),
            availableSecurities.size());

        List<Finding> findings = new ArrayList<>();
        var calculationInputData = collectSecurityData(command, universeIds, existingComponents, findings);
        var candidates = calculateFfmcapForCandidates(calculationInputData);

        var traces = buildTraces(calculationInputData, candidates);

        log.info("Calculated FFMCAP for {} of {} universe members; findings {}", candidates.size(), universeIds.size(),
            findings.size());
        return FfmcapResult.builder()
            .candidates(candidates)
            .traces(traces)
            .findings(findings)
            .build();
    }

    /**
     * Validates the rows of every universe member and existing component, adds the findings and returns the values to
     * use, ordered by security id. Only rows of universe members are checked, so data of other securities, even
     * conflicting duplicates, cannot fail the review. A universe member is eligible only when its rows have no findings
     * and it has no conflicting duplicate rows. An existing component that is not in the universe has no values and is
     * not eligible, so it leaves the index.
     */
    private List<SecurityInput> collectSecurityData(ReviewCommand command, Set<Integer> universeIds,
                                                    Set<Integer> existingComponents, List<Finding> findings) {
        List<Security> allSecurities = command.getSecurities();
        List<Security> universeSecurities = allSecurities.stream()
            .filter(row -> universeIds.contains(row.getSecurityId()))
            .toList();
        var cutoffSecurities = extractSecurities(universeSecurities, command.getCutoffDate(), findings);
        var reviewSecurities = extractSecurities(universeSecurities, command.getReviewDate(), findings);
        findings.addAll(findIgnoredSecurities(allSecurities, universeIds));
        Set<Integer> conflicting = findConflictingSecurityIds(findings);

        List<SecurityInput> collectedSecurityData = new ArrayList<>();
        for (Integer securityId : collectSecurityIds(universeIds, existingComponents)) {
            if (universeIds.contains(securityId)) {
                Security cutoffRow = cutoffSecurities.get(securityId);
                Security reviewRow = reviewSecurities.get(securityId);

                List<Finding> rowFindings = validateRows(securityId, cutoffRow, reviewRow);
                findings.addAll(rowFindings);
                boolean eligible = rowFindings.isEmpty() && !conflicting.contains(securityId);

                collectedSecurityData.add(SecurityInput.of(securityId, existingComponents.contains(securityId),
                    cutoffRow, reviewRow, eligible));
            } else {
                findings.add(buildNotInUniverseFinding(securityId, command.getReviewDate()));
                collectedSecurityData.add(SecurityInput.notInUniverse(securityId));
            }
        }
        return collectedSecurityData;
    }

    private List<Finding> validateRows(Integer securityId, Security cutoffRow, Security reviewRow) {
        List<Finding> rowFindings = new ArrayList<>(validator.validateCutoff(securityId, cutoffRow));
        rowFindings.addAll(validator.validateReview(securityId, reviewRow));
        return rowFindings;
    }

    private Finding buildNotInUniverseFinding(Integer securityId, LocalDate reviewDate) {
        return Finding.builder()
            .code(FindingCode.EXISTING_COMPONENT_NOT_IN_UNIVERSE)
            .securityId(securityId)
            .message("Security %s is an existing index component but not in the universe on %s"
                .formatted(securityId, reviewDate))
            .build();
    }

    /**
     * Calculates the FFMCAP of every eligible security; these are the candidates the selection can rank.
     */
    private List<Candidate> calculateFfmcapForCandidates(List<SecurityInput> inputs) {
        return inputs.stream()
            .filter(SecurityInput::eligible)
            .map(input -> Candidate.builder()
                .securityId(input.securityId())
                .existingComponent(input.existingComponent())
                .ffmcap(input.price().multiply(input.shares()).multiply(input.freeFloat()))
                .build())
            .toList();
    }

    /**
     * Builds one trace record per security from the values used and, for the candidates, the calculated FFMCAP.
     */
    private List<FfmcapTrace> buildTraces(List<SecurityInput> inputs, List<Candidate> candidates) {
        var ffmcapsById = candidates.stream()
            .collect(Collectors.toMap(Candidate::getSecurityId, Candidate::getFfmcap));
        return inputs.stream()
            .map(input -> FfmcapTrace.builder()
                .securityId(input.securityId())
                .existingComponent(input.existingComponent())
                .price(input.price())
                .shares(input.shares())
                .freeFloat(input.freeFloat())
                .ffmcap(ffmcapsById.get(input.securityId()))
                .build())
            .toList();
    }

    /**
     * Universe members and existing components, ordered by security id, so every leaver gets a trace record.
     */
    private List<Integer> collectSecurityIds(Set<Integer> universeIds, Set<Integer> existingComponents) {
        return Stream.concat(universeIds.stream(), existingComponents.stream())
            .distinct()
            .sorted()
            .toList();
    }

    private Set<Integer> findConflictingSecurityIds(List<Finding> findings) {
        return findings.stream()
            .filter(finding -> finding.getCode() == FindingCode.CONFLICTING_DUPLICATE)
            .map(Finding::getSecurityId)
            .collect(Collectors.toSet());
    }

    private Map<Integer, Security> extractSecurities(List<Security> rows, LocalDate date,
                                                     List<Finding> findings) {
        return rows.stream()
            .filter(row -> date.equals(row.getDate()))
            .collect(Collectors.toMap(Security::getSecurityId, Function.identity(),
                (first, second) -> {
                    findings.add(validator.checkDuplicate(first, second, date));
                    return first;
                }));
    }

    /**
     * Returns one info when the security data has securities that are not in the universe, otherwise no finding.
     */
    private List<Finding> findIgnoredSecurities(List<Security> rows, Set<Integer> universeIds) {
        long ignored = rows.stream()
            .map(Security::getSecurityId)
            .filter(securityId -> !universeIds.contains(securityId))
            .distinct()
            .count();
        if (ignored == 0) {
            return List.of();
        }
        return List.of(Finding.builder()
            .code(FindingCode.IGNORED_SECURITIES)
            .field("securityData")
            .message("%d securities in the security data are not in the universe and were ignored"
                .formatted(ignored))
            .build());
    }

    /**
     * Values of one security for the calculation: the cut-off price, the review date shares and free float, and
     * whether they can be used.
     */
    private record SecurityInput(Integer securityId, boolean existingComponent, BigDecimal price, BigDecimal shares,
                                 BigDecimal freeFloat, boolean eligible) {

        static SecurityInput of(Integer securityId, boolean existingComponent, Security cutoffRow, Security reviewRow,
                                boolean eligible) {
            return new SecurityInput(securityId, existingComponent,
                cutoffRow == null ? null : cutoffRow.getPrice(),
                reviewRow == null ? null : reviewRow.getShares(),
                reviewRow == null ? null : reviewRow.getFreeFloat(),
                eligible);
        }

        static SecurityInput notInUniverse(Integer securityId) {
            return new SecurityInput(securityId, true, null, null, null, false);
        }
    }
}
