---
name: testing
description: Use when writing, running, debugging, or reviewing tests, fixtures, regression coverage, and test reports.
---

Read pom.xml for the selected test stack and "Tests" in README.md for actual suites, fixtures and commands.
Establish whether the task is writing, execution, debugging or review; review mode supplies criteria, not permission to
edit tests. Do not infer configured coverage from a naming convention or an empty build.

## Load relevant guidance

- Writing/changing test code: [coding conventions](../implementation/references/coding-conventions.md).
- Architecture or mapping tests: [architecture rules](../implementation/references/architecture.md).
- Changed expectations, compatibility or test dependencies:
  [change discipline](../implementation/references/change-discipline.md).
- Choosing a new Java test stack, or JUnit/Mockito setup or usage:
  [JUnit and Mockito preferences](references/junit-mockito.md).
- Spring-dependent behavior: [Spring testing preferences](references/spring-tests.md).
- Container-backed integration tests: [Testcontainers preferences](references/testcontainers.md).
- Uncertain APIs or runner options: [library research](../implementation/references/library-research.md).

## Test design

Test observable behavior at its owning layer. Cover relevant boundaries, errors and regressions; avoid tests that mirror
implementation. For bugs, demonstrate failure against the old behavior and success after the fix when feasible. Never
weaken assertions or silently replace expected results to hide a regression. For an approved behavior change, make the
approved contract and resulting expectations explicit.

Add architecture checks when real packages justify them, plus relevant mapping and integration coverage. For changed
configuration, verify binding and required settings. For state changes, cover applicable commit/rollback, duplicate,
race and partial-failure behavior when the owning infrastructure exists.

Do not add explanatory comments in the test code, especially the inline comments.

Name methods operation_whenCondition_expectOutcome, unit-test classes *Test and integration-test classes *IT.
Keep test method names under 50 characters, but still to have meaning. Confirm runner discovery for both.
Separate arrange, act and assert with blank lines. Use descriptive inputs and expected
values. Construct the tested object directly for ordinary business logic, validators and mappers. Mock external
collaborators, not the tested object or simple data. Verify interactions only when they express a meaningful contract.

Use conventional Maven test folders unless the build defines another source set; keep fixtures in src/test/resources.
Keep fixtures small and deterministic, using fixed dates or a controlled clock. Extract helpers for meaningful reuse or
readability, and avoid static mutable application-service fields in shared setup.

## Execution and evidence

Use ./mvnw and the actual commands in README.md. Run affected suites/modules first; broaden checks to match the
scope. For combined build gates, read README.md and share evidence with code-quality-checks. Inspect
reports to establish which tests ran, failed or were skipped. Distinguish application failures from Docker, service or
environment blockers. Do not silently substitute test infrastructure. Report commands, actual results, coverage limits
and blocked checks under CLAUDE.md's evidence and permission rules.

Use code-quality-checks for test formatting/static analysis, documentation when test setup or coverage changes, and
project-review for the final changed tests. Document actual images, Docker prerequisites, connection setup and execution
commands in README.md when integration infrastructure is adopted.
