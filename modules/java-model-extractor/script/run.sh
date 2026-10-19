#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

usage() {
    cat >&2 <<USAGE
Uso: $0 <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json]

Rutas relativas se resuelven desde el directorio actual.
Si no se indica salida, se genera output/model_yyyyMMdd_HHmmss.json en este módulo.
Ejemplo:
  $0 modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio target/model.json
USAGE
    exit 2
}

if (($# == 0)) || [[ "$1" == "-h" || "$1" == "--help" ]]; then
    usage
fi
if [[ ! -d "$1" ]]; then
    echo "No existe la carpeta de código: $1" >&2
    exit 2
fi

exec_args=""
for arg in "$@"; do
    escaped=${arg//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+="${exec_args:+ }\"$escaped\""
done

echo "=== Preparando dependencias de java-model-extractor ==="
mvn -f "$project_root/pom.xml" -pl modules/java-model-extractor -am -DskipTests install

echo "=== Analizando proyecto Java ==="
mvn -f "$module_dir/pom.xml" exec:java "-Dexec.args=$exec_args"