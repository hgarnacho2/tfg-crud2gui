#!/usr/bin/env bash
set -e

echo "=== Ejecutando los tests del módulo common ==="
# Nos situamos en la raíz del módulo common (subiendo un nivel desde script/)
cd "$(dirname "$0")/.."
mvn test

echo "=== ¡Pruebas de common completadas con éxito! ==="