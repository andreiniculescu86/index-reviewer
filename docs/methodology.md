# Review methodology

This page describes how the application reviews the SMI: which data it uses, how it ranks and selects the components,
and how it weights them. It follows the assignment and the SIX Swiss Index Methodology Rulebook, version 3.40 of
08.06.2026 (configured as `methodology-version`). Where the rulebook does not define a case, the rule chosen here is
marked as an **assumption**.

How the input data is validated before the review is described in [Data validation](data-validation.md). How every
decision can be followed afterwards is described in [Traceability and audit](traceability-and-auditability.md).

## Configuration

The index rules are configured per index in [application.yml](../src/main/resources/application.yml).

| Property                | SMI value | Rulebook source                           |
|-------------------------|-----------|-------------------------------------------|
| `number-of-components`  | 20        | 5.12.3.2, "Number of Index Components"    |
| `direct-selection-rank` | 18        | 5.12.3.2, "Direct Selection: Rank 1-18"   |
| `buffer-rank`           | 22        | 5.12.3.2, "Buffer: Rank 19-22"            |
| `cap-percentage`        | 18        | 5.12.4, components limited to 18%         |
| `methodology-version`   | 3.40      | Rulebook version the rules are taken from |

The application does not start when `direct-selection-rank` is greater than `number-of-components` or
`number-of-components` is greater than `buffer-rank`, because the direct selection and the buffer could then not fill
the index. If a selection still does not have exactly `number-of-components` components, the selection adds a
`SELECTION_SIZE` error and the review stops with status `FAILED` instead of returning a wrong index. The ranks and
decisions of the selection stay in the trace.

