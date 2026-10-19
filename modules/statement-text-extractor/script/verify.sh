#!/bin/bash

set -e

# Ir al directorio raíz del módulo Maven
cd "$(dirname "$0")/.."

echo "Verificando el módulo Maven statement-text-extractor..."
mvn clean verify

echo "Verificación completada correctamente."