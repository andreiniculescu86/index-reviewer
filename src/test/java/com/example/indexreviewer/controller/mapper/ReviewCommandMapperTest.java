package com.example.indexreviewer.controller.mapper;

import com.example.indexreviewer.dto.csv.Composition;
import com.example.indexreviewer.dto.csv.SecurityData;
import com.example.indexreviewer.dto.csv.Universe;
import com.example.indexreviewer.dto.request.ConfigRequest;
import com.example.indexreviewer.model.IndexType;
import com.example.indexreviewer.model.ReviewCommand;
import com.example.indexreviewer.model.Security;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCommandMapperTest {

    private static final LocalDate CUTOFF_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 9, 21);

    private final ReviewCommandMapper mapper = new ReviewCommandMapperImpl();

    @Test
    void map_whenRequestGiven_expectRequestFields() {
        ConfigRequest request = ConfigRequest.builder()
            .indexType(IndexType.SMI)
            .cutoffDate(CUTOFF_DATE)
            .reviewDate(REVIEW_DATE)
            .build();

        ReviewCommand command = mapper.toServiceModel(request, List.of(), List.of(), List.of());

        assertThat(command.getIndexType()).isEqualTo(IndexType.SMI);
        assertThat(command.getCutoffDate()).isEqualTo(CUTOFF_DATE);
        assertThat(command.getReviewDate()).isEqualTo(REVIEW_DATE);
        assertThat(command.getUniverse()).isEmpty();
        assertThat(command.getSecurities()).isEmpty();
        assertThat(command.getComposition()).isEmpty();
    }

    @Test
    void map_whenUniverseOnTwoDates_expectGroupedByDate() {
        List<Universe> universe = List.of(
            universe(REVIEW_DATE, 10), universe(REVIEW_DATE, 166), universe(CUTOFF_DATE, 10));

        ReviewCommand command = mapper.toServiceModel(new ConfigRequest(), universe, List.of(), List.of());

        assertThat(command.getUniverse()).containsOnlyKeys(REVIEW_DATE, CUTOFF_DATE);
        assertThat(command.getUniverse().get(REVIEW_DATE)).containsExactlyInAnyOrder(10, 166);
        assertThat(command.getUniverse().get(CUTOFF_DATE)).containsExactly(10);
    }

    @Test
    void map_whenUniverseIdRepeated_expectOnce() {
        List<Universe> universe = List.of(universe(REVIEW_DATE, 10), universe(REVIEW_DATE, 10));

        ReviewCommand command = mapper.toServiceModel(new ConfigRequest(), universe, List.of(), List.of());

        assertThat(command.getUniverse().get(REVIEW_DATE)).containsExactly(10);
    }

    @Test
    void map_whenComposition_expectIdsAsRead() {
        List<Composition> composition = List.of(composition(155), composition(205), composition(155));

        ReviewCommand command = mapper.toServiceModel(new ConfigRequest(), List.of(), List.of(), composition);

        assertThat(command.getComposition()).containsExactly(155, 205, 155);
    }

    @Test
    void map_whenSecurityData_expectAllFields() {
        SecurityData cutoffRow = securityData(101, CUTOFF_DATE, "311.5", "0.8048", "5835000");
        SecurityData reviewRow = securityData(101, REVIEW_DATE, null, "0.8476", "5835000");

        ReviewCommand command = mapper.toServiceModel(new ConfigRequest(), List.of(),
            List.of(cutoffRow, reviewRow), List.of());

        assertThat(command.getSecurities()).containsExactly(
            Security.builder()
                .securityId(101)
                .date(CUTOFF_DATE)
                .price(new BigDecimal("311.5"))
                .freeFloat(new BigDecimal("0.8048"))
                .shares(new BigDecimal("5835000"))
                .build(),
            Security.builder()
                .securityId(101)
                .date(REVIEW_DATE)
                .freeFloat(new BigDecimal("0.8476"))
                .shares(new BigDecimal("5835000"))
                .build());
    }

    private static Universe universe(LocalDate date, int id) {
        Universe row = new Universe();
        row.setDate(date);
        row.setId(id);
        return row;
    }

    private static Composition composition(int id) {
        Composition row = new Composition();
        row.setId(id);
        return row;
    }

    private static SecurityData securityData(int id, LocalDate date, String price, String freeFloat,
                                             String shares) {
        SecurityData row = new SecurityData();
        row.setId(id);
        row.setDate(date);
        row.setPrice(price == null ? null : new BigDecimal(price));
        row.setFreeFloat(new BigDecimal(freeFloat));
        row.setShares(new BigDecimal(shares));
        return row;
    }
}
