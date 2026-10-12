#!/usr/bin/env bash
set -e

echo "=== Ejecutando los tests del módulo common-utils ==="
# Nos situamos en la raíz del módulo common-utils (subiendo un nivel desde script/)
cd "$(dirname "$0")/.."
mvn test

echo "=== ¡Pruebas de common-utils completadas con éxito! ==="