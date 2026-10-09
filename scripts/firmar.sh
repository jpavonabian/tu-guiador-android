#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Firma el APK sin firmar con apksigner (build-tools del contenedor). No se hace zipalign:
# el APK de Gradle ya sale alineado, y alinear otra vez cambiaría el contenido y rompería la
# comprobación con apksigcopier. Por eso se pasa --alignment-preserved: sin él apksigner
# recoloca las entradas del ZIP y el APK firmado deja de coincidir con la compilación de cero.
# Solo esquema v2/v3 (minSdk 29, no hace falta v1).
# El keystore se monta en solo lectura; la contraseña se pide aquí y no aparece en la línea de
# comandos ni en el historial. Para no interactivo: KS_PASS en el entorno.
# Requiere Docker. Uso: scripts/firmar.sh <keystore> <alias> [apk_sin_firmar] [apk_firmado]
# Por defecto: build-reproducible/release/app-release-unsigned.apk -> build-reproducible/release/app-release.apk
set -euo pipefail

if [ $# -lt 2 ] || [ $# -gt 4 ]; then
    echo "Uso: $0 <keystore> <alias> [apk_sin_firmar] [apk_firmado]" >&2
    exit 2
fi

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
keystore="$(realpath "$1")"
alias_="$2"
sin_firmar="$(realpath "${3:-$raiz/build-reproducible/release/app-release-unsigned.apk}")"
firmado="$(realpath -m "${4:-$raiz/build-reproducible/release/app-release.apk}")"
imagen=tu-guiador-build

[ -f "$keystore" ] || { echo "No existe el keystore: $keystore" >&2; exit 1; }
[ -f "$sin_firmar" ] || { echo "No existe el APK sin firmar: $sin_firmar (ejecuta scripts/compilar.sh)" >&2; exit 1; }
[ "$firmado" != "$sin_firmar" ] || { echo "El APK firmado no puede ser el de entrada." >&2; exit 1; }

if [ -z "${KS_PASS:-}" ]; then
    read -r -s -p "Contraseña del keystore: " KS_PASS
    echo
fi
export KS_PASS

carpeta_salida="$(dirname "$firmado")"
mkdir -p "$carpeta_salida"

docker build -t "$imagen" "$raiz"
# apksigner usa la misma contraseña para el almacén y la clave. Si difieren, añade --key-pass.
docker run --rm \
    --user "$(id -u):$(id -g)" \
    -e KS_PASS \
    -v "$keystore":/clave/keystore:ro \
    -v "$sin_firmar":/entrada/app.apk:ro \
    -v "$carpeta_salida":/salida \
    "$imagen" \
    bash -c '
        set -euo pipefail
        apksigner="$ANDROID_HOME/build-tools/36.0.0/apksigner"
        "$apksigner" sign \
            --ks /clave/keystore --ks-key-alias "$0" --ks-pass env:KS_PASS \
            --alignment-preserved \
            --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true \
            --out "/salida/$1" /entrada/app.apk
        "$apksigner" verify --verbose --print-certs "/salida/$1"
    ' "$alias_" "$(basename "$firmado")"

echo "APK firmado: $firmado"
echo "Verifica que sale de este código: scripts/verificar-firmado.sh \"$firmado\" <referencia_git>"
