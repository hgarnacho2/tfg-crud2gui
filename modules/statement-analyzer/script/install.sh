#!/usr/bin/env bash
set -e

echo "=== Compilando e instalando el modulo statement-analyzer ==="
# Como el script esta en scripts/ dentro de statement-analyzer, subimos dos niveles para llegar a la raiz del proyecto
cd "$(dirname "$0")/.."
mvn clean install

echo "=== Instalacion completada con exito ==="