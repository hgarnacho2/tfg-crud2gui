#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)
install_dependencies=false
script_args=()

for arg in "$@"; do
    if [[ "$arg" == "--install" ]]; then
        install_dependencies=true
    else
        script_args+=("$arg")
    fi
done

usage() {
    cat >&2 <<USAGE
Uso: $0 <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json] [--install]

Rutas relativas se resuelven desde el directorio actual.
Si no se indica salida, se genera output/model_yyyyMMdd_HHmmss.json en este módulo.
Por defecto no instala dependencias; usa --install para instalarlas antes del análisis.
Ejemplo:
  $0 modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio target/model.json
  $0 modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio --install
USAGE
    exit 2
}

if ((${#script_args[@]} == 0)) || [[ "${script_args[0]}" == "-h" || "${script_args[0]}" == "--help" ]]; then
    usage
fi
if [[ ! -d "${script_args[0]}" ]]; then
    echo "No existe la carpeta de código: ${script_args[0]}" >&2
    exit 2
fi

exec_args=""
for arg in "${script_args[@]}"; do
    escaped=${arg//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+="${exec_args:+ }\"$escaped\""
done

if [[ "$install_dependencies" == "true" ]]; then
    echo "=== Preparando dependencias de java-model-extractor ==="
    mvn -f "$project_root/pom.xml" -pl modules/java-model-extractor -am -DskipTests install
fi

echo "=== Analizando proyecto Java ==="
mvn -f "$module_dir/pom.xml" exec:java "-Dexec.args=$exec_args"