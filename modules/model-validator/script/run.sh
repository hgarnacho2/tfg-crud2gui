#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)
install_dependencies=false
validation_config="$module_dir/conf/validation-options.json"
model_files=()

for arg in "$@"; do
    if [[ "$arg" == "--install" ]]; then
        install_dependencies=true
    elif [[ "$arg" == --config=* ]]; then
        validation_config=${arg#--config=}
    else
        model_files+=("$arg")
    fi
done

if ((${#model_files[@]} != 2)) || [[ "${model_files[0]}" == "-h" || "${model_files[0]}" == "--help" ]]; then
    echo "Uso: $0 <modelo-esperado.json> <modelo-extraido.json> [--config=fichero.json] [--install]" >&2
    echo "Por defecto usa $validation_config; --config permite un override puntual." >&2
    echo "Por defecto no instala dependencias; usa --install para instalarlas antes de comparar." >&2
    exit 2
fi
for file in "${model_files[@]}"; do
    if [[ ! -f "$file" ]]; then
        echo "No existe el fichero JSON: $file" >&2
        exit 2
    fi
done
if [[ ! -f "$validation_config" ]]; then
    echo "No existe la configuracion de validacion: $validation_config" >&2
    exit 2
fi

exec_args=""
for arg in "${model_files[@]}"; do
    escaped=${arg//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+="${exec_args:+ }\"$escaped\""
done
model_name=${model_files[0]##*/}
model_name=${model_name%.*}
model_name=$(printf '%s' "$model_name" | sed -E 's/[^A-Za-z0-9_-]+/_/g; s/^_+|_+$//g')
if [[ -z "$model_name" ]]; then
    model_name="model"
fi
timestamp=$(date '+%Y%m%d_%H%M%S')
report_name="validation_${model_name}_${timestamp}"
output_file="$module_dir/output/${report_name}.json"
suffix=1
while [[ -e "$output_file" ]]; do
    output_file="$module_dir/output/${report_name}_${suffix}.json"
    suffix=$((suffix + 1))
done
escaped=${output_file//\\/\\\\}
escaped=${escaped//\"/\\\"}
exec_args+=" \"$escaped\""
if [[ -n "$validation_config" ]]; then
    escaped=${validation_config//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+=" --config=\"$escaped\""
fi

if [[ "$install_dependencies" == "true" ]]; then
    mvn -f "$project_root/pom.xml" -pl modules/model-validator -am -DskipTests install
fi
mvn -f "$module_dir/pom.xml" exec:java "-Dexec.args=$exec_args"