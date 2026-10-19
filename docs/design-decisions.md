# Assumptions and design decisions

This page lists only what was decided where the assignment says nothing. What the assignment and the rulebook
require is described in [Methodology](methodology.md).

## Contents

- [Summary](#summary)
- [Assumptions](#assumptions)
    - [Selection](#selection): A1, A2
    - [Weighting](#weighting): A3 to A6
    - [Report](#report): A7, A8
    - [Data](#data): A9 to A12
    - [Not covered by the rulebook](#not-covered-by-the-rulebook): A13 to A15
- [Design decisions](#design-decisions): D1 to D9

## Summary

| Not defined by the assignment                    | Decision                                        | See        |
|--------------------------------------------------|-------------------------------------------------|------------|
| Whether to use the optional selection buffer     | Used                                            | A1         |
| Order of securities with equal FFMCAP            | Current members first, then by id               | A2         |
| Capping that pushes another weight above 18%     | Capping repeats until no weight is above 18%    | A3         |
| How the capping factor is calculated             | Multiplier on FFMCAP; 1 = not capped            | A4         |
| Precision of weights and capping factors         | Weights 4 decimals, factors 10 digits           | A5         |
| Rounded weights that do not add up to 100        | Largest remainder method                        | A6         |
| What the "weighting factors" in the report are   | Final weight and capping factor                 | A7         |
| What the "review status" values are              | 4 values, set by the most severe finding        | A8         |
| Missing data for a security                      | Security excluded, with a warning               | A9         |
| Wrong data for a security                        | Review fails                                    | A10        |
| Identical duplicate rows                         | Counted once                                    | A11        |
| A current composition without 20 securities      | Review fails                                    | A12        |
| SPI checks, issuer and liquidity rules, currency | Universe taken as given; rules not applied; CHF | A13 to A15 |

## Assumptions

### Selection

- **A1. The selection buffer is used.**

  *Why:* the assignment makes it optional; using it keeps current members stable, as SIX does for the SMI.

- **A2. Ties.** Securities with equal FFMCAP are ordered current members first, then in the order of the composition
  file, then newcomers by lower id. A `SELECTION_TIE` warning is added when a tie decides a seat.

  *Why:* the rulebook has no tie rule; current members first follows its stability principle (1.3), and the id makes the
  order repeatable.

### Weighting

- **A3. Capping repeats until no weight is above 18%.**

  *Why:* sharing the cut weight can push another constituent above the cap. The assignment example needs one round;
  end-to-end scenarios 3 and 14 need several.

- **A4. The capping factor is a multiplier on FFMCAP:** `1` when not capped, below 1 when capped.

  *Why:* the rulebook gives no formula; it is in a separate SIX calculation rulebook. Formula in
  [Methodology](methodology.md#assumptions).

- **A5. Weights have 4 decimals (`18.0000` = 18%); capping factors have 10 significant digits** (`0.5653526015`).

  *Why:* no precision is given; 0.0001% is far below any meaningful weight difference. The factor needs more digits:
  with 4 decimals, `FFMCAP x capping factor` would give security 155 18.0014% instead of 18%.
- **A6. Rounding uses the largest remainder method.** Every weight is rounded down; the 0.0001 units still missing to
  100 go to the weights with the largest remainders.

  *Why:* every weight stays within 0.0001 of its exact value, the weights add up to exactly 100, no weight can become
  negative, and capped weights stay at exactly 18%.

### Report

- **A7. Weighting factors** are the final `weight` and the `cappingFactor` of each constituent.

  *Why:* the term is not defined; the weight is the result, the capping factor shows how much a constituent was cut.

- **A8. Review status** is `SUCCESS`, `SUCCESS_WITH_WARNINGS`, `FAILED` (data error) or `ERROR` (unexpected failure),
  set by the most severe finding.

  *Why:* the values are not defined; the findings in the report explain the status.

### Data

- **A9. Missing data excludes the security with a warning:** no cut-off price or no review-date row (security 166 in Q3
  2026).

  *Why:* the rulebook would use the latest available data (7.1), but the files only hold the two review dates.
  Consequence: a security can leave because its data is missing, not because of its rank; the warning shows it.

- **A10. Wrong data fails the review:** price or shares 0 or negative, free float outside 0 to 1, two different rows for
  one security and date.

  *Why:* a wrong value gives a wrong index and cannot be corrected here; skipping the row could drop a top 20 security
  unnoticed.

- **A11. Identical duplicate rows count once.**

  *Why:* the assignment universe lists 204 securities twice; identical rows cannot change the result.

- **A12. The current composition must have exactly 20 securities**, otherwise the review fails.

  *Why:* the buffer and the joiners and leavers are measured against the full current index.

### Not covered by the rulebook

- **A13. The universe file is taken as given.** The SPI free float minimum of 20% is not checked again, security data
  outside the universe is ignored, even when it has conflicting rows, and a current member outside the universe leaves.

  *Why:* the universe is an input of the assignment.

- **A14. Issuer aggregation and the liquidity rule are not applied.**

  *Why:* the data has no issuer and no turnover, so each security id counts as its own issuer.

- **A15. All prices are in CHF.**

  *Why:* the data has no currency column.

## Design decisions

- **D1. One multipart request per review**, with the 3 files unchanged and an optional JSON part for index, dates and
  separator. *Instead of* uploading the files once and referencing them.

  *Why:* one review needs nothing more.

- **D2. One report model** for running, reading and listing reviews; the audit record and the trace are separate
  endpoints.

  *Why:* one resource, one model; the audit record and the trace answer other questions.

- **D3. Data errors return `422` and are stored**; an invalid request returns `400` and is not stored.

  *Why:* the request was valid, the data was not, and a failed review must stay traceable.

- **D4. One reviewer for all indices of the same shape, with shared steps.** `RankedIndexReviewer` reviews every
  index configured under `indices`; each step takes that index's rules and returns its result, trace records and
  findings. *Instead of* one reviewer class per index, or a rule engine.

  *Why:* an index of the same shape is added with an enum value and a YAML block, without code logic changes. An index
  with other rules gets its own reviewer next to it.

- **D5. Index rules in `application.yml`**, one block per index, checked at startup. *Instead of* constants in code.

  *Why:* rules change with the rulebook; a wrong rule stops the start instead of producing a wrong index.

- **D6. Data problems are findings, not exceptions.** All rows are checked and every problem is reported at once.

  *Why:* one run shows all problems, and each is stored with the review.
- **D7. The trace is written after each step, and the report is derived from the audit record and the trace.**

  *Why:* a failed review keeps what was calculated before the error, and the report cannot disagree with its
  explanation.
- **D8. In-memory storage behind repository interfaces.** *Instead of* a database.

  *Why:* the application runs without setup; a database can replace it without changing the services. Stored reviews are
  lost on restart.

- **D9. Exact decimals, and each capping round starts again from FFMCAP.** *Instead of* floating point.

  *Why:* FFMCAPs reach hundreds of billions and the results must match exactly; rounding errors do not add up over
  rounds.
