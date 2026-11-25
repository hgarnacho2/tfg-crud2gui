#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

header=""
reset=""
if [[ -t 1 && "${TERM:-}" != "dumb" && -z "${NO_COLOR:-}" ]]; then
    header=$'\033[38;5;208m'
    reset=$'\033[0m'
fi

printf '\n'
printf '%sEjecutando tests de java-model-extractor ...%s\n' "$header" "$reset"
mvn -f "$project_root/pom.xml" -pl modules/java-model-extractor -am test
printf '\n'
printf '%sTests completados%s\n' "$header" "$reset"