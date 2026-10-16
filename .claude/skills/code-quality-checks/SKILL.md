---
name: code-quality-checks
description: Use when formatting files, checking style, or running static analysis and IDE inspections.
---

Use [quality rules](references/code-quality.md) for the affected file type. For Java tooling, read the relevant setup
in docs/development.md. Read pom.xml when Maven configuration or plugin versions matter. Run the requested or
applicable formatter/static checks; inspect their output and reports.
Review formatter edits for unintended changes. Fix introduced violations and rerun affected checks.

Report commands, findings, and unavailable checks. Distinguish existing violations from introduced ones.
Follow CLAUDE.md for dependency approval, deferred checks, and reuse of evidence. For uncertain tool options, use
[library research](../implementation/references/library-research.md).

Testing owns test execution and test evidence. Project-review owns behavioral and design findings. If a combined Maven
command also runs tests, use testing to assess those results; share that run's evidence instead of repeating it.
