package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.Candidate;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import com.example.indexreviewer.model.audit.IndexRules;
import com.example.indexreviewer.model.trace.Finding;
import com.example.indexreviewer.model.trace.FindingCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs the review-level checks: the uploaded data contains the dates of the review, the composition has the size of
 * the index and enough candidates can be ranked. A failed check is an error, because no valid index could be selected.
 * Each check returns its findings, and the caller stores them in the review trace.
 */
@Component
public class ReviewDataValidator {

    /**
     * Checks the input before any security is reviewed: the review dates and the size of the composition.
     */
    public List<Finding> validateComposition(ReviewCommand command, IndexRules rules) {
        List<Finding> findings = new ArrayList<>(validateDates(command));
        long members = command.getComposition().stream().distinct().count();
        if (members != rules.getNumberOfComponents()) {
            findings.add(Finding.builder()
                .code(FindingCode.COMPOSITION_SIZE)
                .field("composition")
                .message("Composition has %d securities but the %s needs %d"
                    .formatted(members, command.getIndexType(), rules.getNumberOfComponents()))
                .build());
        }
        return findings;
    }

    /**
     * Checks that the FFMCAP step left enough candidates to fill the index.
     */
    public List<Finding> validateCandidates(ReviewCommand command, List<Candidate> candidates, IndexRules rules) {
        if (candidates.size() >= rules.getNumberOfComponents()) {
            return List.of();
        }
        return List.of(Finding.builder()
            .code(FindingCode.TOO_FEW_ELIGIBLE)
            .message("Only %d securities have a valid FFMCAP but the %s needs %d"
                .formatted(candidates.size(), command.getIndexType(), rules.getNumberOfComponents()))
            .build());
    }

    public List<Finding> validateDates(ReviewCommand command) {
        var universeDates = command.getUniverse().entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        var securityDates = command.getSecurities().stream()
            .map(Security::getDate)
            .collect(Collectors.toSet());

        List<Finding> findings = new ArrayList<>();
        requireDate("universe", "Universe", universeDates, "review", command.getReviewDate(), findings);
        requireDate("securityData", "Security data", securityDates, "cut-off", command.getCutoffDate(), findings);
        requireDate("securityData", "Security data", securityDates, "review", command.getReviewDate(), findings);
        return findings;
    }

    private static void requireDate(String field, String data, Set<LocalDate> dates, String dateName, LocalDate date,
                                    List<Finding> findings) {
        if (!dates.contains(date)) {
            findings.add(Finding.builder()
                .code(FindingCode.MISSING_DATE)
                .field(field)
                .message("%s has no rows for the %s date %s; available dates: %s"
                    .formatted(data, dateName, date, dates.stream().sorted().toList()))
                .build());
        }
    }
}
