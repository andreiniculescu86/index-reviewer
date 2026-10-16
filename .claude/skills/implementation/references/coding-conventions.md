# Coding conventions

- Keep related linear logic together. Prefer clear, explicit code over tricks, micro-methods, and needless indirection.
- Apply SOLID: keep responsibilities cohesive, allow extension without needless edits, preserve substitutable behavior,
  split interfaces by client needs, and depend on core-owned abstractions. Prefer composition, explicit dependencies,
  constructor injection where applicable, and immutable values where useful.
- Apply KISS and YAGNI. Choose the simplest design that meets current requirements. Avoid speculative extension points,
  interfaces for every class, extra layers, and premature optimization.
- Use design patterns only for demonstrated problems. Explain consequential tradeoffs; do not add abstractions merely to
  name a pattern.

## Java library preferences

Lombok is allowed. Its dependency and build support remain subject to CLAUDE.md approval rules.

## Comments

Default to no comment. Explain a verified, non-obvious constraint or regression trap, not what the code already says.
Keep comments to one or two lines without raising surrounding density. Keep required API docs purposeful. Put history in
requested commit messages and longer design rationale in project docs. Never state guessed causes as fact. Name a
verified downstream consumer when its dependency explains why code must remain.

## Logging

Use existing logging facilities and parameterized values. Log safe identifiers, counts, statuses, and explicit units.
Log success after its actual milestone; initiating an action or returning normally may not mean business success. Use
INFO for meaningful operation/lifecycle events, DEBUG for repetition, WARN for recoverable issues, and ERROR for
failures needing investigation. Log failures at the handling boundary to avoid duplicate stack traces. Use sanitized
diagnostic context. Never dump sensitive payloads, secrets, authorization headers, or raw upstream responses.
