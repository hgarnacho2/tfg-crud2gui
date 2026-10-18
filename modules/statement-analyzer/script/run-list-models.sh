#!/usr/bin/env bash
set -e

if [ -z "$1" ]; then
    echo "No se ha indicado proveedor, se usará 'gemini' por defecto."
    PROVIDER="gemini"
else
    PROVIDER="$1"
fi

# Navegar a la raíz del proyecto statement-analyzer (subiendo desde la carpeta script/)
cd "$(dirname "$0")/.."

echo "=== Listando modelos disponibles para el proveedor: $PROVIDER ==="
mvn -q compile exec:java -Dexec.args="list $PROVIDER"