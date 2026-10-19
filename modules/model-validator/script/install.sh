#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

echo "=== Instalando model y model-validator ==="
mvn -f "$project_root/pom.xml" -pl modules/model-validator -am clean install
echo "=== Instalación completada ==="