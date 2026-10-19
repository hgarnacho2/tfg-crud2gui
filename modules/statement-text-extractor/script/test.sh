#!/usr/bin/env bash
set -e

echo "=== Ejecutando los tests del módulo statement-text-extractor ==="
# Nos situamos en la raíz del módulo (subiendo un nivel desde script/)
cd "$(dirname "$0")/.."
mvn test

echo "=== Pruebas de statement-text-extractor completadas con éxito ==="