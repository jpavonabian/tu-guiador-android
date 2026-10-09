#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Calcula el SHA-256 de un APK y lo guarda en un archivo .sha256 junto a él, para publicarlo
# con la release. Quien lo descargue puede comprobarlo con: sha256sum -c <apk>.sha256
# Uso: scripts/hash-apk.sh [apk]   (por defecto build-reproducible/release/app-release.apk)
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="$(realpath "${1:-$raiz/build-reproducible/release/app-release.apk}")"

[ -f "$apk" ] || { echo "No existe el APK: $apk" >&2; exit 1; }
case "$apk" in
    *-unsigned.apk) echo "Aviso: es el APK sin firmar. Para publicar usa el firmado." >&2 ;;
esac

carpeta="$(dirname "$apk")"
nombre="$(basename "$apk")"
# Se calcula desde la carpeta del APK para que el .sha256 lleve solo el nombre, sin ruta.
# El sed quita el asterisco (modo binario) que añade sha256sum en Windows: deja "hash  nombre".
(cd "$carpeta" && sha256sum "$nombre" | sed 's/^\([0-9a-f]*\) \*/\1  /' | tee "$nombre.sha256")
echo "Guardado en: $carpeta/$nombre.sha256"
