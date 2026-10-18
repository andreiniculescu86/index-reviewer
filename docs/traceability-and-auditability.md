# Traceability and audit

Every review answers two questions afterwards:

- **Traceability:** why does the result look the way it does? Every number can be followed back to its input values.
- **Auditability:** what did the review use: when it ran, with which application version and rules?

Each review is stored in two parts. The **trace** is written stage by stage while the review runs: after each validation
and step, the index reviewer appends the records and findings it returned. The **audit record** is written once when the
review ends, also when it failed or stopped on an unexpected error. Nothing stored is changed afterwards. The report of
a new review is built from the results of its steps, without reading the stored parts back. The report of a stored
review is derived from its audit record and trace. The data checks that create findings are described in
[Data validation](data-validation.md), the ranking and selection rules behind the selection findings in
[Methodology](methodology.md).

**Example review used on this page:** the assignment files, with the cut-off price of security 10 changed to `-2080` in
a copy of the security data. The review failed after the FFMCAP calculation and kept all traces. Its id is
`51b43dce-e920-4416-80f8-50f846df339a`.

## What is stored

| Part                 | Contains                                                          | Stored                    |
|----------------------|-------------------------------------------------------------------|---------------------------|
| [Audit](#audit-part) | Id, index, dates, [status](#review-status), start, version, rules | Once, at the end          |
| [Trace](#trace-part) | One entry per security, plus all findings                         | After each stage          |

The audit record answers which review this is, what it used and how it ended. The trace answers why the result looks
the way it does.

In the code, the audit part is in `model/audit` and stored through `ReviewAuditRepository`. The trace part is in
`model/trace` and stored through `ReviewTraceRepository`.

## Audit part

Returned by `GET /v1/reviews/{reviewId}/audit`.

| Field         | Meaning                                                                                              |
|---------------|------------------------------------------------------------------------------------------------------|
| `status`      | How the review ended, see [Review status](#review-status)                                            |
| `executedAt`  | When the review started, in UTC                                                                      |
| `appVersion`  | Application version that ran the review                                                              |
| `rules`       | Number of components, direct selection rank, buffer rank, cap and methodology version that were used |
| `composition` | New index composition, security ids in rank order; empty when `FAILED` or `ERROR`                    |

**Example:**

```json
{
  "reviewId": "51b43dce-e920-4416-80f8-50f846df339a",
  "indexType": "SMI",
  "cutoffDate": "2026-09-10",
  "reviewDate": "2026-09-21",
  "status": "FAILED",
  "executedAt": "2026-10-17T20:27:12.127565Z",
  "appVersion": "0.1.0-SNAPSHOT",
  "rules": {
    "numberOfComponents": 20,
    "directSelectionRank": 18,
    "bufferRank": 22,
    "capPercentage": 18,
    "methodologyVersion": "SIX Swiss Index Methodology Rulebook 3.40, 08.06.2026"
  },
  "composition": []
}
```

## Trace part

Returned by `GET /v1/reviews/{reviewId}/trace`, or for one security by
`GET /v1/reviews/{reviewId}/securities/{securityId}`. It holds the [security traces](#security-trace) and the
[findings](#findings).

### Security trace

There is one entry for every security in the universe and for every existing index component.

| Field               | Meaning                                                                                |
|---------------------|----------------------------------------------------------------------------------------|
| `securityId`        | The security                                                                           |
| `existingComponent` | Whether the security is in the current index                                           |
| `price`             | Price from the cut-off date row                                                        |
| `shares`            | Number of shares from the review date row                                              |
| `freeFloat`         | Free float from the review date row                                                    |
| `ffmcap`            | `price x shares x freeFloat`; empty when the security is not eligible                  |
| `rank`              | Rank by FFMCAP, 1 for the largest; ties follow the [tie order](methodology.md#ranking) |
| `decision`          | [Selection decision](#selection-decisions)                                             |
| `cappingFactor`     | 1 if not capped, below 1 if capped; [formula](methodology.md#weighting-and-capping)    |
| `weight`            | Final weight as a percentage of the index with 4 decimals, `18.0000` for 18%           |

A value is empty (`null`) when the step that fills it did not run.

Each step returns its own records: the FFMCAP step one per security, the selection step one per candidate and the
weighting step one per index component. The index reviewer stores them, and the trace combines them per security when
it is read.

**Example:** where does the FFMCAP of security 249 come from?

```json
{
  "securityId": 249,
  "existingComponent": false,
  "price": 857.4,
  "shares": 26700000,
  "freeFloat": 0.8486,
  "ffmcap": 19426643388.00000,
  "rank": null,
  "decision": null,
  "cappingFactor": null,
  "weight": null
}
```

`857.4 x 26700000 x 0.8486 = 19426643388`. The free float `0.8486` comes from the review date row. The cut-off date row
has `0.8487`, which is not used. `rank` and `decision` are empty because the selection step did not run.

### Selection decisions

Every ranked candidate gets one decision of the buffer rule described in
[Selection with the buffer](methodology.md#selection-with-the-buffer).

| Decision                             | Meaning                                                     |
|--------------------------------------|-------------------------------------------------------------|
| `SELECTED_DIRECT`                    | Rank 1 to 18, selected directly                             |
| `SELECTED_BUFFER_EXISTING_COMPONENT` | Rank 19 to 22, already in the index, kept by the buffer     |
| `SELECTED_BUFFER_NEWCOMER`           | Rank 19 to 22, new, selected for a seat that was still free |
| `NOT_SELECTED_BUFFER`                | Rank 19 to 22, no seat left                                 |
| `NOT_SELECTED_BELOW_BUFFER`          | Rank 23 or lower                                            |
| `NOT_ELIGIBLE`                       | No valid FFMCAP; the findings of the security explain why   |

**Example:** why is security 166 not eligible? `GET /v1/reviews/51b43dce-e920-4416-80f8-50f846df339a/securities/166`
returns its trace and its finding:

```json
{
  "reviewId": "51b43dce-e920-4416-80f8-50f846df339a",
  "indexType": "SMI",
  "cutoffDate": "2026-09-10",
  "reviewDate": "2026-09-21",
  "status": "FAILED",
  "securities": [
    {
      "securityId": 166,
      "existingComponent": false,
      "price": 0.13,
      "shares": null,
      "freeFloat": null,
      "ffmcap": null,
      "rank": null,
      "decision": "NOT_ELIGIBLE",
      "cappingFactor": null,
      "weight": null
    }
  ],
  "findings": [
    {
      "code": "MISSING_REVIEW_DATA",
      "severity": "WARNING",
      "securityId": 166,
      "field": null,
      "message": "Security 166 excluded: no shares and free float on the review date"
    }
  ]
}
```

The security has a price but no shares and free float, so there is no FFMCAP. The finding
[`MISSING_REVIEW_DATA`](#finding-codes) says why.

### Findings

A finding describes one problem found during the review: in the input data, or in the selection or weighting, for
example a tie that decided the selection. It has a code, a severity, the security it is about (if any), the field that
caused it (if any) and a readable message.

| Severity  | Effect                                                    |
|-----------|-----------------------------------------------------------|
| `ERROR`   | The review fails                                          |
| `WARNING` | The review continues; the problem is handled and recorded |
| `INFO`    | No effect; only for information                           |

#### Finding codes

| Code                                 | Severity | Meaning                                                           |
|--------------------------------------|----------|-------------------------------------------------------------------|
| `MISSING_DATE`                       | ERROR    | The universe or the security data has no rows for a required date |
| `COMPOSITION_SIZE`                   | ERROR    | The current composition does not have 20 different securities     |
| `TOO_FEW_ELIGIBLE`                   | ERROR    | Fewer than 20 securities have a valid FFMCAP                      |
| `INVALID_VALUE`                      | ERROR    | Price, shares or free float missing, zero, negative or too large  |
| `CONFLICTING_DUPLICATE`              | ERROR    | Two rows for the same security and date have different values     |
| `SELECTION_SIZE`                     | ERROR    | The selection did not fill the index with exactly 20 components   |
| `INVALID_WEIGHTS`                    | ERROR    | A weight exceeds the cap                                          |
| `MISSING_CUTOFF_PRICE`               | WARNING  | No price on the cut-off date; the security is not eligible        |
| `MISSING_REVIEW_DATA`                | WARNING  | No row on the review date; the security is not eligible           |
| `DUPLICATE_ROW`                      | WARNING  | Two identical rows for the same security and date; counted once   |
| `EXISTING_COMPONENT_NOT_IN_UNIVERSE` | WARNING  | A current index component is not in the universe; it will leave   |
| `SELECTION_TIE`                      | WARNING  | Equal FFMCAPs were split into selected and not selected           |
| `IGNORED_SECURITIES`                 | INFO     | The security data has rows for securities outside the universe    |

**Example:** the error of security 10, which made the example review fail:

```json
{
  "code": "INVALID_VALUE",
  "severity": "ERROR",
  "securityId": 10,
  "field": "price",
  "message": "Security 10: Price must be positive but is -2080"
}
```

The trace of security 10 keeps the invalid price `-2080` and has the decision `NOT_ELIGIBLE`.

## Review status

The status is stored in the audit record. A review that ends normally gets the status calculated from its
[findings](#findings). A review that stops on an unexpected error gets `ERROR`.

| Status                  | Meaning                                                                       |
|-------------------------|-------------------------------------------------------------------------------|
| `SUCCESS`               | No errors and no warnings                                                     |
| `SUCCESS_WITH_WARNINGS` | No errors, at least one warning; the result is still correct                  |
| `FAILED`                | At least one error; the review stopped                                        |
| `ERROR`                 | The review stopped on an unexpected error; the trace shows how far it got     |

A review with status `FAILED` or `ERROR` has no composition, so its report has empty lists.

**Example:** the example review has one error (`INVALID_VALUE` for security 10) and one warning (`MISSING_REVIEW_DATA`
for security 166), so its status is `FAILED`.

## How the audit is protected

- **Stored once, never changed.** The audit record is written once; storing the same review id twice is rejected.
  Trace records are only appended, never updated or deleted.
- **Failed reviews and reviews with an unexpected error are stored too.** The audit record is written when the review
  ends in any way, so a review that throws still gets one with status `ERROR`. Requests that are rejected before the
  data checks, for example a missing or unreadable file, are not reviews and are not stored.
- **A review is visible once its audit record is stored.** While a review runs, its trace records exist, but the API
  answers its id with not found.
- **The rules, the methodology version and the application version are stored**, because all of them can change
  after a review.
- **Input files are not stored.** A review cannot be repeated from the stored record alone.
- **Logs are not the audit record.** The stored audit record and trace are.

## Design notes

- Validators and steps do not store anything. Each returns its findings and trace records, built from the values it
  used, so they cannot differ from those values. The index reviewer (`SmiReviewer`) stores them right after the stage
  ends and passes on only what the next step needs, for example the candidates with their FFMCAP.
- Findings are recorded where a problem is found. Some, like a missing date, belong to no security.
- In a database, the audit record maps to a table such as `review_audit` and the trace to one table per record type,
  such as `review_ffmcap`, `review_selection`, `review_weight` and `review_finding`. After each stage the index reviewer
  inserts its records and findings, and the audit row is inserted at the end. The in-memory repositories store the two
  separately, so a failure between them can leave a step's records without its findings.
- The capping rounds are not stored; the log shows them at DEBUG level. The final traces are enough to check the
  weights: a capped security has exactly the cap and a capping factor below 1, an uncapped security has a capping factor
  of 1 and a weight proportional to its FFMCAP up to the rounding to 4 decimals and, for the last security, the
  rounding leftover, no weight is above the cap, and all weights add up to exactly 100%. See
  [Weighting and capping](methodology.md#weighting-and-capping).

## API

| Endpoint                                             | Returns                                              |
|------------------------------------------------------|------------------------------------------------------|
| `GET /v1/reviews`                                    | Audit parts of stored reviews, newest first          |
| `GET /v1/reviews/{reviewId}/audit`                   | Identity, status and the [audit part](#audit-part)   |
| `GET /v1/reviews/{reviewId}/trace`                   | Identity, status and the [trace part](#trace-part)   |
| `GET /v1/reviews/{reviewId}/securities/{securityId}` | Identity, status and the trace part for one security |

## Current limits

- Audit records and traces are kept in memory. They are lost when the application stops, and their number is not
  limited.
- The user who ran a review is not recorded, because user management is out of scope.
