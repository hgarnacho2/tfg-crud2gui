#!/usr/bin/env bash
set -e

echo "=== Ejecutando los tests del módulo statement-analyzer ==="
# Nos situamos en la raíz del módulo statement-analyzer (subiendo un nivel desde script/)
cd "$(dirname "$0")/.."
mvn test

echo "=== ¡Pruebas de statement-analyzer completadas con éxito! ==="