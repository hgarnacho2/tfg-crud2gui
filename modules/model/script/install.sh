#!/usr/bin/env bash
set -e

echo "=== Compilando e instalando el módulo model ==="
cd "$(dirname "$0")/.."
mvn clean install

echo "=== Instalación completada con éxito ==="
