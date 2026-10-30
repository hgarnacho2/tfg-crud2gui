#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

echo "=== Instalando common y authenticator ==="
mvn -f "$project_root/pom.xml" -pl modules/authenticator -am clean install
echo "=== Instalacion completada ==="