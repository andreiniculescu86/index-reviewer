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

## Documentation

- [Methodology](docs/methodology.md): dates, FFMCAP, ranking, buffer, capping and the assumptions made.
- [Data validation](docs/data-validation.md): data checks, errors and warnings.
- [Traceability and audit](docs/traceability-and-auditability.md): what each review stores and how to read it.
- [Development](docs/development.md): build checks and code quality tools.
- [AI setup](docs/README-AI.md): the Claude rules, skills and agents used to build the project. They are kept on
  purpose, to show how the AI tools were set up.

## Next steps

- All input is read into memory at once, because the data is spread across 3 files. For large inputs, read the data
  from a database or an API, or use files where each row holds all values of one security.
- Store reviews in a database instead of memory and store the input files so that a review can be repeated.
- Add observability, for example, OpenTelemetry for traces, metrics and logs.
