# Application architecture

The Index Reviewer is a Spring Boot REST service that runs an ordinary index review. A client uploads the input data
as 3 CSV files; the service validates the data, selects the index components, weights and caps them, and returns a
review report. Every review is stored with an audit record and a trace, so that its result and the reasons behind it
can be read again later. The SMI is the only configured index. An index of the same shape, ranked by FFMCAP and
selected with a buffer, is added through configuration only; review dates come with each request.

The design follows four goals:

| Goal                           | How the architecture supports it                                           |
|--------------------------------|----------------------------------------------------------------------------|
| Result follows the rulebook    | Rules come from checked configuration; the final index is checked again    |
| Every result can be explained  | Each step stores its values, decisions and findings per security           |
| Bad data never passes silently | Each data problem stops the review as an error or is recorded as a warning |
| New indices without rework     | Each index has its own reviewer behind one interface, reusing the steps    |

## Contents

- [Context](#context)
- [Building blocks](#building-blocks)
- [Flow 1: run a review](#flow-1-run-a-review)
  - [Outcomes](#outcomes)
- [Flow 2: read a stored review](#flow-2-read-a-stored-review)
- [Stored data](#stored-data)
- [Cross-cutting concerns](#cross-cutting-concerns)
- [Extension points](#extension-points)
- [Constraints](#constraints)
- [Further reading](#further-reading)

## Context

```text
                     POST /v1/reviews (3 CSV files, optional index and dates)
+--------------------+  ------------------------------------------------>  +---------------------------------+
| API client         |  GET /v1/reviews/... (report, audit, trace)          | Index Reviewer                  |
| curl or Swagger UI |  ------------------------------------------------>  | Spring Boot REST API, Java 21   |
|                    |  <------------------------------------------------  |                                 |
+--------------------+  JSON: report, audit, trace or error                 | index rules: application.yml    |
                                                                            | review store: in memory         |
                                                                            +---------------------------------+
                                                                                            |
                                                                                            | structured logs (ECS)
                                                                                            v
                                                                                logs/index-reviewer.json
```

All input of a review arrives in the request. The service calls no other system and keeps the stored reviews in the
memory of the running instance.

## Building blocks

```text
+------------------------------------------------------------------+      +-------------------------------+
| API layer                                                        | ---> | CsvFileReader                 |
| ReviewController, MonitorController, request validation,         |      | uploaded CSV to typed rows    |
| mappers, GlobalExceptionHandler                                  |      +-------------------------------+
+------------------------------------------------------------------+
        | ReviewCommand: rows, index, dates          ^ ReviewReport, audit record, trace
        v                                            |
+------------------------------------------------------------------+      +-------------------------------+
| ReviewService                                                    | ---> | Review store, in memory       |
| picks the IndexReviewer of the index, reads stored reviews       |      | ReviewAuditRepository         |
+------------------------------------------------------------------+      | ReviewTraceRepository         |
        |                                                                 +-------------------------------+
        v                                                                                 ^
+------------------------------------------------------------------+                      |
| RankedIndexReviewer                                              | ---------------------+
| runs the steps with the rules of the requested index, stores the |   trace after each step,
| trace and findings of each step, then the audit record           |   audit record at the end
+------------------------------------------------------------------+
        | calls the steps in order; each returns result, trace records and findings
        v
+------------------------+  +------------------------+  +------------------------+  +------------------------+
| ReviewDataValidator    |  | FfmcapCalculatorStep   |  | ComponentSelectorStep  |  | WeightCalculatorStep   |
| dates, composition,    |  | row checks, eligibility|  | ranking, buffer, ties  |  | weights, capping       |
| enough candidates      |  | and FFMCAP             |  |                        |  | rounds, capping factor |
+------------------------+  +------------------------+  +------------------------+  +------------------------+
```

| Block          | Package                                        | Responsibility                                  |
|----------------|------------------------------------------------|-------------------------------------------------|
| API layer      | `controller`, `dto`, `validation`, `exception` | HTTP contract, request checks, errors           |
| Review service | `service`                                      | Routes a review to its index; reads reviews     |
| Index reviewer | `service.index`                                | Runs the steps for each configured index        |
| Review steps   | `service.review`                               | Calculations returning result, trace, findings  |
| Models         | `model`                                        | Command, candidates, report, audit, trace       |
| Repositories   | `repository`                                   | CSV parsing and review store, behind interfaces |
| Configuration  | `config`, `application.yml`                    | Index rules, OpenAPI, MapStruct, JSON part      |

The steps do not store anything themselves. They take their input and the index rules and return values, so each step
can be tested on its own and reused by another index.

## Flow 1: run a review

The review runs synchronously inside the request.

```text
POST /v1/reviews
   |
   v
API layer: read and check the request (files present and CSV, dates in order, separator, index)
   |
   v
RankedIndexReviewer: rules of the requested index, new review id
   |
   |   stage                                   stored in the trace                stops on
   |   --------------------------------------  ---------------------------------  ------------------------------
   +-> 1. check the input                      findings                           dates missing in the files,
   |                                                                              composition of the wrong size 
   +-> 2. calculate FFMCAP per security        price, shares, free float, FFMCAP, invalid or conflicting values,
   |      and check there are enough           eligibility, findings              fewer than 20 eligible
   +-> 3. rank and select with the buffer      rank, selection decision,          selection not 20 components
   |                                           tie warnings
   +-> 4. weight and cap                       weight, capping factor             a weight above the cap
   |
   v
store the audit record (status, dates, rules, version, time, composition)
   |
   v
response: review report with constituents, joiners, leavers, status and findings
```

Each stage writes to the trace before the next stage starts. A stage with an `ERROR` finding ends the review, so a
failed review still shows everything that was calculated before the error. Warnings never stop a review.

### Outcomes

| Situation                         | Status                  | HTTP  | Stored               | Body                 |
|-----------------------------------|-------------------------|-------|----------------------|----------------------|
| All stages passed, no warnings    | `SUCCESS`               | `201` | audit, trace         | report               |
| All stages passed, with warnings  | `SUCCESS_WITH_WARNINGS` | `201` | audit, trace         | report               |
| A stage found a data error        | `FAILED`                | `422` | audit, partial trace | report with findings |
| Unexpected exception in a stage   | `ERROR`                 | `500` | audit, partial trace | error response       |
| Invalid file, value or date order | none                    | `400` | nothing              | error response       |
| Index without configured rules    | none                    | `422` | nothing              | error response       |

A failed review has empty constituent, joiner and leaver lists. A data error is a review outcome, not a broken
request: the request was valid, the data was not. That is why it is stored and traceable like a successful review.
`ERROR` is never caused by data; it means the application itself failed. Reading any stored review returns `200`. See
[Review status and HTTP status](api.md#review-status-and-http-status).

## Flow 2: read a stored review

```text
GET /v1/reviews                                   ---> list of review reports, filtered by index and dates
GET /v1/reviews/{reviewId}                        ---> review report
GET /v1/reviews/{reviewId}/audit                  ---> audit record
GET /v1/reviews/{reviewId}/trace                  ---> trace of all securities and all findings
GET /v1/reviews/{reviewId}/securities/{securityId} ---> trace and findings of one security

unknown review id ---> 404
```

A stored review is returned with `200` whatever its status, also when it is `FAILED` or `ERROR`. The HTTP status
only says that the read worked; the report's `status` and `findings` say whether the review worked.

## Stored data

```text
review id
   |
   +-- audit record   what the review used and its outcome:
   |                  index, cut-off and review date, rules, application version, time, status, new composition
   |
   +-- trace          how the review reached the outcome:
                      per security: price, shares, free float, FFMCAP, rank, decision, capping factor, weight
                      findings:     code, severity, security, field, message

review report = derived from the audit record and the trace when it is read
```

The report is not stored separately. Deriving it from the audit record and the trace means the report and its
explanation can never disagree. The models are described in the [API reference](api.md).

## Cross-cutting concerns

| Concern         | Approach                                                                                        |
|-----------------|-------------------------------------------------------------------------------------------------|
| Validation      | Request format at the API layer (`400`); data content in the review as findings with severities |
| Error responses | `GlobalExceptionHandler` turns every rejected request into the same `ErrorResponse` shape       |
| Configuration   | SMI rules bound from `application.yml` and validated at startup; a wrong rule stops the start   |
| Precision       | Exact decimals; weights with 4 decimals, capping factors with 10 significant digits             |
| Logging         | Review id, status and step results, as ECS JSON in `logs/index-reviewer.json`                   |
| Concurrency     | Each request runs its own review; the in-memory store uses `ConcurrentHashMap`                  |
| API description | springdoc serves Swagger UI and the OpenAPI specification from the code                         |

## Extension points

| Change                                     | What to add                                        | Unchanged       |
|--------------------------------------------|----------------------------------------------------|-----------------|
| Index of the same shape, e.g. SMI Expanded | `IndexType` value, YAML block under `indices`      | Logic, API      |
| Index with other rules, e.g. the SLI cap   | New step or capping logic, own reviewer if needed  | API, service    |
| Another review period                      | New data files and dates in the request            | Everything      |
| A database                                 | Implementations of the 2 repository interfaces     | Services, steps |
| Another input format                       | A `FileReader` implementation, DTOs, request parts | Services, steps |

The index rules describe one size, one direct selection rank, one buffer and one cap. Every index that follows this
pattern is reviewed by `RankedIndexReviewer`; the steps are listed in
[Adding an index](methodology.md#adding-an-index). An index with a tiered cap, equal weights or other eligibility
rules needs its own steps.

## Constraints

- **One request, one review.** The uploaded files are not kept; the audit record and the trace are.
- **In-memory storage.** Stored reviews are lost when the application stops and are not shared between instances.
- **Synchronous.** A review runs inside the request and the client waits for the report.
- **Configured indices only.** A request for an index without rules is rejected.
- **No user management.** The API has no authentication; it is out of scope for the assignment.

## Further reading

- [README](../README.md): build, run, request examples and the endpoint list.
- [API reference](api.md): endpoints, response models and HTTP statuses.
- [Methodology](methodology.md): how the review selects, weights and caps, and the assumptions made.
- [Data validation](data-validation.md): which input problems are errors and which are warnings.
- [Traceability and audit](traceability-and-auditability.md): what a review stores and how to read it.
- [Assumptions and design decisions](design-decisions.md): the assumptions made and the technical decisions.
