package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.FfmcapResult;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.RowValidation;
import com.example.indexreviewer.model.SecurityRecord;
import com.example.indexreviewer.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calculates FFMCAP = Price(cut-off date) x Shares(review date) x Free Float(review date) for every universe member
 * on the review date. Members without complete data are skipped with a warning. All errors and warnings are returned
 * with the result, and the caller decides whether the review continues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FfmcapCalculator {

    private final SecurityRecordValidator validator;

    /**
     * Returns the FFMCAP per security id, ordered by security id, with the validation result of the data used.
     */
    public FfmcapResult calculate(ReviewCommand command) {
        Set<Integer> universeIds = command.universe().getOrDefault(command.reviewDate(), Set.of());
        List<SecurityRecord> rows = command.securityRecords();
        log.info("Starting calculation of FFMCAP for {}, cut-off: {}, review: {}, universe count {}, security count {}",
            command.indexType(), command.cutoffDate(), command.reviewDate(), universeIds.size(), rows.size());

        var ffmcapValidation = new ValidationResult();
        var cutoffSecurities = extractSecurities(rows, command.cutoffDate(), ffmcapValidation);
        var reviewSecurities = extractSecurities(rows, command.reviewDate(), ffmcapValidation);
        addIgnoredSecuritiesToValidation(rows, universeIds, ffmcapValidation);

        Map<Integer, BigDecimal> ffmcaps = new LinkedHashMap<>();
        for (Integer securityId : universeIds.stream().sorted().toList()) {
            SecurityRecord cutoffRow = cutoffSecurities.get(securityId);
            SecurityRecord reviewRow = reviewSecurities.get(securityId);
            if (validateRows(securityId, cutoffRow, reviewRow, ffmcapValidation)) {
                ffmcaps.put(securityId, calculateFfmcap(cutoffRow, reviewRow));
            }
        }
        log.info("Calculated FFMCAP for {} of {} universe members; errors {}, warnings {}", ffmcaps.size(),
            universeIds.size(), ffmcapValidation.getErrors().size(), ffmcapValidation.getWarnings().size());

        return new FfmcapResult(ffmcaps, ffmcapValidation);
    }

    /**
     * Validates both rows of a security, adds the result to the review validation and returns whether the
     * FFMCAP can be calculated.
     */
    private boolean validateRows(Integer securityId, SecurityRecord cutoffRow, SecurityRecord reviewRow,
                                 ValidationResult validationResult) {
        RowValidation cutoffValidation = validator.validateCutoff(securityId, cutoffRow);
        RowValidation reviewValidation = validator.validateReview(securityId, reviewRow);
        validationResult.add(cutoffValidation);
        validationResult.add(reviewValidation);
        return cutoffValidation.hasNoIssues() && reviewValidation.hasNoIssues();
    }


    private static BigDecimal calculateFfmcap(SecurityRecord cutoffRow, SecurityRecord reviewRow) {
        return cutoffRow.price().multiply(reviewRow.shares()).multiply(reviewRow.freeFloat());
    }

    private Map<Integer, SecurityRecord> extractSecurities(List<SecurityRecord> rows, LocalDate date,
                                                          ValidationResult validation) {
        return rows.stream()
            .filter(row -> date.equals(row.date()))
            .collect(Collectors.toMap(SecurityRecord::securityId, Function.identity(),
                (first, second) -> {
                    validation.add(validator.checkDuplicate(first, second, date));
                    return first;
                }));
    }

    private static void addIgnoredSecuritiesToValidation(List<SecurityRecord> rows, Set<Integer> universeIds,
                                                         ValidationResult validationResult) {
        long ignored = rows.stream()
            .map(SecurityRecord::securityId)
            .filter(securityId -> !universeIds.contains(securityId))
            .distinct()
            .count();
        if (ignored > 0) {
            validationResult.addInfo(
                "%d securities in the security data are not in the universe and were ignored".formatted(ignored));
        }
    }
}
