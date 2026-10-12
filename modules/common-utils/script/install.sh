#!/usr/bin/env bash
set -e

echo "=== Compilando e instalando el modulo common-utils ==="
# Como el script esta en scripts/ dentro de common-utils, subimos dos niveles para llegar a la raiz de common-utils
cd "$(dirname "$0")/.."
mvn clean install

echo "=== Instalacion completada con exito ==="