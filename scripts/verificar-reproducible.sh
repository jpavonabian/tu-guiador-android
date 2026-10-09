#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Compila el release sin firmar dos veces, en contenedores limpios y con entornos distintos,
# a partir de una referencia de git, y compara los APK.
# Requiere Docker. Si difieren, ejecuta diffoscope (instalado en el sistema) y guarda el informe en texto.
# Uso: scripts/verificar-reproducible.sh [referencia_git]   (por defecto HEAD)
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
referencia="${1:-HEAD}"
salida="$raiz/build-reproducible"
imagen=tu-guiador-build

rm -rf "$salida"
mkdir -p "$salida/1" "$salida/2"

docker build -t "$imagen" "$raiz"

# Los scripts se leen de este árbol (:ro); el código a compilar sale de la referencia con git archive.
compilar() {
    local n="$1" trabajo="$2" zona="$3" umask_="$4" host="$5" fecha="$6"
    docker run --rm \
        --hostname "$host" \
        -e TZ="$zona" \
        -e UMASK="$umask_" \
        -e FECHA_ADELANTADA="$fecha" \
        -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" \
        -v "$raiz":/src:ro \
        -v "$salida/$n":/out \
        "$imagen" \
        bash /src/scripts/compilar-release.sh /src "$referencia" "$trabajo" /out
}

echo "== Compilación 1 de 2: ruta corta, UTC, umask 0022, hostname build-uno =="
compilar 1 /build UTC 0022 build-uno ""
echo "== Compilación 2 de 2: ruta larga, UTC+14, umask 0077, hostname build-dos, fecha +30 días =="
compilar 2 /tmp/una/ruta/de/trabajo/mucho/mas/larga/que/la/primera/proyecto Pacific/Kiritimati 0077 build-dos "+30d"

echo "Commit compilado: $(cat "$salida/1/commit.txt") y $(cat "$salida/2/commit.txt")"
sha256sum "$salida/1/app-release-unsigned.apk" "$salida/2/app-release-unsigned.apk"

if cmp -s "$salida/1/app-release-unsigned.apk" "$salida/2/app-release-unsigned.apk"; then
    echo "REPRODUCIBLE: los dos APK son idénticos byte a byte."
    exit 0
fi

echo "NO REPRODUCIBLE: los APK difieren."
if ! command -v diffoscope > /dev/null; then
    echo "diffoscope no está instalado. En Debian/Ubuntu: sudo apt install diffoscope" >&2
    exit 1
fi
informe="$salida/informe-diffoscope.txt"
diffoscope --text "$informe" "$salida/1/app-release-unsigned.apk" "$salida/2/app-release-unsigned.apk" || true
echo "Informe de diffoscope: $informe"
exit 1
