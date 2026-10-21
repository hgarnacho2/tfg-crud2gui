#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

echo "=== Verificando CRUD2GUI Desktop App ==="
mvn -f "$project_root/pom.xml" -pl modules/desktop-app -am clean verify
