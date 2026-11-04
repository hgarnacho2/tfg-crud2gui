#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
module_dir=$(cd -- "$script_dir/.." && pwd)
project_root=$(cd -- "$script_dir/../../.." && pwd)

install_dependencies=false
show_info=false

usage() {
	cat <<USAGE
Uso: $0 [opciones]

Opciones:
	--install      Instala desktop-app y sus dependencias antes de arrancar.
	--no-install   Alias compatible; omite la instalación (comportamiento predeterminado).
  --debug        Muestra la salida INFO de Maven y el diagnóstico DEBUG de MSAL.
  -h, --help     Muestra esta ayuda.

Ejemplos:
	$0
	$0 --install
	$0 --no-install
	$0 --debug --no-install
USAGE
}

for arg in "$@"; do
	case "$arg" in
		--install)
			install_dependencies=true
			;;
		--no-install)
			install_dependencies=false
			;;
		--debug)
			show_info=true
			;;
		-h|--help)
			usage
			exit 0
			;;
		*)
			echo "Opción desconocida: $arg" >&2
			usage >&2
			exit 2
			;;
	esac
done

cd "$project_root"

maven=(mvn)
if [[ "$show_info" != "true" ]]; then
	maven+=(-q)
else
	export MAVEN_OPTS="${MAVEN_OPTS:-} -Dorg.slf4j.simpleLogger.log.com.microsoft.aad.msal4j=debug"
fi

if [[ "$install_dependencies" == "true" ]]; then
	echo "=== Preparando dependencias de CRUD2GUI Desktop App ==="
	"${maven[@]}" -f "$project_root/pom.xml" -pl modules/desktop-app -am -DskipTests install
fi

echo "=== Iniciando CRUD2GUI Desktop App ==="
echo "Opciones disponibles: --install, --no-install, --debug, -h, --help"
echo "Usa '$0 --help' para ver descripciones y ejemplos."
printf 'Opciones seleccionadas:'
if (($# == 0)); then
	printf ' (valores predeterminados)'
else
	printf ' %q' "$@"
fi
printf '\n'
"${maven[@]}" -f "$module_dir/pom.xml" javafx:run