The application also does not start when `number-of-components` x `cap-percentage` is below 100, because the capped
weights could then not add up to 100%. See [Weighting and capping](#weighting-and-capping).

## Dates

| Date    | Q3 2026    | Used for                                          |
|---------|------------|---------------------------------------------------|
| Cut-off | 2026-09-10 | Price                                             |
| Review  | 2026-09-21 | Universe membership, number of shares, free float |

The dates match the rulebook schedule for September 2026. The capping data cut-off is the Thursday 8 days before the
implementation on the third Friday, 18 September (section 4.2). The review date is the Monday after implementation,
when the new shares and free float become effective (section 3.1).

## Eligibility

A security is a candidate when it is in the SPI universe on the review date (section 5.12.3.2, the index universe of
the SMI is the SPI) and has complete and valid data for both dates. Securities without complete data are excluded
with a warning; securities with invalid data fail the review. See [Data validation](data-validation.md).

Universe membership is taken as given; the SPI free float threshold of 20% (section 5.2.3.2) is not re-checked (because
requirements did not specify it nor did it reference that section).

## Free float market capitalization

```
FFMCAP = Price(cut-off date) x Shares(review date) x Free Float(review date)
```

The product is calculated exactly with `BigDecimal`, without rounding.

## Ranking

Candidates are ranked by FFMCAP, highest first.

**Equal FFMCAP (assumption).** The rulebook has no rule for equal values. Its governance section treats a missing rule
as an "insufficient rule" that SIX decides through an escalation process, guided by the basic principles of section
1.3, among them stability ("High index continuity"). Equal FFMCAPs are therefore ordered:

1. Existing components first, so that a tie keeps the current members in the index.
2. Existing components among themselves in the order they appear in the current composition file.
3. Newcomers among themselves by the lower security id, so that the order is always the same.

The order of the current composition is used as given in the input file. The application does not check what the order
is based on.

When candidates with the same FFMCAP end up partly selected and partly not, the tie order and not the FFMCAP decided
the composition. The review then adds a `SELECTION_TIE` warning that names both groups, so that a person can check the
result. A tie that does not split the selection, for example inside ranks 1 to 18, adds no warning.

## Selection with the buffer

The selection follows section 5.12.3.2:

1. Ranks 1 to 18 are selected directly.
2. The remaining 2 seats are filled from the buffer, ranks 19 to 22. Existing components in the buffer are selected
   first, in rank order.
3. Seats that are still open go to the newcomers in the buffer, in rank order.
4. Candidates ranked 23 or lower are never selected. An existing component there leaves the index.

Each candidate gets one decision in the trace: `SELECTED_DIRECT`, `SELECTED_BUFFER_EXISTING_COMPONENT`,
`SELECTED_BUFFER_NEWCOMER`, `NOT_SELECTED_BUFFER` or `NOT_SELECTED_BELOW_BUFFER`.

**Example with a tie at the end of the buffer.** Rank 21 has an FFMCAP of 9. Three candidates follow with an FFMCAP
of 8: one newcomer and two existing components. Because existing components come first in a tie, the two members take
ranks 22 and 23 and the newcomer takes rank 24. The member on rank 22 is inside the buffer and keeps its seat. The
member on rank 23 is below the buffer and leaves. As many members as fit into the buffer are kept, ahead of newcomers
with the same FFMCAP. The review adds a `SELECTION_TIE` warning.

**Example with tied existing components.** Ranks 19 and 20 are newcomers with an FFMCAP of 11 and 10. Securities 21
to 30 all have an FFMCAP of 8, and 25, 28 and 30 are existing components. The current composition lists them in the
order 30, 25, 28.

| Rank | Security | FFMCAP | Existing component | Decision                             |
|------|----------|--------|--------------------|--------------------------------------|
| 19   | 19       | 11     | no                 | `NOT_SELECTED_BUFFER`                |
| 20   | 20       | 10     | no                 | `NOT_SELECTED_BUFFER`                |
| 21   | 30       | 8      | yes                | `SELECTED_BUFFER_EXISTING_COMPONENT` |
| 22   | 25       | 8      | yes                | `SELECTED_BUFFER_EXISTING_COMPONENT` |
| 23   | 28       | 8      | yes                | `NOT_SELECTED_BELOW_BUFFER`          |
| 24   | 21       | 8      | no                 | `NOT_SELECTED_BELOW_BUFFER`          |
| 25   | 22       | 8      | no                 | `NOT_SELECTED_BELOW_BUFFER`          |

Ranks 26 to 30 continue with the newcomers 23, 24, 26, 27 and 29, none selected. The existing components fill the two
buffer seats in composition order, so 30 and 25 stay and 28 leaves. The newcomers 19 and 20 lose their seats although
their FFMCAP is higher, because existing components in the buffer come first. The review adds a `SELECTION_TIE`
warning.

**Result for Q3 2026.** With the assignment data, the existing components 160 and 81 on ranks 21 and 22 keep their
seats, ahead of the newcomers 249 and 28 on ranks 19 and 20. Security 177 joins on rank 7 and security 103 leaves.
`SmiReviewTest` runs the review on these files and checks this result.

## Weighting and capping

The assignment and the rulebook define:

- Each component is weighted by its share of the total FFMCAP of the selected components (section 5.12.4).
- A weight above the cap of 18% is limited to exactly the cap with a capping factor (sections 2.1 and 5.12.4).
- The weight cut from a capped component goes to the components below the cap, in proportion to their weights (the
  example in the assignment).

Weights are stored as fractions of the index, `0.18` for 18%.

### Assumptions on the capping formula

The rulebook gives no formula for the capping factor; the calculation details are in a separate SIX calculation
rulebook that is not part of the assignment. The assignment leaves the details open. The application uses these
**assumptions**:

1. **Rounds.** Giving weight to the components below the cap can push one of them above the cap. The redistribution
   is therefore repeated in rounds until no weight exceeds the cap. The assignment example needs one round; the rule
   that no constituent may exceed the cap requires the repetition in general.
2. **Each round starts from FFMCAP.** Instead of adding the cut weight on top of the previous weights, every round
   recomputes the weights from FFMCAP:
    - a capped component gets exactly the cap;
    - every other component gets `remaining weight x FFMCAP / FFMCAP of all uncapped components`, where
      `remaining weight = 1 - cap x number of capped components`.

   This gives the same result as handing out the cut weight in proportion, without rounding differences adding up over
   the rounds.
3. **Capping factor.** An uncapped component has a factor of 1, so its full FFMCAP counts. A capped component gets
   `cap x FFMCAP of all uncapped components / (remaining weight x FFMCAP)`, the factor that brings its FFMCAP down to
   the cap. `FFMCAP x capping factor`, divided by its total, gives the final weights. Multiplying every factor by the
   same number gives the same weights, so the scale is a convention; this one makes 1 mean "not capped".
4. **Precision.** Divisions use `BigDecimal` with 16 significant digits (`MathContext.DECIMAL64`). Capped weights are
   set to exactly the cap. The weights are not rounded in the report. After the rounds, the review checks that the
   weights add up to 1 within 10^-12 and that none exceeds the cap; otherwise it adds an `INVALID_WEIGHTS` error.
5. **Cap too low.** When `number of components x cap` is below 100%, no weights can meet the cap. The application does
   not start with such a configuration, and the weighting adds a `CAP_NOT_FEASIBLE` error if it still happens.

### Example with two rounds

Five components, a cap of 21%. Weights are rounded to 4 decimals here, so a total can differ by 0.0001.

| Component | FFMCAP    | Weight before capping           |
|-----------|-----------|---------------------------------|
| A         | 8,928.00  | 8,928.00 / 39,025.38 = 22.8774% |
| B         | 8,438.75  | 8,438.75 / 39,025.38 = 21.6237% |
| C         | 7,860.93  | 7,860.93 / 39,025.38 = 20.1431% |
| D         | 7,235.20  | 7,235.20 / 39,025.38 = 18.5397% |
| E         | 6,562.50  | 6,562.50 / 39,025.38 = 16.8160% |
| Total     | 39,025.38 | 100%                            |

**Round 1.** A and B exceed 21% and are capped.

- Cut weight: (22.8774 - 21) + (21.6237 - 21) = 1.8774 + 0.6237 = 2.5012%.
- C, D and E are below the cap and receive it in proportion to their weights, which add up to 55.4988%:

| Component | Share of the cut weight    | Receives                    | New weight                   |
|-----------|----------------------------|-----------------------------|------------------------------|
| C         | 20.1431 / 55.4988 = 36.29% | 2.5012 x 0.3629 = 0.9078%   | 20.1431 + 0.9078 = 21.0509%  |
| D         | 18.5397 / 55.4988 = 33.41% | 2.5012 x 0.3341 = 0.8355%   | 18.5397 + 0.8355 = 19.3753%  |
| E         | 16.8160 / 55.4988 = 30.30% | 2.5012 x 0.3030 = 0.7578%   | 16.8160 + 0.7578 = 17.5738%  |

With the formula of assumption 2: remaining weight = 100% - 2 x 21% = 58%, FFMCAP of C, D and E = 21,658.63, so C gets
58% x 7,860.93 / 21,658.63 = 21.0509%. C was below the cap before the round and now exceeds it.

**Round 2.** C is capped.

- Cut weight: 21.0509 - 21 = 0.0509%.
- D and E receive it in proportion to their weights, which add up to 36.9491%:

| Component | Share of the cut weight    | Receives                    | New weight                   |
|-----------|----------------------------|-----------------------------|------------------------------|
| D         | 19.3753 / 36.9491 = 52.44% | 0.0509 x 0.5244 = 0.0267%   | 19.3753 + 0.0267 = 19.4020%  |
| E         | 17.5738 / 36.9491 = 47.56% | 0.0509 x 0.4756 = 0.0242%   | 17.5738 + 0.0242 = 17.5980%  |

With the formula: remaining weight = 100% - 3 x 21% = 37%, FFMCAP of D and E = 13,797.70, so D gets
37% x 7,235.20 / 13,797.70 = 19.4020%. No weight exceeds 21%, so the rounds stop.

**Result.**

| Component | Final weight | Capping factor | FFMCAP x capping factor |
|-----------|--------------|----------------|-------------------------|
| A         | 21%          | 0.877142       | 7,831.13                |
| B         | 21%          | 0.927996       | 7,831.13                |
| C         | 21%          | 0.996209       | 7,831.13                |
| D         | 19.4020%     | 1              | 7,235.20                |
| E         | 17.5980%     | 1              | 6,562.50                |
| Total     | 100%         |                | 37,291.08               |

Each final weight is `FFMCAP x capping factor / 37,291.08`. For A the factor is
21% x 13,797.70 / (37% x 8,928.00) = 0.877142. `WeightCalculatorStepTest` checks this example.

**Result for Q3 2026.** With the assignment data, securities 155 and 205 weigh 25.42% and 23.49% before capping. One
round caps both at 18%, with capping factors 0.565353 and 0.611736, and the largest uncapped weight is security 63 with
14.26%. `SmiReviewTest` runs the review on these files and checks this result.

## Joiners and leavers

- **Joiners** are selected components that are not in the current composition.
- **Leavers** are current components that are not selected.

