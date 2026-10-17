package com.example.indexreviewer.service.review;

import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.RowValidation;
import com.example.indexreviewer.model.SecurityRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that the uploaded data contains the dates of the review. A missing date is an error, because no security
 * could be reviewed.
 */
@Component
public class ReviewDataValidator {

    public RowValidation validateDates(ReviewCommand command) {
        Set<LocalDate> universeDates = command.universe().entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        Set<LocalDate> securityDates = command.securityRecords().stream()
            .map(SecurityRecord::date)
            .collect(Collectors.toSet());

        List<String> errors = new ArrayList<>();
        requireDate("Universe", universeDates, "review", command.reviewDate(), errors);
        requireDate("Security data", securityDates, "cut-off", command.cutoffDate(), errors);
        requireDate("Security data", securityDates, "review", command.reviewDate(), errors);
        return new RowValidation(errors, List.of());
    }

    private static void requireDate(String data, Set<LocalDate> dates, String dateName, LocalDate date,
                                    List<String> errors) {
        if (!dates.contains(date)) {
            errors.add("%s has no rows for the %s date %s; available dates: %s"
                .formatted(data, dateName, date, dates.stream().sorted().toList()));
        }
    }
}
