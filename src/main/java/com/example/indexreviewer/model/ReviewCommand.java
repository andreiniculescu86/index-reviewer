package com.example.indexreviewer.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Input of one review. The universe maps each date to the security ids that are members on that date. The
 * composition lists the existing index components as read, including duplicates, so that they can be validated.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewCommand {

    IndexType indexType;
    LocalDate cutoffDate;
    LocalDate reviewDate;
    Map<LocalDate, Set<Integer>> universe;
    List<Security> securities;
    List<Integer> composition;
}
