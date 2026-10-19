#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

if (($# != 2)) || [[ "$1" == "-h" || "$1" == "--help" ]]; then
    echo "Uso: $0 <modelo-esperado.json> <modelo-extraido.json>" >&2
    exit 2
fi
for file in "$@"; do
    if [[ ! -f "$file" ]]; then
        echo "No existe el fichero JSON: $file" >&2
        exit 2
    fi
done

exec_args=""
for arg in "$@"; do
    escaped=${arg//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+="${exec_args:+ }\"$escaped\""
done

mvn -f "$project_root/pom.xml" -pl modules/model-validator -am -DskipTests install
mvn -f "$module_dir/pom.xml" exec:java "-Dexec.args=$exec_args"