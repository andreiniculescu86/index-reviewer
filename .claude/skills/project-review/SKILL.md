---
name: project-review
description: Use when reviewing a supplied diff or pull request, preparing a requested commit, or drafting PR/MR text.
---

For a review, follow the review workflow below. For a requested commit or PR/MR draft, use the final section and apply
review criteria to the relevant diff as needed; it does not authorize implementation work.

## Review workflow

Establish the requested baseline and scope. Apply [change discipline](../implementation/references/change-discipline.md)
for compatibility and dependency approval checks. Trace introduced changes and related call sites: affected input,
changed path, and observable impact. Check relevant null/error contracts, resource ownership, concurrency, persistence,
and meaningful coverage gaps. Inspect the complete diff and enough surrounding code to understand each changed path.
Continue through the whole diff after finding an issue. Report unrelated defects separately, with evidence.

For base-branch reviews, compare from the merge base rather than directly against the branch tip. Use the base branch's
configured upstream when it exists and is ahead of the local branch; otherwise use the local branch. Run
`git merge-base HEAD <comparison-ref>`, then inspect `git diff <merge-base-sha>`. If the local branch is unavailable,
try its configured upstream explicitly before reporting that the target is unavailable. Do not fetch or modify refs
unless authorized.

Load relevant criteria only:

- Java edits: [coding conventions](../implementation/references/coding-conventions.md) and
  [quality rules](../code-quality-checks/references/code-quality.md), Java section.
- Structural mapping choices, including handwritten mappers, or MapStruct configuration:
  [MapStruct preferences](../implementation/references/mapstruct.md).
- Models, contracts, or boundaries: [architecture rules](../implementation/references/architecture.md) and affected
  project decisions.
- Tests or coverage gaps: testing; docs/testing.md when actual suite setup or coverage matters.
- Configuration, integrations, failures, or state: relevant [runtime
  safety](../implementation/references/runtime-safety.md) sections.
- Selected Spring behavior: [Spring guidance](../implementation/references/spring-boot.md).
- Uncertain APIs: [library research](../implementation/references/library-research.md).
- Documented behavior, setup, commands, or AI structure: documentation; inspect affected docs and links.

Treat linked instructions as review criteria. Do not edit code, tests, docs, or run mutating formatters during review.
Reuse supplied evidence. If further checks are needed, use testing for test execution or code-quality-checks for static
analysis within the review's permissions. Never invent defects or enforce absent frameworks. Distinguish concrete
violations from preferences and unverified concerns. Cite actual quality rules; avoid repeating tool findings without
useful context.

Report actionable findings with file/line, trigger, impact, and priority, ordered by severity. Keep each cited range
small and overlapping the reviewed diff. Use these priorities:

- P0: universal release blocker or critical failure.
- P1: urgent defect to fix next.
- P2: ordinary defect to fix.
- P3: low-impact issue still worth fixing.

Return no defect findings when none are supported. Always include the compatibility-impact report defined by change
discipline, plus evidence gaps. Report approved intentional breaks there without labeling them unapproved defects.
For changed configuration, include runtime safety's injection mapping and unresolved deployment decisions.
Review authorizes no implementation, commit, or external comment.

## Requested commits and PR/MR drafts

Inspect Git status and the relevant diff to establish scope. Follow CLAUDE.md staging and permission rules. Resolve
unrelated staged work without silently unstaging it. Review the exact staged diff before committing. Reuse relevant
checks or route missing test/static evidence as above; report results and limits.

Keep commit messages technical, without personal or demographic descriptions. Describe the change. PR/MR drafts explain
the problem, resulting behavior, and
verification; omit commit lists. Include compatibility impact, applicable approvals, and environment-injection changes.
Surface unresolved approval gaps before the affected action. Follow CLAUDE.md authored-text and attribution rules.
For multiline CLI bodies, write exact text to a temporary file and use --body-file where supported. Drafting does not
authorize publishing.
