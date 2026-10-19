# API reference

This page is here to help the reviewers of this application: it shows every endpoint and its response model in one
place, next to the code. In a real project the API documentation would live in a dedicated tool, such as an API
portal or a published OpenAPI specification. The running application serves the same information as Swagger UI at
http://localhost:8096/ and as OpenAPI JSON at http://localhost:8096/v3/api-docs.

All endpoints return JSON. Numbers such as weights and capping factors are JSON numbers; weights are percentages
(`18` for 18%) with 4 decimals; capping factors have 4 decimals.

| Endpoint                                                                  | Purpose                              |
|---------------------------------------------------------------------------|--------------------------------------|
| [`POST /v1/reviews`](#run-a-review)                                       | Run a review with uploaded CSV files |
| [`GET /v1/reviews`](#list-reviews)                                        | List the stored reviews              |
| [`GET /v1/reviews/{reviewId}`](#get-a-review)                             | Report of one stored review          |
| [`GET /v1/reviews/{reviewId}/audit`](#get-the-audit-record)               | What the review used                 |
| [`GET /v1/reviews/{reviewId}/trace`](#get-the-trace)                      | How the review reached its result    |
| [`GET /v1/reviews/{reviewId}/securities/{securityId}`](#get-one-security) | Trace and findings of one security   |

## Contents

- [Review status and HTTP status](#review-status-and-http-status)
  - [Review statuses](#review-statuses)
  - [Running a review: `POST /v1/reviews`](#running-a-review-post-v1reviews)
  - [Reading a stored review: `GET` endpoints](#reading-a-stored-review-get-endpoints)
  - [Examples](#examples)
- [Run a review](#run-a-review)
  - [ConfigRequest](#configrequest)
  - [ReviewResponse](#reviewresponse)
  - [ConstituentResponse](#constituentresponse)
  - [FindingResponse](#findingresponse)
- [List reviews](#list-reviews)
- [Get a review](#get-a-review)
- [Get the audit record](#get-the-audit-record)
  - [ReviewAuditResponse](#reviewauditresponse)
  - [IndexRulesResponse](#indexrulesresponse)
- [Get the trace](#get-the-trace)
  - [ReviewTraceResponse](#reviewtraceresponse)
  - [SecurityTraceResponse](#securitytraceresponse)
- [Get one security](#get-one-security)
- [ErrorResponse](#errorresponse)
  - [Error](#error)

## Review status and HTTP status

Every response has up to two statuses, and they answer different questions:

| Status                      | Question it answers           | Values                                             |
|-----------------------------|-------------------------------|----------------------------------------------------|
| HTTP status code            | Did the **request** work?     | `201` `200` `400` `404` `413` `422` `500`          |
| Review `status` in the body | Did the **calculation** work? | `SUCCESS` `SUCCESS_WITH_WARNINGS` `FAILED` `ERROR` |

A review `status` only exists when a review was run and stored. A request that is rejected before the review starts
returns an [ErrorResponse](#errorresponse) without a review `status`.

### Review statuses

| Review `status`         | Meaning                                  | Typical cause                                   |
|-------------------------|------------------------------------------|-------------------------------------------------|
| `SUCCESS`               | Index calculated, no data problems       | Clean input files                               |
| `SUCCESS_WITH_WARNINGS` | Index calculated, some data handled      | A security without review-date data left out    |
| `FAILED`                | Index cannot be calculated correctly     | Bad, inconsistent or incomplete data, see below |
| `ERROR`                 | The application failed while calculating | A bug or system problem, never the data         |

A review is `FAILED` when the data cannot give a correct index, for example:

- a file has no rows for the cut-off date or the review date;
- the current composition does not have 20 different securities;
- a price or number of shares is 0 or below, or a free float is outside 0 to 1 (such as `85` sent as a percentage);
- one security has two different rows for the same date;
- fewer than 20 securities have usable data;
- the final selection or weights fail their checks.

The findings in the report say which of these happened. Do not confuse the review `status` `ERROR` with the finding
`severity` `ERROR`: a finding with severity `ERROR` is one data problem, and it makes the review `FAILED`, not
`ERROR`.

### Running a review: `POST /v1/reviews`

| Situation                                  | HTTP  | Body                                         | Stored     |
|--------------------------------------------|-------|----------------------------------------------|------------|
| Index calculated, with or without warnings | `201` | Report, `SUCCESS` or `SUCCESS_WITH_WARNINGS` | Yes        |
| Data cannot give a correct index           | `422` | Report, `FAILED`, findings say why           | Yes        |
| Application failed while calculating       | `500` | ErrorResponse `INTERNAL_ERROR`, no report    | As `ERROR` |
| Missing file, bad value, wrong date order  | `400` | ErrorResponse                                | No         |
| File above the upload size limit           | `413` | ErrorResponse                                | No         |
| Index without configured rules             | `422` | ErrorResponse `INDEX_NOT_CONFIGURED`         | No         |

A `500` response does not contain the review id. The review stored with status `ERROR` can be found with
`GET /v1/reviews`.

### Reading a stored review: `GET` endpoints

| Situation               | HTTP  | Body                                             |
|-------------------------|-------|--------------------------------------------------|
| The review exists       | `200` | Report with its status, also `FAILED` or `ERROR` |
| Unknown review id       | `404` | ErrorResponse `NOT_FOUND`                        |
| Review id is not a UUID | `400` | ErrorResponse `INVALID_VALUE`                    |

Reading always returns `200` for a stored review: the read worked, even if the review did not. A client must check the
report's `status` and `findings` to know whether the review succeeded.

### Examples

Free float `85` instead of `0.85` for security 108:

```text
POST /v1/reviews           -> 422  "status": "FAILED", finding INVALID_VALUE (ERROR), security 108, field freeFloat
GET  /v1/reviews/{id}      -> 200  "status": "FAILED", same finding
```

The assignment files, where security 166 has no review-date row:

```text
POST /v1/reviews           -> 201  "status": "SUCCESS_WITH_WARNINGS", finding MISSING_REVIEW_DATA (WARNING), 166
```

No `composition` file in the request:

```text
POST /v1/reviews           -> 400  ErrorResponse MISSING_FILE, field composition; nothing stored
```

## Run a review

`POST /v1/reviews`, `multipart/form-data`. Runs the review and stores it, also when it fails on data errors.

| Part            | Required | Content                                                         |
|-----------------|----------|-----------------------------------------------------------------|
| `universe`      | yes      | SPI universe CSV: `date;id`                                     |
| `securityData`  | yes      | Security data CSV: `id;date;price;free_float;shares`            |
| `composition`   | yes      | Current composition CSV: `id`                                   |
| `configRequest` | no       | JSON [ConfigRequest](#configrequest), defaults apply without it |

| HTTP  | Body                              | When                                                                     |
|-------|-----------------------------------|--------------------------------------------------------------------------|
| `201` | [ReviewResponse](#reviewresponse) | Review ran: `SUCCESS` or `SUCCESS_WITH_WARNINGS`                         |
| `422` | [ReviewResponse](#reviewresponse) | Review failed on data errors: `FAILED`, stored                           |
| `422` | [ErrorResponse](#errorresponse)   | Index not configured; nothing stored                                     |
| `400` | [ErrorResponse](#errorresponse)   | Missing, empty or non-CSV file, invalid value, cut-off after review date |
| `413` | [ErrorResponse](#errorresponse)   | A file exceeds the upload size limit                                     |
| `500` | [ErrorResponse](#errorresponse)   | Unexpected error; stored with status `ERROR`                             |

### ConfigRequest

| Field          | Type                | Default      | Meaning                                                        |
|----------------|---------------------|--------------|----------------------------------------------------------------|
| `indexType`    | string              | `SMI`        | Index to review; only `SMI` is configured                      |
| `cutoffDate`   | date (`yyyy-MM-dd`) | `2026-09-10` | Date of the prices                                             |
| `reviewDate`   | date (`yyyy-MM-dd`) | `2026-09-21` | Date of universe, shares, free float; not before cut-off       |
| `csvSeparator` | string              | `;`          | Separator of all 3 files; not a quote, backslash or line break |

### ReviewResponse

The review report. The same model is returned by `POST /v1/reviews`, `GET /v1/reviews/{reviewId}` and, as a list, by
`GET /v1/reviews`.

| Field          | Type                                       | Meaning                                              |
|----------------|--------------------------------------------|------------------------------------------------------|
| `reviewId`     | UUID                                       | Id of the stored review, used by the other endpoints |
| `status`       | [status][status]                           | Outcome of the review                                |
| `constituents` | list of [ConstituentResponse][constituent] | Final components in rank order; empty on failure     |
| `joiners`      | list of security ids                       | Selected, not in the current composition             |
| `leavers`      | list of security ids                       | In the current composition, not selected             |
| `findings`     | list of [FindingResponse][finding]         | Errors and warnings that explain the status          |

### ConstituentResponse

| Field           | Type    | Meaning                                                                |
|-----------------|---------|------------------------------------------------------------------------|
| `securityId`    | integer | Security id                                                            |
| `weight`        | number  | Final weight after capping, in percent; at most 18                     |
| `cappingFactor` | number  | Factor applied to the FFMCAP; `1` when not capped, below 1 when capped |

### FindingResponse

| Field        | Type                 | Meaning                                      |
|--------------|----------------------|----------------------------------------------|
| `code`       | [code][codes]        | Kind of problem                              |
| `severity`   | [severity][severity] | How the finding affects the review           |
| `securityId` | integer or null      | Security concerned; null for the whole input |
| `field`      | string or null       | Data field or input file concerned           |
| `message`    | string               | Readable explanation                         |

## List reviews

`GET /v1/reviews`. Returns the stored reviews, newest first, as a list of [ReviewResponse](#reviewresponse). All
filters are optional and combined.

| Query parameter | Type                | Meaning                                |
|-----------------|---------------------|----------------------------------------|
| `indexType`     | string              | Only reviews of this index, e.g. `SMI` |
| `cutoffDate`    | date (`yyyy-MM-dd`) | Only reviews with this cut-off date    |
| `reviewDate`    | date (`yyyy-MM-dd`) | Only reviews with this review date     |

| HTTP  | Body                                      | When                                                        |
|-------|-------------------------------------------|-------------------------------------------------------------|
| `200` | list of [ReviewResponse](#reviewresponse) | Always, also for `FAILED` and `ERROR` reviews; can be empty |
| `400` | [ErrorResponse](#errorresponse)           | Unknown index or a date that is not ISO                     |

## Get a review

`GET /v1/reviews/{reviewId}`. Returns the report of one stored review, derived again from its audit record and trace.

| HTTP  | Body                              | When                                                             |
|-------|-----------------------------------|------------------------------------------------------------------|
| `200` | [ReviewResponse](#reviewresponse) | The review exists, whatever its status, also `FAILED` or `ERROR` |
| `400` | [ErrorResponse](#errorresponse)   | The review id is not a UUID                                      |
| `404` | [ErrorResponse](#errorresponse)   | Unknown review id                                                |

## Get the audit record

`GET /v1/reviews/{reviewId}/audit`. Returns what the review used: dates, rules, application version and time.
Status codes as for [Get a review](#get-a-review).

### ReviewAuditResponse

| Field         | Type                                      | Meaning                                         |
|---------------|-------------------------------------------|-------------------------------------------------|
| `reviewId`    | UUID                                      | Review id                                       |
| `indexType`   | string                                    | Reviewed index                                  |
| `cutoffDate`  | date                                      | Cut-off date the review used                    |
| `reviewDate`  | date                                      | Review date the review used                     |
| `status`      | [status][status]                          | Outcome of the review                           |
| `executedAt`  | timestamp (UTC)                           | When the review started                         |
| `appVersion`  | string                                    | Application version that ran the review         |
| `rules`       | [IndexRulesResponse](#indexrulesresponse) | Selection and weighting parameters used         |
| `composition` | list of security ids                      | New composition in rank order; empty on failure |

### IndexRulesResponse

| Field                 | Type    | Meaning                              |
|-----------------------|---------|--------------------------------------|
| `numberOfComponents`  | integer | Index size, 20 for the SMI           |
| `directSelectionRank` | integer | Last rank selected directly, 18      |
| `bufferRank`          | integer | Last rank of the buffer, 22          |
| `capPercentage`       | number  | Maximum weight in percent, 18        |
| `methodologyVersion`  | string  | Rulebook version the rules come from |

## Get the trace

`GET /v1/reviews/{reviewId}/trace`. Returns how the review reached its result: the values, rank and decision of every
security, and all findings. Status codes as for [Get a review](#get-a-review).

### ReviewTraceResponse

| Field        | Type                                      | Meaning                                 |
|--------------|-------------------------------------------|-----------------------------------------|
| `reviewId`   | UUID                                      | Review id                               |
| `indexType`  | string                                    | Reviewed index                          |
| `cutoffDate` | date                                      | Cut-off date                            |
| `reviewDate` | date                                      | Review date                             |
| `status`     | [status][status]                          | Outcome of the review                   |
| `securities` | list of [SecurityTraceResponse][security] | Universe members and current components |
| `findings`   | list of [FindingResponse][finding]        | All findings of the review              |

### SecurityTraceResponse

Values of steps that did not run for the security are null.

| Field               | Type                 | Meaning                                            |
|---------------------|----------------------|----------------------------------------------------|
| `securityId`        | integer              | Security id                                        |
| `existingComponent` | boolean              | In the current composition                         |
| `price`             | number               | Price on the cut-off date                          |
| `shares`            | number               | Shares on the review date                          |
| `freeFloat`         | number               | Free float factor on the review date               |
| `ffmcap`            | number               | `price x shares x freeFloat`; null if not eligible |
| `rank`              | integer              | Rank by FFMCAP among the eligible securities       |
| `decision`          | [decision][decision] | Why the security is in or out                      |
| `cappingFactor`     | number               | Capping factor, if selected                        |
| `weight`            | number               | Final weight in percent, if selected               |

## Get one security

`GET /v1/reviews/{reviewId}/securities/{securityId}`. Returns a [ReviewTraceResponse](#reviewtraceresponse) that holds
only this security and its findings, for example to see why it left the index.

| HTTP  | Body                                        | When                                                         |
|-------|---------------------------------------------|--------------------------------------------------------------|
| `200` | [ReviewTraceResponse](#reviewtraceresponse) | The security is part of the review                           |
| `400` | [ErrorResponse](#errorresponse)             | The review id is not a UUID or the security id not a number  |
| `404` | [ErrorResponse](#errorresponse)             | Unknown review id, or the security is not part of the review |

## ErrorResponse

Returned when a request is rejected (`400`, `413`, `422` for an index without rules), which is not stored, and on an
unexpected error (`500`), where the review is stored with status `ERROR`.

| Field      | Type                    | Meaning                            |
|------------|-------------------------|------------------------------------|
| `dateTime` | `yyyy-MM-dd HH:mm:ss`   | When the error occurred            |
| `status`   | integer                 | HTTP status                        |
| `title`    | string                  | HTTP status text, e.g. `Not Found` |
| `instance` | string                  | Request path                       |
| `errors`   | list of [Error](#error) | One entry per problem              |

### Error

| Field     | Type           | Meaning                           |
|-----------|----------------|-----------------------------------|
| `code`    | [code][errors] | Kind of problem                   |
| `field`   | string         | Request part or field concerned   |
| `value`   | string         | Rejected value, when there is one |
| `message` | string         | Readable explanation              |

Example, `GET /v1/reviews/00000000-0000-0000-0000-000000000000`:

```json
{
  "dateTime": "2026-10-18 20:18:17",
  "status": 404,
  "title": "Not Found",
  "instance": "/v1/reviews/00000000-0000-0000-0000-000000000000",
  "errors": [
    {
      "code": "NOT_FOUND",
      "field": "reviewId",
      "value": "00000000-0000-0000-0000-000000000000",
      "message": "Review 00000000-0000-0000-0000-000000000000 does not exist"
    }
  ]
}
```

[status]: traceability-and-auditability.md#review-status
[codes]: traceability-and-auditability.md#finding-codes
[severity]: data-validation.md#severities
[decision]: methodology.md#selection-decisions
[errors]: data-validation.md#request-errors
[constituent]: #constituentresponse
[finding]: #findingresponse
[security]: #securitytraceresponse
