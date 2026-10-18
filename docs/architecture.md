# Application architecture

The Index Reviewer is a REST service that runs an ordinary index review. A client uploads the input data as CSV
files, the service selects the index components, weights and caps them, and returns a review report: the final
constituents with their weighting factors, the joiners, the leavers and the review status. Every review is stored, so
that its result and the reasons behind it can be read again later. The SMI is the only configured index.

## Context

```text
  CSV files: SPI universe,
  security data, current
  composition
        |
        | uploaded by the user
        v
+---------------------+   runs a review: POST /v1/reviews     +-------------------------------------+
| API client          | ------------------------------------> | Index Reviewer                      |
| curl or Swagger UI  |   reads reviews: GET /v1/reviews/...  | Spring Boot REST API                |
|                     | ------------------------------------> |                                     |
|                     |                                       |   index rules:  application.yml     |
|                     | <------------------------------------ |   review store: in memory           |
+---------------------+   report, audit, trace                +-------------------------------------+
                                                                                 |
                                                                                 | structured logs (ECS)
                                                                                 v
                                                                    logs/index-reviewer.json
```

The service has no database, message broker or upstream system. All input of a review arrives in the request, and
the stored reviews live in the memory of the running instance.

## Flow 1: run a review

A review runs synchronously inside the request. It passes through four stages; each stage records its results and
findings in the review trace before the next one starts. A stage that finds a data error stops the review, so a
failed review still shows how far it got.

```text
POST /v1/reviews: 3 CSV files, optional index and dates
        |
        v
Files readable and values valid? ---- no ----> 400 Bad Request, nothing stored
        |
        | yes
        v
+---------------------------------------------+
| 1. Check the input                          |
|    dates present, composition complete      | ----+
+---------------------------------------------+     |
        |                                           |
        v                                           |
+---------------------------------------------+     |
| 2. Calculate FFMCAP per universe security   | ----+
+---------------------------------------------+     |
        |                                           |   data error
        v                                           +------------> Store the audit record, status FAILED
+---------------------------------------------+     |              422 Unprocessable Content, empty lists
| 3. Rank and select the components,          |     |
|    with the buffer                          | ----+
+---------------------------------------------+     |
        |                                           |
        v                                           |
+---------------------------------------------+     |
| 4. Weight and cap the components            | ----+
+---------------------------------------------+
        |
        v
Store the audit record
        |
        v
201 Created: report with constituents, weights, joiners, leavers, status
```

An unexpected error inside a stage is stored with status `ERROR` and answered with `500`.

## Flow 2: read a stored review

```text
+------------+   GET /v1/reviews/{reviewId}         +----------------+   looks up the audit    +--------------+
| API client |   .../audit, .../trace,              | Index Reviewer |   record and the trace  | Review store |
|            |   .../securities/{securityId}        |                | ----------------------> | in memory    |
|            | -----------------------------------> |                |                         |              |
|            | <----------------------------------- |                |                         |              |
+------------+   report, audit, trace, or 404       +----------------+                         +--------------+
                 for an unknown review
```

The report of a stored review is derived again from its audit record and trace, so it always matches what the review
recorded. `GET /v1/reviews` lists the reports of the stored reviews and can filter them by index and dates. Every report
includes the findings of its trace, so a client sees why a review has warnings or failed.

## Service-level constraints

- **One request, one review.** The uploaded files are not kept; the review stores its audit record and its trace.
- **In-memory storage.** Stored reviews are lost when the application stops, and they are not shared between
  instances.
- **Configured indices only.** The index rules come from configuration. A request for an index without rules is
  rejected.
- **No user management.** The API has no authentication; it is out of scope for the assignment.

## Further reading

- [README](../README.md): build, run, request examples and the endpoint list.
- [Methodology](methodology.md): how the review selects, weights and caps, and the assumptions made.
- [Data validation](data-validation.md): which input problems are errors and which are warnings.
- [Traceability and audit](traceability-and-auditability.md): what a review stores and how to read it.
