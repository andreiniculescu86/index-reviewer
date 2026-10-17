# AI setup map

This file explains where the project's AI guidance lives. Read the linked files for current rules and settings.

## Standing rules

- [CLAUDE.md](../CLAUDE.md): project-wide permissions, communication preferences and task routing.
- [Root README](../README.md): project entry point and build overview.

## Skills

- [Skill folders](../.claude/skills/): reusable task procedures. Each SKILL.md defines its purpose and when it applies.
- [Implementation](../.claude/skills/implementation/SKILL.md): feature, bug-fix, and configuration workflow.
- [Testing](../.claude/skills/testing/SKILL.md): test-writing, execution, fixtures, coverage, and test reports.
- [Code quality checks](../.claude/skills/code-quality-checks/SKILL.md): formatting, style checks, and static analysis.
- [Project review](../.claude/skills/project-review/SKILL.md): review scope, checks, and findings.
- [Documentation](../.claude/skills/documentation/SKILL.md): README, Markdown, and architecture maintenance.

## Conditional references

Reusable instructions live in skill references. Skills route tasks to relevant files; project guides describe this
application's evolving setup and behavior.

- [Change discipline](../.claude/skills/implementation/references/change-discipline.md): compatibility and approvals.
- [Coding conventions](../.claude/skills/implementation/references/coding-conventions.md): code design, comments,
  logging.
- [Architecture](../.claude/skills/implementation/references/architecture.md): code boundaries and mapping ownership.
- [MapStruct](../.claude/skills/implementation/references/mapstruct.md): structural mapping tool preferences.
- [Quality rules](../.claude/skills/code-quality-checks/references/code-quality.md): formatting and static-analysis
  rules.
- [Runtime safety](../.claude/skills/implementation/references/runtime-safety.md): configuration, security, reliability,
  data.
- [Spring Boot](../.claude/skills/implementation/references/spring-boot.md): production framework preferences.
- [JUnit and Mockito](../.claude/skills/testing/references/junit-mockito.md): Java test-library preferences.
- [Spring tests](../.claude/skills/testing/references/spring-tests.md): Spring test contexts, MVC and service
  connections.
- [Testcontainers](../.claude/skills/testing/references/testcontainers.md): container-backed integration preferences.
- [Library research](../.claude/skills/implementation/references/library-research.md): version-specific API lookup.
- [Architecture documentation](../.claude/skills/documentation/references/architecture.md): service overview and main
  flows.

## Agents

- [Agent definitions](../.claude/agents/): delegated roles, editing permissions, skill routing, and expected output.
- [Explorer](../.claude/agents/explorer.md): investigation role.
- [Implementer](../.claude/agents/implementer.md): scoped implementation role.
- [Code Review](../.claude/agents/code-reviewer.md): independent code review role.

## Project guides

- [Architecture guide](architecture.md): service purposes, external systems, and main flow diagrams.
- [Testing guide](testing.md): this application's test suites, fixtures, services, setup, and commands.
- [Development setup](development.md): build gates, quality profiles, commands, overrides, and IDE setup.
- [Data validation](data-validation.md): service-layer review data checks, errors and warnings.
- Git permissions and standing rules live in [CLAUDE.md](../CLAUDE.md); requested commit and PR/MR procedures live in
  the [project-review skill](../.claude/skills/project-review/SKILL.md#requested-commits-and-prmr-drafts).

## Configuration and tools

- [MCP servers](../.mcp.json): project-scoped Serena and Context7 connections.
- [Serena configuration](../.serena/project.yml): semantic navigation settings.
- [Maven build](../pom.xml): selected Java/dependency versions and configured build checks.
- Quality profiles: [Checkstyle rules](../checkstyle_rules.xml), [IntelliJ formatter](../code_style_formatter.xml) and
  [IntelliJ inspections](../intelij_inspections.xml) used by Maven and the helper scripts.
- [Helper scripts](scripts/): Java formatting and inspections.
- Personal Claude configuration, globally installed skills/plugins, and authentication live outside this repository.

## How the pieces connect

CLAUDE.md routes tasks to skills and guides. Skills load relevant references. Agents reuse skills rather than duplicate
procedures. Configurations enable tools; scripts execute repeatable commands. Installed global guidance also applies
when available and relevant. File existence alone does not mean its full contents are loaded for every task.
