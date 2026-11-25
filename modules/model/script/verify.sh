#!/bin/bash
set -e

cd "$(dirname "$0")/.."
echo "Verificando el módulo Maven model..."
mvn clean verify

echo "Verificación completada correctamente."
