# Design decisions

The technical choices behind the Index Reviewer, the alternatives that were considered and why each choice was made.
The business assumptions of the review itself, such as capping rounds, rounding and ties, are in
[methodology.md](methodology.md).

## API

### One multipart request per review

`POST /v1/reviews` takes the 3 CSV files and an optional `configRequest` part in one multipart request.

| Alternative                                                 | Why not                                            |
|-------------------------------------------------------------|----------------------------------------------------|
| Upload market data, universe and composition once, then run | More endpoints and stored inputs; not needed for   |
| reviews that reference them by id                           | one review per request                             |
| JSON body with all rows                                     | The input is given as CSV; clients would have to   |
|                                                             | convert it                                         |

The files stay in the format of the assignment. The request adds what the files do not say: the index and the dates.
The internal model already keeps universe, security data and composition apart, so separate uploads can be added later
without changing the review logic.

### One report model for a review

`POST /v1/reviews`, `GET /v1/reviews/{reviewId}` and `GET /v1/reviews` return the same `ReviewResponse`: constituents,
joiners, leavers, status and findings. The list returns the same resource as the single read, so a client handles one
model. The audit data and the trace are separate sub-resources (`/audit`, `/trace`, `/securities/{securityId}`),
because they answer different questions and are much larger.

### HTTP status

| Status | When                                                 | Why                                                  |
|--------|------------------------------------------------------|------------------------------------------------------|
| `201`  | The review ran, with or without warnings             | A review execution was created and stored            |
| `422`  | The review ran but failed on data errors             | The request was valid, the data was not; the stored  |
|        |                                                      | report and its findings are returned                 |
| `400`  | A file is missing or not CSV, a value or the dates   | The request itself is invalid; nothing is stored     |
|        | are invalid                                          |                                                      |

A data error is a review outcome, not a request error, so a failed review is stored and traceable like a successful one.

## Review flow

### A fixed pipeline of steps per index

`SmiReviewer` runs validation, FFMCAP calculation, selection and weighting in order. Each step returns its result,
trace records and findings; the reviewer stores them in the trace before the next step starts. A step with an error
ends the review.

| Alternative                                   | Why not                                                          |
|-----------------------------------------------|------------------------------------------------------------------|
| Generic rule engine or configurable pipeline  | More abstraction than one index needs (KISS, YAGNI)              |
| One service method doing everything           | Steps could not be tested or reused separately                   |

The steps take the index rules as parameters (`IndexRules`: size, direct selection rank, buffer rank, cap), so they
are not tied to the SMI values.

### Index-specific reviewers behind one interface

`ReviewService` picks the `IndexReviewer` implementation by `IndexType`. Adding an index means adding an
implementation; the controller and `ReviewService` stay unchanged.

### Rules in configuration

The SMI values (20 components, direct selection up to rank 18, buffer up to rank 22, cap 18%, rulebook version) are in
`application.yml` and bound to `SmiConfiguration`. Bean Validation checks them at startup, including that the ranks
are in order and that the cap can be met (`components x cap >= 100`). A wrong configuration stops the application
instead of producing a wrong review.

### Review dates from the request

The cut-off and review dates come from the request, with the Q3 2026 dates as defaults. Another review period needs
new data files and dates, not a code change.

## Calculation

### `BigDecimal` for all amounts

FFMCAP, weights and capping factors use `BigDecimal`. `double` would add binary rounding errors to values in the
hundreds of billions and make the results hard to compare with expected values. Weights and capping factors are
rounded half up to 4 decimals; FFMCAP is not rounded.

### Capping recalculated from FFMCAP in each round

Each capping round calculates the uncapped weights again from FFMCAP instead of adding redistributed weight to the
previous round. The result equals the proportional redistribution of the assignment example, and rounding errors do
not add up over rounds. The formulas are in [methodology.md](methodology.md#weighting-and-capping).

## Data validation

### Findings with severities instead of exceptions

Data problems become findings with a code, a severity, the security id, the field and a message. An `ERROR` fails the
review, a `WARNING` is handled the same way every time and recorded, an `INFO` changes nothing. The review status
follows from the most severe finding.

| Alternative                       | Why not                                                                   |
|-----------------------------------|---------------------------------------------------------------------------|
| Throw on the first problem        | The client sees one problem at a time and nothing is traceable            |
| Ignore problems the code can skip | Results would change silently                                             |

Corrupt rows are errors, not warnings: a skipped row could belong to a top 20 security. The checks are listed in
[data-validation.md](data-validation.md).

### Format checks at the API boundary, data checks in the service

Missing files, wrong file types, the CSV separator and the date order are checked on the request and return `400`.
Content checks that need all files together, such as missing review-date data, run in the service and become findings.

## Traceability and audit

### Trace stored step by step

The trace is written after every step, so a failed review keeps everything calculated before the error. The report of
a stored review is derived again from its audit record and trace, so the report and its explanation cannot disagree.

### In-memory storage behind repository interfaces

`ReviewAuditRepository` and `ReviewTraceRepository` have in-memory implementations based on `ConcurrentHashMap`. A
database is not needed for the assignment and would add setup for the reviewer. Stored reviews are lost on restart;
the interfaces allow a database implementation without changing the services.

## Libraries

| Library            | Use                                         | Alternative considered                           |
|--------------------|---------------------------------------------|--------------------------------------------------|
| Spring Boot WebMVC | REST API, validation, configuration binding | None; the standard choice for a Java REST API    |
| OpenCSV            | Reading CSV into typed rows                 | Hand-written parsing: more code for quoting,     |
|                    |                                             | separators and type conversion errors            |
| MapStruct          | DTO to model mapping                        | Manual mappers: repetitive and easy to get wrong |
| Lombok             | Value classes and builders                  | Records only: no builders or `@Singular` lists   |
| springdoc-openapi  | Swagger UI to run reviews without curl      | Static API description: would go out of date     |

## Known limits

- The steps live in the `smi` package although they only depend on `IndexRules`. A second index would move them to a
  shared package.
- `IndexRules` describes one buffer and one cap for all components. An index with a tiered cap, such as the SLI
  (9% for the 4 largest, 4.5% for the rest), needs another capping step.
- Issuer aggregation, the liquidity rule and the 12-month selection list are not implemented; see
  [methodology.md](methodology.md#not-implemented).
