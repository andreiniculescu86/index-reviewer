# Assumptions and design decisions

The assignment says: *"The exact implementation details are intentionally left open. You are expected to make
reasonable assumptions where necessary, document them clearly, and justify their design decisions."*

This page lists those choices. Rules the assignment or the rulebook fix (FFMCAP formula, dates, 20 components, ranks
1 to 18 direct, 18% cap, proportional redistribution) are in [Methodology](methodology.md), not here.

## What the assignment leaves open

| Open point in the assignment                    | Choice made                                     | See        |
|-------------------------------------------------|-------------------------------------------------|------------|
| "A selection buffer may be considered"          | Buffer applied, current components first        | A1         |
| How weights are adjusted with capping factors   | Rounds; factor 1 = not capped; 4 decimals       | A2 to A5   |
| What "weighting factors" are                    | Final weight and capping factor                 | A6         |
| What "review status" is                         | Most severe finding decides the status          | A7         |
| What "data validation" checks and how it reacts | Missing data excludes, bad data fails           | A8 to A10  |
| Ambiguous cases in the rules and the data       | Ties, universe as given, rules without data     | A11 to A14 |
| How the API looks                               | One upload, one report model, `422` on bad data | D1 to D3   |
| How to support other indices, dates and rules   | One reviewer per index, rules in config         | D4, D5     |
| How to make results traceable and auditable     | Findings, trace per step, audit record          | D6, D7     |

## Assumptions

**Selection**

- **A1. Buffer applied.** Ranks 19 to 22 go to current components first, then to newcomers by rank (rulebook
  5.12.3.2). *Why:* the assignment allows it, and it is how SIX reviews the SMI: it keeps the index stable.

**Weighting**

- **A2. Capping in rounds.** Cap every weight above 18%, share the cut weight among the others, repeat until none is
  above. *Why:* sharing can push another component above the cap; the example needs one round, real data may not.
- **A3. Capping factor.** A multiplier on FFMCAP: `1` when not capped, below 1 when capped. *Why:* the rulebook gives
  no formula; it is in a separate SIX calculation rulebook. Formula in [Methodology](methodology.md#assumptions).
- **A4. 4 decimals.** Weights (`18.0000` = 18%) and capping factors, rounded half up. *Why:* no precision is given.
- **A5. Rounding leftover to the last constituent.** The few 0.0001 that rounding misses go to the lowest-ranked
  constituent, so weights sum to exactly 100. *Why:* capped weights stay at exactly 18%, and the result is repeatable.

**Report**

- **A6. Weighting factors** are the final `weight` and the `cappingFactor` of each constituent. *Why:* the assignment
  does not define the term; the report shows both the result and how much each component was cut.
- **A7. Review status** follows the most severe finding: `SUCCESS` (none), `SUCCESS_WITH_WARNINGS`, `FAILED` (a data
  error), `ERROR` (unexpected failure). *Why:* the term is not defined; the findings in the report explain the status.

**Data**

- **A8. Missing data excludes the security with a warning.** No cut-off price or no review-date row (security 166 in
  Q3 2026). *Why:* the rulebook does not stop a review for missing data (7.1); listings and delistings cause it.
- **A9. Invalid data fails the review.** Price or shares 0 or negative, free float outside 0 to 1, two different rows
  for one security and date. *Why:* the rulebook does not use wrong data (7.2); skipping it could drop a top 20
  security unnoticed.
- **A10. Identical duplicate rows count once.** *Why:* the assignment universe lists 204 securities twice; identical
  rows cannot change the result.

**Ambiguous cases**

- **A11. Ties.** Equal FFMCAP ranks current components first, then by composition order, then by lower id, with a
  `SELECTION_TIE` warning when it decides a seat. *Why:* no rule in the rulebook; its stability principle (1.3).
- **A12. Universe taken as given.** The SPI 20% free float minimum is not checked again; data outside the universe is
  ignored; a current component outside the universe leaves. *Why:* the universe is an input of the assignment.
- **A13. Rules without data are not applied.** Issuer aggregation (5.12.4, no issuer column), the liquidity rule
  (5.12.3.2, no turnover), the 12-month selection list (4.3, the assignment ranks by FFMCAP), capping between
  reviews (5.12.4, out of scope).
- **A14. One currency.** All prices are CHF. *Why:* the data has no currency column.

## Design decisions

- **D1. One multipart request per review**, with the 3 files as given and an optional JSON part for index, dates
  and separator. *Instead of* uploading files once and referencing them. *Why:* one review needs nothing more.
- **D2. One report model** for `POST /v1/reviews`, `GET /v1/reviews/{reviewId}` and the list; audit and trace are
  separate endpoints. *Why:* one resource, one model; audit and trace answer other questions.
- **D3. Data errors return `422` and are stored**; an invalid request returns `400` and is not. *Why:* the request was
  valid, the data was not, and a failed review must stay traceable.
- **D4. One reviewer per index, shared steps.** Each step takes the index rules and returns result, trace and
  findings. *Instead of* a rule engine. *Why:* a new index adds a reviewer; API, service and steps stay unchanged.
- **D5. Index rules in `application.yml`**, checked at startup. *Instead of* constants. *Why:* rules change with the
  rulebook; a wrong rule stops the start instead of producing a wrong index.
- **D6. Data problems are findings, not exceptions.** All rows are checked and every problem is reported at once,
  with code, severity, security and field. *Why:* one run shows all problems, and each is stored with the review.
- **D7. Trace written after each step; report derived from audit record and trace.** *Why:* a failed review keeps
  what was calculated before the error, and the report cannot disagree with its explanation.
- **D8. In-memory storage behind repository interfaces.** *Instead of* a database. *Why:* runs without setup; a
  database can replace it without changing the services. Stored reviews are lost on restart.
- **D9. Exact decimals; each capping round starts from FFMCAP.** *Instead of* floating point. *Why:* FFMCAPs reach
  hundreds of billions and results must match exactly; rounding errors do not add up over rounds.
