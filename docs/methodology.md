# Review methodology

How the application reviews the SMI. It follows the assignment and the SIX Swiss Index Methodology Rulebook 3.40 of
08.06.2026. Rules that the rulebook does not define are marked as **assumption**; all assumptions are listed in
[Assumptions and design decisions](design-decisions.md#assumptions).

See also [Data validation](data-validation.md) and [Traceability and audit](traceability-and-auditability.md).

## Contents

- [Configuration](#configuration)
  - [Adding an index](#adding-an-index)
- [Dates](#dates)
- [Eligibility](#eligibility)
- [FFMCAP](#ffmcap)
- [Ranking](#ranking)
- [Selection with the buffer](#selection-with-the-buffer)
  - [Selection decisions](#selection-decisions)
- [Weighting and capping](#weighting-and-capping)
  - [Assumptions](#assumptions)
- [Joiners and leavers](#joiners-and-leavers)
- [Not implemented](#not-implemented)

## Configuration

The index rules are in [application.yml](../src/main/resources/application.yml), one block per index under
`indices`. The SMI block:

| Property                | SMI  | Rulebook                      |
|-------------------------|------|-------------------------------|
| `number-of-components`  | 20   | 5.12.3.2                      |
| `direct-selection-rank` | 18   | 5.12.3.2, ranks 1 to 18       |
| `buffer-rank`           | 22   | 5.12.3.2, buffer ranks 19-22  |
| `cap-percentage`        | 18   | 5.12.4                        |
| `methodology-version`   | 3.40 | Rulebook version of the rules |

The index is chosen with `indexType`:

| `indexType` | Meaning                                       |
|-------------|-----------------------------------------------|
| `SMI`       | Swiss Market Index; the only index configured |

The application does not start when:

- `direct-selection-rank` > `number-of-components`, or `number-of-components` > `buffer-rank`;
- `number-of-components` x `cap-percentage` < 100, because the weights could then not meet the cap.

These checks run for every configured index.

### Adding an index

An index that is ranked by FFMCAP, selected with a buffer and weighted by FFMCAP with one cap for all components is
added without code logic changes. Examples from the rulebook:

| Index        | Size | Direct | Buffer up to | Cap                          |
|--------------|------|--------|--------------|------------------------------|
| SMI Expanded | 50   | 47     | 53           | none (`cap-percentage: 100`) |
| UBS 100      | 100  | 100    | 100          | none (`cap-percentage: 100`) |
| SPI 20       | 20   | 18     | 22           | none (`cap-percentage: 100`) |

UBS 100 has no buffer, so its direct selection rank and buffer rank both equal its size.

1. Add the index to [IndexType](../src/main/java/com/example/indexreviewer/model/IndexType.java), for example
   `SMI_EXPANDED`.
2. Add its block under `indices` in `application.yml` with the 5 properties above. Write the key in lower case with
   `-` between words, for example `smi-expanded:` for `SMI_EXPANDED`.
3. Add tests: one that the block binds to the enum value and passes the startup checks, and one review with data for
   that index, whose composition has exactly `number-of-components` securities.

`RankedIndexReviewer` reviews every configured index; `ReviewService`, the steps, the API, the audit record and the
trace stay unchanged. A request selects the index with `indexType`.

Indices with other rules need code: the SLI caps its 4 largest components at 9% and the others at 4.5%, and SMIM,
SPI Extra and SPI ex SLI are derived from other indices. See [Not implemented](#not-implemented).

## Dates

| Date    | Q3 2026    | Used for                               |
|---------|------------|----------------------------------------|
| Cut-off | 2026-09-10 | Price                                  |
| Review  | 2026-09-21 | Universe, number of shares, free float |

Both dates match the rulebook schedule: the capping data cut-off is the Thursday 8 days before implementation on
Friday 18 September (section 4.2), and new shares and free float apply from the Monday after (section 3.1).

## Eligibility

A security is a candidate when it is in the SPI universe on the review date and has valid data for both dates.
Missing data excludes the security with a warning. Invalid data fails the review. See
[Data validation](data-validation.md).

The universe file is taken as given. The SPI free float minimum of 20% (section 5.2.3.2) is not checked again,
because the assignment does not ask for it.

## FFMCAP

```
FFMCAP = Price(cut-off date) x Shares(review date) x Free Float(review date)
```

Calculated exactly with `BigDecimal`, without rounding.

## Ranking

Candidates are ranked by FFMCAP, highest first.

**Equal FFMCAP (assumption).** The rulebook has no rule for ties. Following its stability principle (section 1.3),
equal FFMCAPs are ordered:

1. existing components first;
2. existing components in the order of the composition file;
3. newcomers by the lower security id.

When a tie decides who is selected, the review adds a `SELECTION_TIE` warning, see
[finding codes](traceability-and-auditability.md#finding-codes).

## Selection with the buffer

Section 5.12.3.2:

1. Ranks 1 to 18 are selected.
2. The last 2 seats go to existing components in ranks 19 to 22, in rank order.
3. Seats still free go to newcomers in ranks 19 to 22, in rank order.
4. Rank 23 or lower is never selected. An existing component there leaves.

If the selection does not have exactly 20 components, the review fails with `SELECTION_SIZE`.

### Selection decisions

Every security in the trace gets one decision:

| Decision                             | Meaning                                                        |
|--------------------------------------|----------------------------------------------------------------|
| `SELECTED_DIRECT`                    | Rank 1 to 18; selected                                         |
| `SELECTED_BUFFER_EXISTING_COMPONENT` | Rank 19 to 22, already in the index; keeps its seat            |
| `SELECTED_BUFFER_NEWCOMER`           | Rank 19 to 22, not in the index; took a seat still free        |
| `NOT_SELECTED_BUFFER`                | Rank 19 to 22; no seat left. An existing component here leaves |
| `NOT_SELECTED_BELOW_BUFFER`          | Rank 23 or lower; never selected. An existing component leaves |
| `NOT_ELIGIBLE`                       | Not ranked: no valid FFMCAP; its findings say why              |

**Example.** Ranks 19 and 20 are newcomers, ranks 21 and 22 are existing components. The existing components keep
the 2 seats. The newcomers are not selected.

**Q3 2026 result.** Existing components 160 and 81 on ranks 21 and 22 stay, ahead of newcomers 249 and 28 on ranks 19
and 20. Security 177 joins on rank 7, security 103 leaves. `RankedIndexReviewerTest` checks this.

## Weighting and capping

From the assignment and the rulebook (sections 2.1, 5.12.4):

- the weight is the share of the total FFMCAP of the 20 components;
- a weight above 18% is cut to exactly 18% with a capping factor;
- the cut weight goes to the other components in proportion to their weight.

Weights are percentages, `18` for 18%, with 4 decimals, and add up to exactly 100. Capping factors have 10
decimals.

### Assumptions

The rulebook gives no formula. It is in a separate SIX calculation rulebook that is not part of the assignment.

1. **Rounds.** Giving weight to the others can push one of them above the cap, so capping repeats until no weight is
   above the cap.
2. **Each round starts from FFMCAP.** Capped components get the cap. The others get
   `remaining weight x FFMCAP / FFMCAP of all uncapped`, with `remaining weight = 100 - cap x number capped`. This
   equals the proportional sharing, without rounding errors adding up over rounds.
3. **Capping factor.** 1 when not capped. When capped: `cap x FFMCAP of all uncapped / (remaining weight x FFMCAP)`.
   `FFMCAP x capping factor` is the capped market capitalization; divided by the sum over all components it gives the
   weight. The scale is a convention: 1 means "not capped". The factor has 10 decimals (`0.5653526015`), so that
   `FFMCAP x capping factor` reproduces the weights to well below 0.0001.
4. **Largest remainder rounding.** Every weight is rounded down to 4 decimals. The 0.0001 units still missing to 100
   go one each to the weights with the largest rounding remainders; on equal remainders the higher rank goes first.
   Every weight stays within 0.0001 of its exact value, and the weights add up to exactly 100.
5. **Final check.** The review fails with `INVALID_WEIGHTS` when a weight is above the cap or negative, the weights
   do not add up to 100, or `FFMCAP x capping factor` does not reproduce a weight within 0.0001, see
   [finding codes](traceability-and-auditability.md#finding-codes). With a valid configuration this cannot happen.

**Example.** Components A, B, C, D with FFMCAP 50, 30, 15, 5 and a cap of 35%:

| Round | Weights                   | Newly capped |
|-------|---------------------------|--------------|
| 1     | A 50, B 30, C 15, D 5     | A            |
| 2     | A 35, B 39, C 19.5, D 6.5 | B            |
| 3     | A 35, B 35, C 22.5, D 7.5 | none, stop   |

Capping factors: A `35 x 20 / (30 x 50) = 0.4666666667`, B `35 x 20 / (30 x 30) = 0.7777777778`, C and D `1`.
`WeightCalculatorStepTest` checks a two-round example with 5 components and a cap of 21%.

**Q3 2026 result.** Securities 155 and 205 weigh 25.42% and 23.49% before capping. Both are capped at 18%, with
capping factors 0.5653526015 and 0.6117362279. Rounded down, the weights add up to 99.9991%; the 9 missing units of
0.0001 go to the 9 weights with the largest remainders, for example security 63 ends at 14.2591% (exact 14.259043%).
`RankedIndexReviewerTest` checks this.

## Joiners and leavers

- **Joiners:** selected, but not in the current composition.
- **Leavers:** in the current composition, but not selected.

## Not implemented

- **Issuer aggregation** (section 5.12.4): the data has no issuer, so each security id counts as its own issuer.
- **Liquidity rule for multiple primary listings** (section 5.12.3.2): the data has no turnover.
- **Selection list** (section 4.3): the assignment ranks by FFMCAP only, not by 12-month FFMCAP and turnover.
- **Capping between reviews** (section 5.12.4): only the review itself is in scope.
- **Other indices**: only the SMI is configured. Indices of the same shape are added by configuration, see
  [Adding an index](#adding-an-index). Indices with a tiered cap (SLI), derived from other indices (SMIM, SPI Extra,
  SPI ex SLI) or based on other data (ESG, dividends) need new steps.
