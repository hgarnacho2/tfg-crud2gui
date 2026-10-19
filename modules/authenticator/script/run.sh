#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

cd "$project_root"

if [[ "${AUTHENTICATOR_DEBUG:-false}" == "true" ]]; then
    export MAVEN_OPTS="${MAVEN_OPTS:-} -Dorg.slf4j.simpleLogger.log.com.microsoft.aad.msal4j=debug"
fi

#echo "=== Preparando common-utils y authenticator ==="
#mvn -f "$project_root/pom.xml" -pl modules/authenticator -am -DskipTests install

echo "=== Iniciando autenticacion interactiva con Entra ID ==="
mvn -f "$module_dir/pom.xml" exec:java \
    "-Dexec.mainClass=com.crud2gui.authenticator.AuthenticatorMain"
