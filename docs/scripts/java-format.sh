#!/bin/sh
# Purpose: This script is the project entry point for Java formatting. It gives developers and coding agents one repeatable command for applying the project formatter.
# Examples: ./docs/scripts/java-format.sh src/main/java/example/Service.java
# Set IDEA_HOME to your IntelliJ installation directory on another machine.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
quality_dir=${JAVA_QUALITY_DIR:-"$project_dir"}
idea_dir=${IDEA_HOME:-/Applications/IntelliJ IDEA.app}
if [ "$#" -eq 0 ]; then
  echo "Pass changed Java file paths; no files were formatted." >&2
  exit 2
fi
for file in "$@"; do
  case "$file" in *.java) ;; *) echo "Only Java files are supported: $file" >&2; exit 2 ;; esac
  [ -f "$file" ] || { echo "Missing file: $file" >&2; exit 2; }
done
exec "$idea_dir/Contents/bin/format.sh" -s "$quality_dir/code_style_formatter.xml" "$@"
