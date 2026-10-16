# Runtime configuration, reliability, and data correctness

Read only sections relevant to the affected configuration, integration, or state-changing path.

## Environment and security

Keep environment-specific endpoints, credentials, ports, resource names, operational limits, and launch/deployment setup
outside application source. Keep business rules and invariants in code. Do not turn every constant into an environment
setting.

Tracked files may contain nonsecret defaults or examples. Inject secrets and deployment-specific values from the runtime
environment, mounted configuration, or an approved secret store. Keep real credentials out of tracked YAML, images,
launch commands, reports, and logs. Validate required settings at startup; avoid unsafe production fallbacks. Keep
environment-specific launch commands and deployment settings in the existing scripts/manifests/configuration layer. Do
not add Kubernetes resources or choose infrastructure merely because this template mentions Kubernetes.

For every added/changed setting, report its key, purpose, safe default or required status, sensitive status, and source.
Identify which values must be extracted from the application package for environment injection. Show the intended
mechanism when known, such as Kubernetes ConfigMap, Secret/secret-store integration, environment variable, or mounted
file. Ask the user to confirm unresolved deployment ownership/source before wiring the affected setting. Do not ask for
secret values in chat. Document the agreed mapping and required deployment changes in existing project/deployment
documentation.

Use least privilege and validate untrusted input at boundaries. Preserve authorization and error sanitization when
changing flows. Never leak secrets or internal provider details through public errors or configuration diagnostics.

## Resilience and fault tolerance

Identify relevant failure modes and acceptable failure/latency behavior before choosing a pattern. Set bounded timeouts
for external calls. Retry only transient failures when repetition is safe or protected by idempotency. Bound attempts
and total time; use backoff/jitter where warranted. Account for retry amplification across layers and cancellation.
Consider circuit breakers, bulkheads, rate limits, or fallbacks only for a demonstrated failure/isolation requirement.
Do not add patterns, libraries, or switches speculatively. Respect dependency approval before introducing support tools.
Fallbacks must preserve correctness and expose degraded outcomes; never report a failed or partial operation as success.

## Transactions and data integrity

Define the atomic unit, transaction owner, rollback behavior, and consistency requirement. Preserve invariants through
partial failures, duplicates, concurrency, and replay. Choose relevant constraints, locking/version checks, and
idempotency from actual requirements. Do not assume method annotations alone prove atomicity or correct transaction
propagation. Avoid holding database transactions across slow remote calls without a justified design. Do not assume a
local database transaction makes messaging or another service atomic. Evaluate an outbox or compensation only if
cross-boundary consistency requires it. Preserve schema/data compatibility and plan migrations/rollback before
destructive changes.

## Error handling

Preserve meaningful causes and distinguish validation, business rejection, transient failure, and permanent failure.
Translate errors at adapter/handling boundaries. Do not swallow exceptions, catch broadly to return success, or retry
permanent failures.
