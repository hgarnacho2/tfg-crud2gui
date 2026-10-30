#!/usr/bin/env bash
set -e

# Nos situamos siempre en la raíz del proyecto (subiendo un nivel desde script/)
cd "$(dirname "$0")/.."

# Si le pasas un argumento (ej: ./script/test.sh modules/common), ejecuta solo ese módulo.
# Si no le pasas nada, puedes decidir si ejecuta todo o avisa.
if [ -n "$1" ]; then
    echo "=== Ejecutando tests para el módulo: $1 ==/="
    mvn test -pl "$1"
else
    echo "=== Ejecutando los tests de todo el proyecto ==="
    mvn test
fi

echo "=== ¡Pruebas completadas con éxito! ==="