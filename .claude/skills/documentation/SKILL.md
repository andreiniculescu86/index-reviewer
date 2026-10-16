---
name: documentation
description: Use when writing or updating READMEs, project Markdown, or high-level application architecture diagrams.
---

Inspect affected code, configuration, build, and existing docs. After a change, assess whether README.md, docs/README.md,
and relevant Markdown need updates. Use known context for that decision; read only affected files. Update stale or
incomplete content in the same scoped change. Preserve accurate unrelated text.

## Where updates belong

- README.md: project entry point, setup, usage, and working commands.
- docs/README.md: navigation map for standing rules, skills, references, agents, guides, configuration, and tools. Update
  links and purposes after moves/additions/removals. Describe where guidance lives, not its changing rule content.
- docs/architecture.md: high-level service purpose, external interactions, and main service flows. For architecture
  updates, follow [architecture scope](references/architecture.md).
- docs/testing.md: this application's test suites, fixtures, services, setup, commands, and coverage limits.
- docs/development.md: configured build gates, quality profiles, scripts, overrides, and local tool setup.
- testing skill: reusable test-writing, execution, and evidence rules.
- Skill references: shared policies and conditional procedures such as quality checks. Keep each rule in one place.
- CLAUDE.md and project-review: standing Git rules and requested commit/PR/MR procedures, respectively.
- Relevant skill or agent file: changed AI workflow or role instructions.

For internal design rationale, update an existing relevant document. Create a focused design note only when a real
decision needs one. Keep it out of the high-level architecture overview.

Check other existing Markdown files directly affected by the task. Prefer links over copied versions, settings, or
instructions. Do not create empty guides or speculative requirements. Load only affected documents.

## Review and evidence

Document implemented behavior. Label agreed future plans separately; do not present them as current behavior. Do not
invent dependencies, integrations, endpoints, commands, checks, or diagram relationships. Flag missing evidence. Keep
secrets out of documentation. Preserve compatibility and dependency approval requirements.

Check local links, moved paths, code/command examples, and diagram consistency against current files. Use shared
[quality checks](../code-quality-checks/references/code-quality.md), Markdown guidance, for formatting. Include
docs in the final diff review. Report what changed, or why no documentation update was needed when relevant.
Documentation work alone does not authorize implementation or external publication.
