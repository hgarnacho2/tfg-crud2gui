#!/usr/bin/env bash
set -e

# Navegar a la raíz del proyecto multi-módulo (subiendo desde la carpeta script/)
cd "$(dirname "$0")/.."

echo "=== Compilando e instalando todos los módulos del proyecto ==="
mvn clean install

echo "=== ¡Proyecto compilado e instalado con éxito! ==="