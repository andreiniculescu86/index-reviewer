#!/bin/sh
# Purpose: This script is the project entry point for Java inspections. It gives developers and coding agents one repeatable command for running the project inspection tooling.
# Examples: ./docs/scripts/java-inspect.sh; set IDEA_HOME for another IntelliJ installation.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
quality_dir=${JAVA_QUALITY_DIR:-"$HOME/git/java/ide_settings/java/quality"}
idea_dir=${IDEA_HOME:-/Applications/IntelliJ IDEA.app}
report_dir="$project_dir/target/idea-inspections"
mkdir -p "$project_dir/target"
if [ -d "$report_dir" ]; then
  echo "Existing inspection reports found; move them aside before a fresh run: $report_dir" >&2
  exit 2
fi
exec "$idea_dir/Contents/bin/inspect.sh" "$project_dir" "$quality_dir/intellij-inspections.xml" "$report_dir" -d "$project_dir/src"
