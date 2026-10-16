# Formatting and static-analysis rules

## Java

Use the configured IntelliJ formatter scheme, inspection profile, and Checkstyle rules. Read actual rule options and
severities. Do not substitute defaults, enable disabled rules, or weaken checks. Surface genuine instruction conflicts
before the affected action. Keep profile changes deliberate and reviewed.

Apply formatting to changed Java files and review the resulting diff. Inspect static-analysis reports; exit status alone
may not establish that no issues exist. Do not close the user's IDE to run a CLI tool. Keep manual checks pending
without evidence.

## Markdown, JSON and TOML

Use the configured IntelliJ scheme and a 120-character margin. Preserve structure, URLs, code, and structured values.
Keep long indivisible tokens intact. Wrap prose without changing meaning. Validate JSON/TOML after edits and check local
Markdown links.

IntelliJ TOML formatting does not reflow multiline instructions. Wrap instruction prose manually at the configured
margin, preserving structured content and intended values.

## Sonar

Claim Sonar compliance only with evidence from this project's binding, scanner, profile, and quality gate. Do not borrow
another project's key or infer Sonar results from Checkstyle or IntelliJ checks.
