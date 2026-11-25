#!/usr/bin/env bash
set -e

echo "=== Ejecutando las pruebas del módulo model ==="
cd "$(dirname "$0")/.."
mvn test

echo "=== Pruebas de model completadas con éxito ==="
