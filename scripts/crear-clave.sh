#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Crea un almacén de claves (keystore) nuevo para firmar la app, con el keytool del contenedor.
# keytool pide las contraseñas y los datos del certificado de forma interactiva.
# Guarda el archivo FUERA del repositorio y haz copia de seguridad: si lo pierdes, no podrás
# publicar actualizaciones con la misma firma. Nunca lo subas al repo.
# Requiere Docker. Uso: scripts/crear-clave.sh <ruta_del_keystore> [alias]   (alias por defecto: tuguiador)
set -euo pipefail

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Uso: $0 <ruta_del_keystore> [alias]" >&2
    exit 2
fi

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
destino="$(realpath -m "$1")"
alias_="${2:-tuguiador}"
imagen=tu-guiador-build

if [ -e "$destino" ]; then
    echo "Ya existe $destino. No se sobrescribe." >&2
    exit 1
fi
case "$destino" in
    "$raiz"/*) echo "El keystore no debe estar dentro del repositorio." >&2; exit 1 ;;
esac

carpeta="$(dirname "$destino")"
mkdir -p "$carpeta"

docker build -t "$imagen" "$raiz"
# Validez de 10000 días (~27 años), RSA 4096.
docker run --rm -it \
    --user "$(id -u):$(id -g)" \
    -v "$carpeta":/clave \
    "$imagen" \
    keytool -genkeypair -v \
        -keystore "/clave/$(basename "$destino")" \
        -alias "$alias_" \
        -keyalg RSA -keysize 4096 -validity 10000

chmod 600 "$destino"
echo "Keystore creado: $destino (alias: $alias_). Haz copia de seguridad."
