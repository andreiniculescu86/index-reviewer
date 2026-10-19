# Index Reviewer

Spring Boot REST API that runs the ordinary SMI index review for Q3 2026, following the SIX Swiss Index Methodology
Rulebook (section 5.12).

A review takes 3 CSV files: the SPI universe, the security data and the current SMI composition. It then:

1. validates the data and records every problem as an error or a warning;
2. calculates the free float market capitalization (FFMCAP) of every eligible security, with the price of the
   cut-off date (2026-09-10) and the shares and free float of the review date (2026-09-21); Dates are configurable if
   these are present in the CSV files.
3. ranks the securities by FFMCAP and selects the 20 components, ranks 1 to 18 directly and ranks 19 to 22 through
   the buffer, where current components keep their seats first;
4. weights the components by FFMCAP and caps every weight at 18% with a capping factor.

The response is the review report: the final constituents with their weights and capping factors, the joiners, the
leavers, the review status and the findings behind that status. Every review is stored with an audit record and a
trace per security, so its result can be explained afterwards.

The assignment, its requirements and the CSV files are in
[docs/assignment-requirements-SIX](docs/assignment-requirements-SIX).

Note: The Markdown documentation is structured primarily to support the review of this home assignment.
In a production project, some of this information would be organized and maintained differently.

## Contents

- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [Run a review](#run-a-review)
  - [Expected result](#expected-result)
  - [Index, dates and separator](#index-dates-and-separator)
  - [Response fields](#response-fields)
- [Endpoints](#endpoints)
- [Tests](#tests)
- [Documentation](#documentation)
  - [Methodology](#methodology)
  - [Data validation](#data-validation)
  - [Traceability and audit](#traceability-and-audit)
  - [Assumptions and design decisions](#assumptions-and-design-decisions)
  - [API reference](#api-reference)
  - [Architecture](#architecture)
  - [AI setup](#ai-setup)
- [Next steps](#next-steps)

## Requirements

- Java 21 or newer
- No Maven installation needed: the Maven wrapper downloads Maven 3.9.12. Use `./mvnw` on macOS and Linux, and
  `mvnw.cmd` on Windows.

## Build and run

Build the application and run all [tests](#tests), the coverage report and Checkstyle.

```bash
./mvnw verify
```

Start the application on port 8096.

```bash
java -jar target/index-reviewer-0.1.0-SNAPSHOT.jar
```

Swagger UI is also available: http://localhost:8096/. It documents every endpoint and can upload the files.

## Run a review

Use the Swagger UI at http://localhost:8096/ (easiest) or the command line.
Send the 3 files as multipart form data. The input files of the assignment are in
[docs/assignment-requirements-SIX](docs/assignment-requirements-SIX).

From the project root, this runs the review on the assignment files:

```bash
DATA_DIR="docs/assignment-requirements-SIX"
```

```bash
curl -F "universe=@$DATA_DIR/spi_universe.csv" \
  -F "securityData=@$DATA_DIR/sec_data.csv" \
  -F "composition=@$DATA_DIR/composition.csv" \
  http://localhost:8096/v1/reviews
```

Each `-F name=@file` uploads one file as the form part `name`. The file names can differ; the part names
`universe`, `securityData` and `composition` must stay. For your own files, set `DATA_DIR` to their folder, absolute
or relative to where you run curl.

### Expected result

For the assignment files the review returns `201` with this report (shortened):

```json
{
  "reviewId": "50114c22-846f-454a-a347-3f8af29ba35c",
  "status": "SUCCESS_WITH_WARNINGS",
  "constituents": [
    {
      "securityId": 155,
      "weight": 18.0000,
      "cappingFactor": 0.5653526015
    },
    {
      "securityId": 205,
      "weight": 18.0000,
      "cappingFactor": 0.6117362279
    },
    {
      "securityId": 63,
      "weight": 14.2591,
      "cappingFactor": 1.0000000000
    },
    {
      "securityId": 81,
      "weight": 0.8503,
      "cappingFactor": 1.0000000000
    }
  ],
  "joiners": [
    177
  ],
  "leavers": [
    103
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

- 20 constituents in rank order; 155 and 205 would weigh 25.42% and 23.49%, so both are capped at 18%.
- Security 177 joins and security 103 leaves.
- `SUCCESS_WITH_WARNINGS` is expected: security 166 is in the universe but has no review-date row in
  `sec_data.csv`, so it is excluded with a warning. The result itself is complete.

### Index, dates and separator

The optional `configRequest` part sets the index, the dates and the CSV separator. Without it, the review uses SMI,
cut-off date 2026-09-10, review date 2026-09-21 and `;`. Currently, only SMI is configured; other indices of the
same shape are added by configuration, see [Adding an index](docs/methodology.md#adding-an-index).

```bash
curl -F 'configRequest={"indexType":"SMI","cutoffDate":"2026-09-10","reviewDate":"2026-09-21"}' \
  -F "universe=@$DATA_DIR/spi_universe.csv" \
  -F "securityData=@$DATA_DIR/sec_data.csv" \
  -F "composition=@$DATA_DIR/composition.csv" \
  http://localhost:8096/v1/reviews
```

### Response fields

The response is the review report:

| Field          | Meaning                                                                                                                                     |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `reviewId`     | Id of the stored review, used by the other endpoints                                                                                        |
| `status`       | `SUCCESS`, `SUCCESS_WITH_WARNINGS`, `FAILED` or `ERROR`, see [Review status](docs/traceability-and-auditability.md#review-status)           |
| `constituents` | Final components in rank order, with `weight` and `cappingFactor`                                                                           |
| `joiners`      | Ids that enter the index                                                                                                                    |
| `leavers`      | Ids that leave the index                                                                                                                    |
| `findings`     | Errors and warnings found in the data, which explain the `status`, see [finding codes](docs/traceability-and-auditability.md#finding-codes) |

The HTTP status code says whether the request worked; the report's `status` says whether the index calculation
worked:

| Situation when running a review                 | HTTP  | Report `status`                      |
|-------------------------------------------------|-------|--------------------------------------|
| Index calculated                                | `201` | `SUCCESS` or `SUCCESS_WITH_WARNINGS` |
| Bad, inconsistent or incomplete data            | `422` | `FAILED`; the findings say why       |
| The application itself failed                   | `500` | no report; stored as `ERROR`         |
| Request invalid: missing file, bad value, dates | `400` | no report; nothing stored            |

Reading a stored review with `GET` always returns `200`, also for a `FAILED` or `ERROR` review. Details and examples
are in [Review status and HTTP status](docs/api.md#review-status-and-http-status).

## Endpoints

The request and response models of each endpoint are described in the [API reference](docs/api.md).

| Endpoint                                                             | Returns                                       |
|----------------------------------------------------------------------|-----------------------------------------------|
| [`POST /v1/reviews`][api-run]                                        | Runs a review and returns its report          |
| [`GET /v1/reviews`][api-list]                                        | Stored reviews, newest first; filters         |
| [`GET /v1/reviews/{reviewId}`][api-get]                              | Report of one stored review                   |
| [`GET /v1/reviews/{reviewId}/audit`][api-audit]                      | What the review used: dates, rules, version   |
| [`GET /v1/reviews/{reviewId}/trace`][api-trace]                      | Values, rank, decision per security; findings |
| [`GET /v1/reviews/{reviewId}/securities/{securityId}`][api-security] | Trace of one security, e.g. why it left       |

[api-run]: docs/api.md#run-a-review

[api-list]: docs/api.md#list-reviews

[api-get]: docs/api.md#get-a-review

[api-audit]: docs/api.md#get-the-audit-record

[api-trace]: docs/api.md#get-the-trace

[api-security]: docs/api.md#get-one-security

To see why a review has warnings or failed, read its `findings` field from the /review response; the trace also lists
them per security ID.

Reviews are kept in memory and are lost when the application stops.

## Tests

`./mvnw verify` runs everything below and fails the build on any failed test or Checkstyle violation.

| Check                 | What it covers                                  | Report                          |
|-----------------------|-------------------------------------------------|---------------------------------|
| Unit tests (208)      | 18 test classes, one part at a time             | `target/surefire-reports`       |
| End-to-end tests (24) | 24 scenarios, each a real HTTP call to the app  | `target/failsafe-reports`       |
| Coverage (JaCoCo)     | All tests: 93% of instructions, 80% of branches | `target/site/jacoco/index.html` |
| Checkstyle            | Main and test code, `checkstyle_rules.xml`      | `target/checkstyle-result.xml`  |

**Unit tests (208)** check each part on its own, with small inputs whose expected values are easy to verify by hand:

- the review steps: FFMCAP from the right dates, ranking and buffer selection with ties, capping in rounds and the
  rounding of weights;
- the data checks: the errors and warnings in [Data validation](docs/data-validation.md);
- the API: request validation, status codes, error responses and mapping;
- the CSV reader (separators, BOM, line endings, broken files) and the review store.

**End-to-end tests (24)** start the application, upload one set of CSV files from
[src/test/resources/e2e](src/test/resources/e2e) and check the HTTP response and the stored report, audit record and
trace. Each scenario is a case that can happen with real input:

| Area                       | Scenarios             | Examples                                            |
|----------------------------|-----------------------|-----------------------------------------------------|
| Assignment data            | 1                     | Q3 2026: joiner 177, leaver 103, 155 and 205 at 18% |
| Selection and buffer       | 2, 16 to 21           | All 20 members leave, buffer seats, ties            |
| Weighting and capping      | 3, 14, 22             | Capping over several rounds, no capping             |
| Data changes between dates | 4, 11, 12, 13, 15, 23 | Delisting, IPO, member without data                 |
| Data errors (`422`)        | 5, 6, 7, 9, 10, 24    | Free float in percent, conflicting rows             |
| File format                | 8                     | Comma separator, BOM, Windows line endings          |

## Documentation

### Methodology

- [Methodology (`methodology.md`)](docs/methodology.md): dates, FFMCAP, ranking, buffer, capping and the assumptions
  made.

### Data validation

- [Data validation (`data-validation.md`)](docs/data-validation.md): data checks, errors and warnings.

### Traceability and audit

- [Traceability and audit (`traceability-and-auditability.md`)](docs/traceability-and-auditability.md): what each review
  stores and how to read it.

### Assumptions and design decisions

- [Assumptions and design decisions (`design-decisions.md`)](docs/design-decisions.md): every assumption made where the
  assignment is open, and the technical decisions with their alternatives.

### API reference

- [API reference (`api.md`)](docs/api.md): endpoints, request parts, response models and HTTP statuses.

### Architecture

- [Architecture (`architecture.md`)](docs/architecture.md): what the service does, its external interactions and the
  main review flows.

### AI setup

- [AI setup (`README-AI.md`)](docs/README-AI.md): the Claude rules, skills and agents used to build the project. They
  are kept on purpose, to show how the AI tools were set up.

## Next steps

- All input is read into memory at once, because the data is spread across 3 files. For large inputs, read the data
  from a database or an API, or use files where each row holds all values of one security.
- Store reviews in a database instead of memory and store the input files so that a review can be repeated.
- Add observability, for example, OpenTelemetry for traces, metrics and logs.
