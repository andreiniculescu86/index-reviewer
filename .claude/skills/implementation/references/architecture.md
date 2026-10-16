# Code boundaries and mapping

## Ownership

- Domain owns business concepts and invariants. Application services coordinate use cases. Adapters own transport,
  persistence, messaging, and external integrations. Source dependencies point toward the core.
- Domain must not depend on application orchestration or adapters. Application services depend on core-owned ports and
  models. Define outbound ports in the core and implement them in adapters.
- Organize real features by cohesive capability and responsibility. Keep integration models with their owning adapter.
  Avoid catch-all model packages and premature empty hierarchies.
- Transport adapters own request/response DTOs. Application owns commands, queries, and use-case results. Domain owns
  entities/value objects. Persistence adapters own storage representations.
- Services must not accept or return controller DTOs. Core contracts must not expose storage records, provider/driver
  types, query builders, generated database models, framework paging types, or integration-specific exceptions.
- Adapters may use core-owned models. Do not duplicate models merely because several components use them. Public APIs
  need intentional response contracts; never expose internal/storage objects directly.

## Mapping and validation

- Adapter-owned dedicated mappers translate requests to application inputs, results to responses, and
  integration/storage representations to and from core-owned types. Mappers perform no I/O, service calls, or
  business-policy decisions.
- Define null and partial-update semantics and allowed writable fields. Mappings must not blindly copy client-owned
  values into stored objects. Map manually when clearer or when constructors/factories must enforce invariants.
- Keep infrastructure annotations and lifecycle assumptions out of core models by default. Document any exception and
  its coupling/behavior. An annotation alone does not prove provider independence.
- Validate transport input at its boundary, enforce business invariants in the core, and format errors at the handling
  boundary. Use mechanisms supported by the selected framework.

## Design constraints

Define ownership of transactions, consistency, fetching, retry and idempotency where relevant. Missing optional data
does not prove an integration succeeded. Base consequential designs on actual workload, failure, latency, security and
operational requirements; do not invent scale assumptions.
