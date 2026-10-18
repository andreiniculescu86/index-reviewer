# Index Reviewer

REST API that runs the SMI index review for Q3 2026, as described in the assignment and the SIX methodology rulebook
(section 5.12). It reads the 3 CSV files, selects the 20 components, caps the weights at 18% and returns a report.
Assignment requirements and csv files are located in [docs](docs/assignment-requirements-SIX)

## Requirements

- Java 25
- Maven 3.9 or newer (the project has no Maven wrapper)

## Build and run

Build the application and run the tests and Checkstyle.

```bash
mvn verify
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

| Field          | Meaning                                                           |
|----------------|-------------------------------------------------------------------|
| `reviewId`     | Id of the stored review, used by the other endpoints              |
| `status`       | `SUCCESS`, `SUCCESS_WITH_WARNINGS`, `FAILED` or `ERROR`           |
| `constituents` | Final components in rank order, with `weight` and `cappingFactor` |
| `joiners`      | Ids that enter the index                                          |
| `leavers`      | Ids that leave the index                                          |
| `findings`     | Errors and warnings found in the data, which explain the `status` |

HTTP status: `201` when the review ran, `422` when it failed on data errors (status `FAILED`, the findings, empty
composition lists), `400` when a file or a value of the request is invalid.

## Endpoints

| Endpoint                                             | Returns                                           |
|------------------------------------------------------|---------------------------------------------------|
| `POST /v1/reviews`                                   | Runs a review and returns its report              |
| `GET /v1/reviews`                                    | Reports of stored reviews, newest first; filters  |
| `GET /v1/reviews/{reviewId}`                         | Report of a stored review                         |
| `GET /v1/reviews/{reviewId}/audit`                   | What the review used: dates, rules, version, time |
| `GET /v1/reviews/{reviewId}/trace`                   | Values, rank and decision per security, findings  |
| `GET /v1/reviews/{reviewId}/securities/{securityId}` | Trace of one security, e.g. why it left the index |

Filters of `GET /v1/reviews`: `indexType`, `cutoffDate`, `reviewDate`, for example
`/v1/reviews?indexType=SMI&cutoffDate=2026-09-10`.

`GET /v1/reviews` returns the same report model as `GET /v1/reviews/{reviewId}`.

To see why a review has warnings or failed, read its `findings`; the trace also lists them per security.

Reviews are kept in memory and are lost when the application stops.

## Tests

`mvn verify` runs the unit tests and the end-to-end tests in `ReviewEndToEndIT`. Each end-to-end test starts the
application, uploads one set of files from [src/test/resources/e2e](src/test/resources/e2e) and checks the HTTP
response, the stored report, audit and trace.

| Folder     | Scenario                                                                  | Expected                   |
|------------|---------------------------------------------------------------------------|----------------------------|
| scenario1  | Assignment files                                                          | Q3 2026 result             |
| scenario2  | 4 current components in the buffer compete for 2 seats                    | 2 kept, 2 leave            |
| scenario3  | Few large securities, capping needs 2 rounds, rounding leftover           | Capped weights             |
| scenario4  | Delisted component, component without review data, IPO after cut-off      | Leavers, joiners           |
| scenario5  | Free float sent in percent (85 instead of 0.85)                           | 422 `FAILED`               |
| scenario6  | Rows loaded twice, one with a changed value                               | 422 `FAILED`               |
| scenario7  | Request dates not in the files                                            | 422 `FAILED`               |
| scenario8  | Comma separated export with BOM, CRLF and spaces                          | 400, then 201 with `,`     |
| scenario9  | Truncated universe, fewer than 20 valid securities                        | 422 `FAILED`               |
| scenario10 | Composition with one id twice                                             | 422 `FAILED`               |
| scenario11 | Shares and free float change between the dates; review rows carry a price | Values from the right date |
| scenario12 | Equal FFMCAP written differently (50 vs 50.00) at the last buffer seat    | Tie order, `SELECTION_TIE` |
| scenario13 | FFMCAPs that differ only after the 15th digit decide rank 18              | Exact ranking              |
| scenario14 | Very concentrated index, capping over 3 rounds, 4 capped                  | Capped weights             |
| scenario15 | Security data for the whole market, incl. larger and broken non-SPI rows  | Ignored, `SUCCESS`         |

## Documentation

- [Methodology](docs/methodology.md): dates, FFMCAP, ranking, buffer, capping and the assumptions made.
- [Data validation](docs/data-validation.md): data checks, errors and warnings.
- [Traceability and audit](docs/traceability-and-auditability.md): what each review stores and how to read it.
- [Design decisions](docs/design-decisions.md): technical choices, the alternatives and why.
- [AI setup](docs/README-AI.md): the Claude rules, skills and agents used to build the project. They are kept on
  purpose, to show how the AI tools were set up.

## Next steps

- All input is read into memory at once, because the data is spread across 3 files. For large inputs, read the data
  from a database or an API, or use files where each row holds all values of one security.
- Store reviews in a database instead of memory and store the input files so that a review can be repeated.
- Add observability, for example, OpenTelemetry for traces, metrics and logs.
