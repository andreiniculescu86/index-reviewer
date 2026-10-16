# Development tools and build setup

[pom.xml](../pom.xml) defines the Java target, Maven plugin versions, and build checks. The helper scripts live in
[docs/scripts](scripts/). Test suites and their setup belong in [testing.md](testing.md).

## Claude Code setup

Start Claude Code from the repository root so it discovers the project instructions, skills, agents and MCP
configuration:

```sh
claude
```

- CLAUDE.md contains standing project rules. Use `/context` to inspect loaded memory files.
- `.claude/skills/` contains five task skills. Claude selects relevant skills, or you can invoke `/implementation`,
  `/testing`, `/code-quality-checks`, `/project-review` or `/documentation` explicitly. Supporting references load as
  needed. The review skill is named project-review because Claude Code has a built-in `/code-review` that would
  otherwise take precedence.
- `.claude/agents/` defines explorer, implementer and code-reviewer. Ask Claude to use a named agent for bounded work.
  Explorer and code-reviewer have read-only tools. The parent supplies Git diffs and command results because those roles
  cannot run a shell. Implementer inherits the session's permissions. These agents cannot spawn more agents.
- `.claude/settings.json` limits ordinary concurrent subagents to three. This is a concurrency setting, not a total
  session or token limit. Model selection inherits from the parent session; no model is pinned by this project. It also
  sets empty commit and PR attribution so Claude adds no co-author trailer or generated-by footer. Personal overrides
  belong in the ignored `.claude/settings.local.json`.
- `.mcp.json` connects Serena and Context7. Approve the project servers in Claude's first-use prompt or `/mcp` before
  using them. Run `claude mcp list` to inspect discovery and approval status.

Serena uses the existing installation at `${HOME}/.local/bin/serena`, its Claude Code context and this repository's
configuration. Adjust that command if Serena is installed elsewhere. Context7 uses its public HTTP endpoint without an
embedded API key. No new tool installation or dependency is part of this setup.

Project files do not transfer Codex's personal instructions, plugins, skills, MCP approvals or login state. Claude's
user-level settings and installed plugins apply independently. Restart an existing Claude session after switching to
this branch so it reloads the project setup. The same project files apply when opening this folder in Claude Code's
desktop interface; they do not configure an ordinary Claude chat.

## Quality profiles

The quality profiles live in the project root, and Maven and the Java helpers read them from there:

- [checkstyle_rules.xml](../checkstyle_rules.xml): Checkstyle rules, severities, and exclusions.
- [code_style_formatter.xml](../code_style_formatter.xml): IntelliJ scheme JavaCodeStyle.
- [intelij_inspections.xml](../intelij_inspections.xml): IntelliJ profile JavaInspections.

To use profiles from another folder, Maven accepts `-Djava.quality.dir=/absolute/profile/folder`; the Java helpers
accept JAVA_QUALITY_DIR. The folder must contain files with the same names.

## Commands

- `./mvnw verify`: combined build gate. It reaches the configured test phase and Checkstyle execution. Checkstyle covers
  production/test Java, excludes resources, and fails active warning-level violations. See testing.md for current test
  availability. One run can supply build, test, and static-check evidence.
- `./mvnw checkstyle:check`: run the configured Checkstyle goal directly for a static-only check.
- `./docs/scripts/java-format.sh <changed-file.java> ...`: format named Java files with the selected scheme.
- `./docs/scripts/java-inspect.sh`: inspect src and write target/idea-inspections. Existing reports must be moved aside
  before a fresh run; the script refuses to overwrite them.

## Tool references

- [Claude project instructions](https://code.claude.com/docs/en/memory)
- [Claude skills](https://code.claude.com/docs/en/skills)
- [Claude subagents](https://code.claude.com/docs/en/sub-agents)
- [Claude settings](https://code.claude.com/docs/en/settings)
- [Claude MCP configuration](https://code.claude.com/docs/en/mcp)
- [IntelliJ formatter](https://www.jetbrains.com/help/idea/command-line-formatter.html)
- [IntelliJ inspections](https://www.jetbrains.com/help/idea/command-line-code-inspector.html)
- [Maven Checkstyle check](https://maven.apache.org/plugins/maven-checkstyle-plugin/check-mojo.html)
