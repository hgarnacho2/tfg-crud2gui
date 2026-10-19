#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

echo "=== Verificando common-utils y authenticator ==="
mvn -f "$project_root/pom.xml" -pl modules/authenticator -am clean verify
echo "=== Verificacion completada ==="