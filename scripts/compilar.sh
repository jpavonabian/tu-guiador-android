#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Compila el release SIN firmar en el entorno fijado (Docker), a partir de una referencia de git.
# Salida: build-reproducible/release/app-release-unsigned.apk y commit.txt.
# Requiere Docker. Uso: scripts/compilar.sh [referencia_git]   (por defecto HEAD)
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
referencia="${1:-HEAD}"
salida="$raiz/build-reproducible/release"
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
echo "APK sin firmar: $salida/app-release-unsigned.apk"
