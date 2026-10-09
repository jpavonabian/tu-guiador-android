#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Compara un APK firmado por ti con uno compilado de cero desde una referencia de git,
# usando apksigcopier compare.
# Requiere Docker y apksigcopier (pip install apksigcopier). Si difieren, ejecuta diffoscope.
# Uso: scripts/verificar-firmado.sh <apk_firmado> [referencia_git]   (por defecto HEAD)
set -euo pipefail

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Uso: $0 <apk_firmado> [referencia_git]" >&2
    exit 2
fi

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
firmado="$(realpath "$1")"
referencia="${2:-HEAD}"
salida="$raiz/build-reproducible/firmado"
imagen=tu-guiador-build

rm -rf "$salida"
mkdir -p "$salida"

docker build -t "$imagen" "$raiz"
docker run --rm \
    -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" \
    -v "$raiz":/src:ro \
    -v "$salida":/out \
    "$imagen" \
    bash /src/scripts/compilar-release.sh /src "$referencia" /build /out

echo "Commit compilado: $(cat "$salida/commit.txt")"

if apksigcopier compare "$firmado" --unsigned "$salida/app-release-unsigned.apk"; then
    echo "VERIFICADO: el APK firmado coincide con la compilación desde cero."
    exit 0
fi

echo "NO COINCIDE: el APK firmado difiere de la compilación desde cero."
if ! command -v diffoscope > /dev/null; then
    echo "diffoscope no está instalado. En Debian/Ubuntu: sudo apt install diffoscope" >&2
    exit 1
fi
# Copia la firma de tu APK al compilado propio para comparar solo el contenido.
reconstruido="$salida/app-release-reconstruido.apk"
informe="$salida/informe-diffoscope.txt"
if apksigcopier copy "$firmado" "$salida/app-release-unsigned.apk" "$reconstruido"; then
    otro="$reconstruido"
else
    otro="$salida/app-release-unsigned.apk"
fi
diffoscope --text "$informe" "$firmado" "$otro" || true
echo "Informe de diffoscope: $informe"
exit 1
