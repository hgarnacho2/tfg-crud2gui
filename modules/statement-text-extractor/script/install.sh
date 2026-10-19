#!/usr/bin/env bash
set -e

echo "=== Compilando e instalando el modulo statement-text-extractor ==="
# El script vive dentro de statement-text-extractor; subimos a la raiz del modulo.
cd "$(dirname "$0")/.."
mvn clean install

echo "=== Instalación completada con éxito ==="