#!/usr/bin/env bash
set -e

echo "=== Compilando e instalando el modulo common ==="
# Como el script esta en script/ dentro de common, subimos un nivel para llegar a la raiz de common
cd "$(dirname "$0")/.."
mvn clean install

echo "=== Instalacion completada con exito ==="