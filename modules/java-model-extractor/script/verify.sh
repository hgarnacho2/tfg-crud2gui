#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

echo "=== Verificando model y java-model-extractor ==="
mvn -f "$project_root/pom.xml" -pl modules/java-model-extractor -am clean verify
echo "=== Verificación completada ==="