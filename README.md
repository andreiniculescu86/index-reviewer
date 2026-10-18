# Index Reviewer

Spring Boot REST API that runs the ordinary SMI index review for Q3 2026, following the SIX Swiss Index Methodology
Rulebook (section 5.12).

A review takes 3 CSV files: the SPI universe, the security data and the current SMI composition. It then:

1. validates the data and records every problem as an error or a warning;
2. calculates the free float market capitalization (FFMCAP) of every eligible security, with the price of the
   cut-off date (2026-09-10) and the shares and free float of the review date (2026-09-21);
3. ranks the securities by FFMCAP and selects the 20 components, ranks 1 to 18 directly and ranks 19 to 22 through
   the buffer, where current components keep their seats first;
4. weights the components by FFMCAP and caps every weight at 18% with a capping factor.

The response is the review report: the final constituents with their weights and capping factors, the joiners, the
leavers, the review status and the findings behind that status. Every review is stored with an audit record and a
trace per security, so its result can be explained afterwards.

The assignment, its requirements and the CSV files are in
[docs/assignment-requirements-SIX](docs/assignment-requirements-SIX).

## Requirements

- Java 21 or newer
- No Maven installation needed: the Maven wrapper downloads Maven 3.9.12. Use `./mvnw` on macOS and Linux, and
  `mvnw.cmd` on Windows.

## Build and run

Build the application and run the tests and Checkstyle.

```bash
./mvnw verify
```

Start the application on port 8096.

```bash
java -jar target/index-reviewer-0.1.0-SNAPSHOT.jar
```

Swagger UI is also available: http://localhost:8096/. It documents every endpoint and can upload the files.

## Run a review

Either use the Swagger UI or the command line. RECOMMENDATION: use Swagger UI at http://localhost:8096/
Send the 3 files as multipart form data. The input files of the assignment are in
[docs/assignment-requirements-SIX](docs/assignment-requirements-SIX).

Set `DATA_DIR` to the folder with your 3 CSV files, absolute or relative to where you run curl. For the assignment
files, from the project root: `DATA_DIR="docs/assignment-requirements-SIX"`.

```bash
DATA_DIR="/path/to/folder"
```

```bash
curl -F "universe=@$DATA_DIR/spi_universe.csv" \
  -F "securityData=@$DATA_DIR/sec_data.csv" \
  -F "composition=@$DATA_DIR/composition.csv" \
  http://localhost:8096/v1/reviews
```

Each `-F name=@file` uploads one file as the form part `name`. The file names can differ; the part names
`universe`, `securityData` and `composition` must stay.

The optional `configRequest` part sets the index, the dates and the CSV separator. Without it, the review uses SMI,
cut-off date 2026-09-10, review date 2026-09-21 and `;`. Currently, only SMI is supported.

```bash
curl -F 'configRequest={"indexType":"SMI","cutoffDate":"2026-09-10","reviewDate":"2026-09-21"}' \
  -F "universe=@$DATA_DIR/spi_universe.csv" \
  -F "securityData=@$DATA_DIR/sec_data.csv" \
  -F "composition=@$DATA_DIR/composition.csv" \
  http://localhost:8096/v1/reviews
```

The response is the review report:

| Field          | Meaning                                                                                                                                     |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `reviewId`     | Id of the stored review, used by the other endpoints                                                                                        |
| `status`       | `SUCCESS`, `SUCCESS_WITH_WARNINGS`, `FAILED` or `ERROR`, see [Review status](docs/traceability-and-auditability.md#review-status)           |
| `constituents` | Final components in rank order, with `weight` and `cappingFactor`                                                                           |
| `joiners`      | Ids that enter the index                                                                                                                    |
| `leavers`      | Ids that leave the index                                                                                                                    |
| `findings`     | Errors and warnings found in the data, which explain the `status`, see [finding codes](docs/traceability-and-auditability.md#finding-codes) |

HTTP status: `201` when the review ran, `422` when it failed on data errors (status `FAILED`, the findings, empty
composition lists), `400` when a file or a value of the request is invalid.

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

## Documentation

- [Methodology](docs/methodology.md): dates, FFMCAP, ranking, buffer, capping and the assumptions made.
- [Data validation](docs/data-validation.md): data checks, errors and warnings.
- [Traceability and audit](docs/traceability-and-auditability.md): what each review stores and how to read it.
- [Assumptions and design decisions](docs/design-decisions.md): every assumption made where the assignment is open,
  and the technical decisions with their alternatives.
- [API reference](docs/api.md): endpoints, request parts, response models and HTTP statuses.
- [Architecture](docs/architecture.md): what the service does, its external interactions and the main review flows.
- [AI setup](docs/README-AI.md): the Claude rules, skills and agents used to build the project. They are kept on
  purpose, to show how the AI tools were set up.

## Next steps

- All input is read into memory at once, because the data is spread across 3 files. For large inputs, read the data
  from a database or an API, or use files where each row holds all values of one security.
- Store reviews in a database instead of memory and store the input files so that a review can be repeated.
- Add observability, for example, OpenTelemetry for traces, metrics and logs.
