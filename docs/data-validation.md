# Data validation

How the review checks its input data before it calculates FFMCAP.

File problems, such as a missing file, a missing CSV header or a value that cannot be parsed, are rejected before the
review starts, with HTTP 400. They are not stored as reviews. See [Request errors](#request-errors).

The checks run in `ReviewDataValidator` (whole input) and `SecurityRecordValidator` (one security). Each problem gives
a finding with a code, a severity, the security id, the field and a message. The findings are stored in the
[trace](traceability-and-auditability.md#findings).

## Severities

The full list of finding codes is in [Traceability](traceability-and-auditability.md#finding-codes).

| Severity  | Meaning                                     | Effect                                                   |
|-----------|---------------------------------------------|----------------------------------------------------------|
| `ERROR`   | The data would make the result wrong        | Review stops, status `FAILED`, HTTP 422 with empty lists |
| `WARNING` | The data is handled the same way every time | Security excluded or duplicate counted once; review runs |
| `INFO`    | Data that was not needed                    | None                                                     |

The review runs in stages and stops after the first stage with an error. Within a stage every row is checked, so
all errors of that stage are reported, not only the first one. For example, a composition with the wrong size stops
the review before any security data row is checked.

Excluding a security with missing data is an assumption: the rulebook would use the latest available data (section
7.1), but the files only hold the two review dates. See
[Assumptions and design decisions](design-decisions.md#data). Only rows of universe members are checked, so data of
other securities, even conflicting rows, cannot fail the review.

## Checks

The cut-off date row gives the price. The review date row gives the shares and the free float.

| Check                                                             | Code                                 | Severity |
|-------------------------------------------------------------------|--------------------------------------|----------|
| Universe has no members on the review date                        | `MISSING_DATE`                       | Error    |
| Security data has no rows for the cut-off or the review date      | `MISSING_DATE`                       | Error    |
| Composition does not have 20 different securities                 | `COMPOSITION_SIZE`                   | Error    |
| Fewer than 20 securities have a valid FFMCAP                      | `TOO_FEW_ELIGIBLE`                   | Error    |
| Price is 0 or negative                                            | `INVALID_VALUE`                      | Error    |
| Shares are missing, 0 or negative                                 | `INVALID_VALUE`                      | Error    |
| Free float is missing, 0, negative or above 1                     | `INVALID_VALUE`                      | Error    |
| A value has more than 20 integer digits or 20 decimals            | `INVALID_VALUE`                      | Error    |
| Two different rows for one universe member and date               | `CONFLICTING_DUPLICATE`              | Error    |
| No price on the cut-off date: security excluded                   | `MISSING_CUTOFF_PRICE`               | Warning  |
| No row on the review date: security excluded                      | `MISSING_REVIEW_DATA`                | Warning  |
| Two identical rows for one universe member and date: counted once | `DUPLICATE_ROW`                      | Warning  |
| A current component is not in the universe: it leaves             | `EXISTING_COMPONENT_NOT_IN_UNIVERSE` | Warning  |
| Security data has securities outside the universe: ignored        | `IGNORED_SECURITIES`                 | Info     |

Notes:

- One row can have several errors, for example shares of 0 and a free float of 1.5.
- Duplicates are compared by value, so `0.5` and `0.50` are equal.
- A security with an `INVALID_VALUE`, `CONFLICTING_DUPLICATE`, `MISSING_CUTOFF_PRICE`, `MISSING_REVIEW_DATA` or
  `EXISTING_COMPONENT_NOT_IN_UNIVERSE` finding gets the decision `NOT_ELIGIBLE` in the trace. An identical duplicate
  (`DUPLICATE_ROW`) is counted once and the security stays eligible.
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

## Request errors

A rejected request returns an error body with one entry per problem: `code`, `field`, `value` and `message`. These
requests are not reviews and are not stored.

| Code                   | HTTP | Meaning                                                                            |
|------------------------|------|------------------------------------------------------------------------------------|
| `MISSING_FILE`         | 400  | A file part (`universe`, `securityData`, `composition`) is missing                 |
| `INVALID_FILE`         | 400  | A file is empty, not a `.csv`, or its content cannot be read (header, value, line) |
| `MISSING_VALUE`        | 400  | A `configRequest` value is set to `null`                                           |
| `INVALID_VALUE`        | 400  | A value has the wrong type or format: date, `indexType`, review id, `csvSeparator` |
| `INVALID_DATE_RANGE`   | 400  | The cut-off date is after the review date                                          |
| `MALFORMED_JSON`       | 400  | `configRequest` is not valid JSON                                                  |
| `INVALID_REQUEST`      | 4xx  | Any other rejected request, for example a file above the size limit (413)          |
| `INDEX_NOT_CONFIGURED` | 422  | The `indexType` has no review configured                                           |
| `NOT_FOUND`            | 404  | Unknown review id, or a security that is not part of the review                    |
| `INTERNAL_ERROR`       | 500  | Unexpected error; details are in the server log                                    |
