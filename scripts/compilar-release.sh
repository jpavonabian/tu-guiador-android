#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Se ejecuta DENTRO del contenedor (ver Dockerfile). Compila el release sin firmar
# a partir de lo versionado en una referencia de git (commit o tag), no del árbol de trabajo.
# Uso: compilar-release.sh <repositorio_solo_lectura> <referencia> <carpeta_de_trabajo> <carpeta_salida>
# Variables opcionales:
#   UMASK              umask con el que se compila (por ejemplo 0077).
#   FECHA_ADELANTADA   desplazamiento de faketime (por ejemplo +30d).
#   HOST_UID, HOST_GID propietario con el que quedan los archivos de salida.
set -euo pipefail

if [ $# -ne 4 ]; then
    echo "Uso: $0 <repositorio_solo_lectura> <referencia> <carpeta_de_trabajo> <carpeta_salida>" >&2
    echo "Este script corre dentro del contenedor. Para compilar desde tu máquina usa scripts/compilar.sh." >&2
    exit 2
fi

repositorio="$1"
referencia="$2"
trabajo="$3"
salida="$4"

umask "${UMASK:-0022}"

# Solo se compila lo que está en el commit: nada del árbol de trabajo.
commit="$(git -c safe.directory='*' -C "$repositorio" rev-parse --verify "$referencia^{commit}")"
rm -rf "$trabajo"
mkdir -p "$trabajo"
git -c safe.directory='*' -C "$repositorio" archive --format=tar "$commit" | tar -C "$trabajo" -xf -

cd "$trabajo"
export GRADLE_USER_HOME=/tmp/gradle-home
gradle=(bash ./gradlew --no-daemon --no-build-cache --console=plain)
if [ -n "${FECHA_ADELANTADA:-}" ]; then
    # Con la fecha adelantada caducan los certificados TLS: primero se descarga todo con el reloj
    # real, y la compilación que cuenta se hace con el reloj adelantado y sin red.
    "${gradle[@]}" assembleRelease
    "${gradle[@]}" clean
    faketime -f "$FECHA_ADELANTADA" "${gradle[@]}" --offline assembleRelease
else
    "${gradle[@]}" assembleRelease
fi

cp app/build/outputs/apk/release/app-release-unsigned.apk "$salida/app-release-unsigned.apk"
echo "$commit" > "$salida/commit.txt"

# El contenedor corre como root: devuelve los archivos al usuario que lanzó el script.
if [ -n "${HOST_UID:-}" ]; then
    chown "$HOST_UID:${HOST_GID:-$HOST_UID}" "$salida/app-release-unsigned.apk" "$salida/commit.txt"
fi
