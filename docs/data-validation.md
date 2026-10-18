# Data validation

How the review checks its input data before it calculates FFMCAP.

File problems, such as a missing file, a missing CSV header or a value that cannot be parsed, are rejected before the
review starts, with HTTP 400. They are not stored as reviews.

The checks run in `ReviewDataValidator` (whole input) and `SecurityRecordValidator` (one security). Each problem gives
a finding with a code, a severity, the security id, the field and a message. The findings are stored in the
[trace](traceability-and-auditability.md#findings).

## Severities

| Severity  | Meaning                                     | Effect                                                   |
|-----------|---------------------------------------------|----------------------------------------------------------|
| `ERROR`   | The data would make the result wrong        | Review stops, status `FAILED`, HTTP 422 with empty lists |
| `WARNING` | The data is handled the same way every time | Security excluded or duplicate counted once; review runs |
| `INFO`    | Data that was not needed                    | None                                                     |

All rows are checked first, so every error is reported, not only the first one.

This follows the rulebook correction policy: missing data does not stop a review (section 7.1), wrong data is not
used (section 7.2).

## Checks

The cut-off date row gives the price. The review date row gives the shares and the free float.

| Check                                                           | Code                                 | Severity |
|-----------------------------------------------------------------|--------------------------------------|----------|
| Universe has no members on the review date                      | `MISSING_DATE`                       | Error    |
| Security data has no rows for the cut-off or the review date    | `MISSING_DATE`                       | Error    |
| Composition does not have 20 different securities               | `COMPOSITION_SIZE`                   | Error    |
| Fewer than 20 securities have a valid FFMCAP                    | `TOO_FEW_ELIGIBLE`                   | Error    |
| Price is 0 or negative                                          | `INVALID_VALUE`                      | Error    |
| Shares are missing, 0 or negative                               | `INVALID_VALUE`                      | Error    |
| Free float is missing, 0, negative or above 1                   | `INVALID_VALUE`                      | Error    |
| A value has more than 20 integer digits or 20 decimals          | `INVALID_VALUE`                      | Error    |
| Two rows for the same security and date with different values   | `CONFLICTING_DUPLICATE`              | Error    |
| No price on the cut-off date: security excluded                 | `MISSING_CUTOFF_PRICE`               | Warning  |
| No row on the review date: security excluded                    | `MISSING_REVIEW_DATA`                | Warning  |
| Two identical rows for the same security and date: counted once | `DUPLICATE_ROW`                      | Warning  |
| A current component is not in the universe: it leaves           | `EXISTING_COMPONENT_NOT_IN_UNIVERSE` | Warning  |
| Security data has securities outside the universe: ignored      | `IGNORED_SECURITIES`                 | Info     |

Notes:

- One row can have several errors, for example shares of 0 and a free float of 1.5.
- Duplicates are compared by value, so `0.5` and `0.50` are equal.
- A security with an error or a warning gets the decision `NOT_ELIGIBLE` in the trace.
- The digit limit keeps the FFMCAP calculation in range. The assignment data has at most 10 integer digits and 4
  decimals.

## Handled without a finding

- Leading and trailing spaces in values and headers are removed.
- Duplicate universe rows and duplicate composition ids are counted once.
- A price on a review date row is ignored.

## Assignment data

The review of the assignment files ends with `SUCCESS_WITH_WARNINGS`:

- security 166 has no review date row (`MISSING_REVIEW_DATA`) and is excluded;
- the universe lists 204 securities twice; they are counted once, without a finding.
