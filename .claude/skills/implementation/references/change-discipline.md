# Change discipline

CLAUDE.md owns permission gates for breaking changes and new dependencies. Apply them through these checks.

## Compatibility

Establish the baseline from code, callers, tests, and documented contracts. Check affected API shapes/statuses,
validation/errors, defaults/configuration, stored data, events, side effects, and consumers. Fixes can change behavior
consumers rely on; missing tests do not prove a feature is unused.

Choose a compatible solution first. If a break is needed or compatibility is materially uncertain, present old/new
behavior, affected consumers/data, the reason, a compatible alternative, and migration/rollback implications. Obtain the
required approval before changing that behavior. Continue independent safe work while waiting.

## Dependencies

Inventory direct and relevant transitive effects before proposals. Include compatibility/security implications and
alternatives using existing tools. Batch known additions in one clear question under CLAUDE.md's approval rule.

Check additions introduced by starters, parent/BOM changes, upgrades, generated build edits, and validation tools.
Explain newly introduced transitive groups; do not require one prompt per artifact. Do not regenerate locks or install
proposed tools while approval is pending. Version-only changes still need compatibility review.

## Completion and review

Compare the final diff and call sites with the baseline and relevant regression/contract evidence. Distinguish observed
behavior from gaps in coverage or consumer evidence.

Always report compatibility impact: none identified in scope, approved breaks, unapproved changes, or unresolved risk.
For each detected break, give old/new behavior, affected consumers/data, file references, approval evidence or gap, and
verification/migration status. Approved breaks still need user validation; report them without calling them unapproved
defects. Flag unapproved breaks as actionable findings. Missing consumers/tests limit the conclusion; report that limit.
