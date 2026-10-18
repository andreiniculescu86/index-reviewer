# Traceability and audit

Every stored review answers two questions:

- **Traceability:** why does the result look like this? Every number goes back to its input values.
- **Auditability:** what did the review use: when it ran, which application version and which rules?

Each review is stored in two parts:

| Part                 | Contains                                                         | Stored             |
|----------------------|------------------------------------------------------------------|--------------------|
| [Audit](#audit-part) | Id, index, dates, [status](#review-status), time, version, rules | Once, when it ends |
| [Trace](#trace-part) | One entry per security, and all findings                         | After each step    |

Stored data is never changed. Failed reviews and reviews with an unexpected error are stored too.

**Example review on this page:** the assignment files, with the cut-off price of security 10 changed to `-2080`. It
failed after the FFMCAP step. Id: `51b43dce-e920-4416-80f8-50f846df339a`.

## Audit part

`GET /v1/reviews/{reviewId}/audit`

| Field         | Meaning                                                                            |
|---------------|------------------------------------------------------------------------------------|
| `status`      | How the review ended, see [Review status](#review-status)                          |
| `executedAt`  | When the review started, in UTC                                                    |
| `appVersion`  | Application version                                                                |
| `rules`       | Number of components, direct selection rank, buffer rank, cap, methodology version |
| `composition` | New composition, ids in rank order; empty when `FAILED` or `ERROR`                 |

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

`GET /v1/reviews` returns the reports of all stored reviews, newest first, in the same model as
`GET /v1/reviews/{reviewId}`. The audit part of each review stays available from its audit endpoint.

## Trace part

`GET /v1/reviews/{reviewId}/trace`, or for one security `GET /v1/reviews/{reviewId}/securities/{securityId}`.

### Security trace

One entry for every security in the universe and every current component.

| Field               | Meaning                                                                                 |
|---------------------|-----------------------------------------------------------------------------------------|
| `securityId`        | The security                                                                            |
| `existingComponent` | In the current index                                                                    |
| `price`             | Price on the cut-off date                                                               |
| `shares`            | Number of shares on the review date                                                     |
| `freeFloat`         | Free float on the review date                                                           |
| `ffmcap`            | `price x shares x freeFloat`; empty when not eligible                                   |
| `rank`              | Rank by FFMCAP, 1 for the largest, see [ranking](methodology.md#ranking)                |
| `decision`          | [Selection decision](#selection-decisions)                                              |
| `cappingFactor`     | 1 if not capped, below 1 if capped, see [capping](methodology.md#weighting-and-capping) |
| `weight`            | Final weight in percent, `18.0000` for 18%                                              |

A value is `null` when its step did not run.

**Example:** the FFMCAP of security 249.

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

`857.4 x 26700000 x 0.8486 = 19426643388`. The free float comes from the review date row; the cut-off row has
`0.8487`, which is not used. `rank` and `decision` are empty because the review stopped before the selection.

### Selection decisions

See [Selection with the buffer](methodology.md#selection-with-the-buffer).

| Decision                             | Meaning                                   |
|--------------------------------------|-------------------------------------------|
| `SELECTED_DIRECT`                    | Rank 1 to 18                              |
| `SELECTED_BUFFER_EXISTING_COMPONENT` | Rank 19 to 22, already in the index, kept |
| `SELECTED_BUFFER_NEWCOMER`           | Rank 19 to 22, new, took a free seat      |
| `NOT_SELECTED_BUFFER`                | Rank 19 to 22, no seat left               |
| `NOT_SELECTED_BELOW_BUFFER`          | Rank 23 or lower                          |
| `NOT_ELIGIBLE`                       | No valid FFMCAP; the findings explain why |

**Example:** why is security 166 not eligible?
`GET /v1/reviews/51b43dce-e920-4416-80f8-50f846df339a/securities/166`:

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

### Findings

A finding is one problem found in the data, the selection or the weighting. Data checks are in
[Data validation](data-validation.md).

| Code                                 | Severity | Meaning                                                          |
|--------------------------------------|----------|------------------------------------------------------------------|
| `MISSING_DATE`                       | ERROR    | No rows for a required date                                      |
| `COMPOSITION_SIZE`                   | ERROR    | The current composition does not have 20 securities              |
| `TOO_FEW_ELIGIBLE`                   | ERROR    | Fewer than 20 securities have a valid FFMCAP                     |
| `INVALID_VALUE`                      | ERROR    | Price, shares or free float missing, zero, negative or too large |
| `CONFLICTING_DUPLICATE`              | ERROR    | Two rows for the same security and date differ                   |
| `SELECTION_SIZE`                     | ERROR    | The selection does not have exactly 20 components                |
| `INVALID_WEIGHTS`                    | ERROR    | A weight exceeds the cap                                         |
| `MISSING_CUTOFF_PRICE`               | WARNING  | No price on the cut-off date; not eligible                       |
| `MISSING_REVIEW_DATA`                | WARNING  | No row on the review date; not eligible                          |
| `DUPLICATE_ROW`                      | WARNING  | Two identical rows; counted once                                 |
| `EXISTING_COMPONENT_NOT_IN_UNIVERSE` | WARNING  | A current component is not in the universe; it leaves            |
| `SELECTION_TIE`                      | WARNING  | A tie decided who is selected                                    |
| `IGNORED_SECURITIES`                 | INFO     | Rows for securities outside the universe                         |

**Example:** the error that made the example review fail.

```json
{
  "code": "INVALID_VALUE",
  "severity": "ERROR",
  "securityId": 10,
  "field": "price",
  "message": "Security 10: Price must be positive but is -2080"
}
```

## Review status

| Status                  | Meaning                                                  |
|-------------------------|----------------------------------------------------------|
| `SUCCESS`               | No errors, no warnings                                   |
| `SUCCESS_WITH_WARNINGS` | Warnings only; the result is correct                     |
| `FAILED`                | At least one error; no result                            |
| `ERROR`                 | Unexpected error; the trace shows how far the review got |

`FAILED` and `ERROR` reviews have empty lists in their report.

The example review is `FAILED`: one error (security 10) and one warning (security 166).

## Limits

- Reviews are kept in memory and lost when the application stops.
- Input files are not stored, so a review cannot be repeated from the stored data.
- The capping rounds are not stored; the log shows them at DEBUG level. The final weights and capping factors are
  enough to check the result.
- A review is visible only after it ends. While it runs, its id returns 404.
- The user who ran a review is not stored; user management is out of scope.
