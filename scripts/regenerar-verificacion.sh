#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Regenera gradle/verification-metadata.xml dentro del contenedor, para que incluya también los
# artefactos de Linux (aapt2, etc.). Úsalo tras cambiar una dependencia o un plugin.
# Trabaja sobre una copia de lo versionado en HEAD (git archive) y solo copia de vuelta el XML.
# Si el XML cambia, revisa el diff antes de commitear.
# Requiere Docker. Uso: scripts/regenerar-verificacion.sh
set -euo pipefail

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
imagen=tu-guiador-build
trabajo="$(mktemp -d)"
trap 'rm -rf "$trabajo"' EXIT

git -C "$raiz" archive --format=tar HEAD | tar -C "$trabajo" -xf -
# Parte del XML actual (incluido lo que aún no esté commiteado) para conservar las entradas existentes.
cp "$raiz/gradle/verification-metadata.xml" "$trabajo/gradle/verification-metadata.xml"

docker build -t "$imagen" "$raiz"
docker run --rm \
    -e HOST_UID="$(id -u)" -e HOST_GID="$(id -g)" \
    -v "$trabajo":/build \
    -w /build \
    "$imagen" \
    bash -c '
        set -euo pipefail
        export GRADLE_USER_HOME=/tmp/gradle-home
        bash ./gradlew --no-daemon --no-build-cache --console=plain \
            --write-verification-metadata sha256 \
            assembleDebug assembleRelease testDebugUnitTest lint
    ' || estado=$?
# El contenedor corre como root: devuelve todo a tu usuario para poder borrar la copia temporal.
docker run --rm -v "$trabajo":/build "$imagen" chown -R "$(id -u):$(id -g)" /build
[ "${estado:-0}" -eq 0 ] || exit "$estado"

cp "$trabajo/gradle/verification-metadata.xml" "$raiz/gradle/verification-metadata.xml"
echo "Actualizado gradle/verification-metadata.xml. Cambios:"
git -C "$raiz" diff --stat -- gradle/verification-metadata.xml
