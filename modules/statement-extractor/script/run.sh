#!/usr/bin/env bash
set -euo pipefail

# --- Rutas derivadas de la ubicación del propio script (no dependen del directorio actual) ---
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
pdf_dir=$(cd -- "$script_dir/.." && pwd)
samples_dir="$pdf_dir/src/test/resources"           # donde se busca si solo se indica el nombre
output_root="${PDF_OUTPUT_DIR:-$pdf_dir/output}"    # salida: output/<nombre>/
main_class="com.crud2gui.statement.extractor.Main"

usage() {
    cat >&2 <<USAGE
Uso: $0 <pdf>
  <pdf> puede ser una ruta (absoluta o relativa), o solo el nombre del fichero
  (con o sin .pdf), que se busca por defecto en src/test/resources.
  Genera output/<nombre>/<nombre>.txt y output/<nombre>/<nombre>-filtrado.txt
Ejemplos:
  $0 enunciado_practica_23
  $0 enunciado_practica_23.pdf
  $0 "C:/otra/carpeta/enunciado.pdf"
USAGE
    exit 1
}

if (($# != 1)) || [[ "$1" == "-h" || "$1" == "--help" ]]; then
    usage
fi

arg=$1

# Rutas de Windows (C:\... o C:/...) -> formato de Git Bash
if command -v cygpath >/dev/null 2>&1 && [[ "$arg" == [A-Za-z]:* ]]; then
    arg=$(cygpath -u "$arg")
fi

# Busca el PDF tal cual y, si no existe, en la carpeta de ejemplos.
input_pdf=""
for candidate in "$arg" "$samples_dir/$arg" "$samples_dir/$arg.pdf"; do
    if [[ -f "$candidate" ]]; then
        input_pdf="$(cd -- "$(dirname -- "$candidate")" && pwd)/$(basename -- "$candidate")"
        break
    fi
done

if [[ -z "$input_pdf" ]]; then
    echo "No se encuentra el PDF: $1 (buscado también en $samples_dir)" >&2
    exit 1
fi

name=$(basename -- "$input_pdf")
base=${name%.*}
out_dir="$output_root/$base"
mkdir -p "$out_dir"
output_txt="$out_dir/$base.txt"
filtered_txt="$out_dir/$base-filtrado.txt"

if command -v cygpath >/dev/null 2>&1; then
    input_pdf=$(cygpath -m "$input_pdf")
    output_txt=$(cygpath -m "$output_txt")
    filtered_txt=$(cygpath -m "$filtered_txt")
fi

# 'compile' es necesario: exec:java por sí solo no compila las clases.
mvn -q -f "$pdf_dir/pom.xml" compile exec:java -e \
    "-Dexec.mainClass=$main_class" \
    "-Dexec.args=\"$input_pdf\" \"$output_txt\" \"$filtered_txt\""
