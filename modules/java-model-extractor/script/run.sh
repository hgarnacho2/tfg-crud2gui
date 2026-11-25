#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)
install_dependencies=false
show_info=false
script_args=()

# Match the Windows console to the UTF-8 output written by Main, then restore it.
if [[ "$OSTYPE" == msys* || "$OSTYPE" == cygwin* ]] && command -v chcp.com >/dev/null 2>&1; then
    console_code_page=$(chcp.com | grep -oE '[0-9]+$' || true)
    if [[ -n "$console_code_page" && "$console_code_page" != "65001" ]]; then
        chcp.com 65001 >/dev/null
        trap 'chcp.com "$console_code_page" >/dev/null' EXIT
    fi
fi

for arg in "$@"; do
    case "$arg" in
        --install) install_dependencies=true ;;
        --verbose) show_info=true ;;
        *) script_args+=("$arg") ;;
    esac
done

usage() {
    local header="" error_header="" reset=""
    if [[ -t 2 && "${TERM:-}" != "dumb" && -z "${NO_COLOR:-}" ]]; then
        header=$'\033[1;36m'
        error_header=$'\033[1;31m'
        reset=$'\033[0m'
    fi
    if [[ -n "${1:-}" ]]; then
        printf '\n%sError%s\n  %s\n\n' "$error_header" "$reset" "$1" >&2
    fi
    cat >&2 <<USAGE
${header}Uso${reset}
  $0 <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json] [--install] [--verbose]
  $0 --help

${header}Parámetros${reset}
  Parámetro                Descripción
  -----------------------  ----------------------------------------------------------
  <carpeta-con-codigo>     Obligatorio. Carpeta que contiene el código Java a analizar.
  [salida.json]            Opcional. Fichero de destino con extensión .json.
                           Su carpeta contenedora debe existir. (no se crean carpetas).
                           Si se omite, se usa JAVA_MODEL_OUTPUT_DIR y el nombre
                           modelo-<carpeta-de-codigo>.json. Si ya existe el fichero,
                           se añade _1, _2, etc. antes de la extensión.
  --manager=id             Opcional. Fuerza la clase gestora mediante su identificador
                           totalmente cualificado, en lugar de la selección automática.
  --config=fichero.json    Opcional. Configuración del detector de clase gestora.
                           Las opciones omitidas conservan sus valores predeterminados.
  --install                Opcional. Instala el módulo y sus dependencias del proyecto
                           antes de analizar, sin ejecutar pruebas. Si se omite, usa
                           las dependencias ya instaladas en Maven. El extractor se
                           compila siempre antes de ejecutar el análisis.
  --verbose                Opcional. Muestra los mensajes INFO de Maven; por defecto
                           se muestran solo sus avisos y errores.
  -h, --help               Muestra esta ayuda en lugar de ejecutar el análisis.

${header}Rutas${reset}
  Las rutas relativas de entrada, salida y configuración parten del directorio actual.
  JAVA_MODEL_OUTPUT_DIR relativo se resuelve desde la raíz del proyecto; si no se
  encuentra, desde el directorio actual. Sin configuración, se usa output del módulo
  o, si no se encuentra la raíz del proyecto, output del directorio actual.

${header}Ejemplos desde la raíz del proyecto${reset}
  ./modules/java-model-extractor/script/run.sh modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio modelo.json
  ./modules/java-model-extractor/script/run.sh modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio --install
  ./modules/java-model-extractor/script/run.sh modules/java-model-extractor/src/test/resources/fixtures/manager-portfolio --verbose

${header}Ejemplos desde modules/java-model-extractor${reset}
  ./script/run.sh src/test/resources/fixtures/manager-portfolio modelo.json
  ./script/run.sh src/test/resources/fixtures/manager-portfolio --install
USAGE
    exit 2
}

if ((${#script_args[@]} == 0)); then
    usage "Falta el parámetro obligatorio <carpeta-con-codigo>."
fi
if [[ "${script_args[0]}" == "-h" || "${script_args[0]}" == "--help" ]]; then
    usage
fi
if [[ "${script_args[0]}" == --manager=* || "${script_args[0]}" == --config=* ]]; then
    usage "Falta el parámetro obligatorio <carpeta-con-codigo> antes de las opciones."
fi
if [[ ! -d "${script_args[0]}" ]]; then
    echo "No existe la carpeta de código: ${script_args[0]}" >&2
    exit 2
fi

output_arg=""
output_arg_provided=false
for ((i = 1; i < ${#script_args[@]}; i++)); do
    case "${script_args[i]}" in
        --manager=*|--config=*|--*) continue ;;
        *)
            output_arg=${script_args[i]}
            output_arg_provided=true
            break
            ;;
    esac
done
if [[ "$output_arg_provided" == "true" && ! "$output_arg" =~ \.[jJ][sS][oO][nN]$ ]]; then
    usage "La ruta de salida debe terminar en .json: $output_arg"
fi

header="" reset=""
if [[ -t 1 && "${TERM:-}" != "dumb" && -z "${NO_COLOR:-}" ]]; then
    header=$'\033[1;32m'
    reset=$'\033[0m'
fi

printf '\n%sAnalizando código java ...%s\n\n' "$header" "$reset"

maven=(mvn)
if [[ "$show_info" == "true" ]]; then
    maven+=("-Dorg.slf4j.simpleLogger.defaultLogLevel=info")
else
    maven+=("-Dorg.slf4j.simpleLogger.defaultLogLevel=warn")
fi

exec_args=""
for arg in "${script_args[@]}"; do
    escaped=${arg//\\/\\\\}
    escaped=${escaped//\"/\\\"}
    exec_args+="${exec_args:+ }\"$escaped\""
done

if [[ "$install_dependencies" == "true" ]]; then
    printf '\n=== Preparando dependencias de java-model-extractor ===\n'
    "${maven[@]}" -f "$project_root/pom.xml" -pl modules/java-model-extractor -am -DskipTests install
fi

"${maven[@]}" -f "$module_dir/pom.xml" \
    "-Dcrud2gui.extractor.install=$install_dependencies" \
    "-Dcrud2gui.extractor.verbose=$show_info" \
    compile exec:java "-Dexec.args=$exec_args"