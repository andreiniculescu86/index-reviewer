---
name: implementation
description: Use when implementing production features, fixing bugs, or changing application runtime configuration.
---

Inspect affected code and pom.xml. Match surrounding style and syntax supported by the selected Java version. Follow
[change discipline](references/change-discipline.md) before edits and final reporting. For Java edits, use
[coding conventions](references/coding-conventions.md).

Load relevant guidance only:

- Models, contracts, responsibilities or unresolved design: [architecture rules](references/architecture.md) and
  relevant project decisions. Read docs/architecture.md when service-level context is needed.
- Choosing structural mapping tools or changing MapStruct mappings/configuration:
  [MapStruct preferences](references/mapstruct.md), together with the applicable mapping boundaries.
- Test edits, regression coverage, or behavioral checks: testing.
- Selected Spring code/configuration: [Spring guidance](references/spring-boot.md).
- Configuration, integrations, failure handling or state changes:
  relevant [runtime safety](references/runtime-safety.md) sections.
- Uncertain version-specific behavior: [library research](references/library-research.md).

For unresolved design, identify missing decisions affecting behavior, contracts, ownership or operations. Resolve
routine choices from evidence; ask concise questions about material choices, explain tradeoffs and continue independent
work while awaiting answers. Do not repeat settled design questions. Use documentation to record agreed rationale at
its appropriate scope.

For bugs, establish the cause and reproduce the symptom when feasible. Distinguish workarounds from root-cause fixes.
State uncertainty when the cause remains unknown. Use testing for meaningful regression coverage when feasible.

Complete the scoped change. Coordinate applicable tests through testing and formatting/static analysis through
code-quality-checks. Read "Build and run" in README.md for build checks. A combined build may supply both kinds of
evidence; run it once and assess each result. Instruction-only edits need instruction/link validation, not an empty
Maven build as behavioral evidence.

Use documentation to update affected READMEs and Markdown. Use project-review on the final implementation/configuration
diff, including changed tests and docs. Report actual evidence, compatibility impact, and required environment
injection.
