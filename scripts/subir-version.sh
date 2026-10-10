#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Prepara el código para una versión nueva: cambia versionName y versionCode en
# app/build.gradle.kts y crea los changelogs de fastlane (es-ES y en-US) para ese versionCode.
# Solo toca esas dos líneas: si el patrón no encaja en exactamente una línea, aborta sin
# cambiar nada. No hace commit: revisa el diff y commitea tú. Hay que estar en una rama
# distinta de main y con el árbol limpio.
# Uso: scripts/subir-version.sh [versionName] [versionCode]   (lo que falte, se pregunta)
# Ejemplo: scripts/subir-version.sh 1.1 2
set -euo pipefail

if [ $# -gt 2 ]; then
    echo "Uso: $0 [versionName] [versionCode]" >&2
    exit 2
fi

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle="$raiz/app/build.gradle.kts"
carpeta_es="$raiz/fastlane/metadata/android/es-ES/changelogs"
carpeta_en="$raiz/fastlane/metadata/android/en-US/changelogs"
marca_pendiente="PENDIENTE"
limite_changelog=500

cd "$raiz"

git rev-parse --is-inside-work-tree > /dev/null 2>&1 || { echo "Esto no es un repositorio git." >&2; exit 1; }
rama="$(git symbolic-ref --short -q HEAD || true)"
if [ -z "$rama" ] || [ "$rama" = main ]; then
    echo "Estás en '${rama:-HEAD suelto}'. Crea una rama para la subida de versión (no se commitea en main)." >&2
    exit 1
fi
[ -z "$(git status --porcelain)" ] || { echo "El árbol de trabajo no está limpio. Haz commit o guarda los cambios antes." >&2; exit 1; }
[ -f "$gradle" ] || { echo "No existe $gradle" >&2; exit 1; }

# Devuelve el valor capturado por el patrón sed si hay exactamente una línea que encaja.
leer_unico() {
    local resultado
    resultado="$(sed -n "$1" "$gradle")"
    if [ -z "$resultado" ] || [ "$(printf '%s\n' "$resultado" | wc -l)" -ne 1 ]; then
        echo "No encuentro una única línea para '$2' en $gradle. No se cambia nada." >&2
        exit 1
    fi
    printf '%s' "$resultado"
}

codigo_actual="$(leer_unico 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)[[:space:]]*$/\1/p' versionCode)"
nombre_actual="$(leer_unico 's/^[[:space:]]*versionName = "\([^"]*\)"[[:space:]]*$/\1/p' versionName)"
echo "Versión actual: $nombre_actual (versionCode $codigo_actual)"

nombre="${1:-}"
codigo="${2:-}"
if [ -z "$nombre" ]; then
    read -r -p "Nuevo versionName (por ejemplo 1.1): " nombre
fi
if [ -z "$codigo" ]; then
    read -r -p "Nuevo versionCode (mayor que $codigo_actual): " codigo
fi

[[ "$nombre" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]] || { echo "versionName no válido: '$nombre'. Usa números separados por puntos, como 1.1 o 1.1.2." >&2; exit 1; }
[[ "$codigo" =~ ^[1-9][0-9]*$ ]] || { echo "versionCode no válido: '$codigo'. Debe ser un entero positivo." >&2; exit 1; }
[ "$codigo" -gt "$codigo_actual" ] || { echo "El versionCode ($codigo) debe ser mayor que el actual ($codigo_actual)." >&2; exit 1; }
mayor="$(printf '%s\n%s\n' "$nombre_actual" "$nombre" | sort -V | tail -n 1)"
if [ "$nombre" = "$nombre_actual" ] || [ "$mayor" != "$nombre" ]; then
    echo "El versionName ($nombre) debe ser mayor que el actual ($nombre_actual)." >&2
    exit 1
fi

if git rev-parse -q --verify "refs/tags/v$nombre" > /dev/null; then
    echo "Ya existe el tag v$nombre." >&2
    exit 1
fi
cambios_es="$carpeta_es/$codigo.txt"
cambios_en="$carpeta_en/$codigo.txt"
for archivo in "$cambios_es" "$cambios_en"; do
    [ ! -e "$archivo" ] || { echo "Ya existe $archivo. No se sobrescribe." >&2; exit 1; }
done

# Lee líneas hasta una línea vacía (o el fin de la entrada). Si no se escribe nada, devuelve
# la marca de pendiente, que release.sh rechaza hasta que se sustituya por el texto real.
pedir_changelog() {
    local idioma="$1" linea texto=""
    echo "Changelog en $idioma. Escribe el texto y termina con una línea vacía (vacío del todo = pendiente):" >&2
    while IFS= read -r linea; do
        [ -n "$linea" ] || break
        texto="${texto:+$texto$'\n'}$linea"
    done
    if [ -z "$texto" ]; then
        texto="$marca_pendiente: escribe aquí el texto de la versión $nombre."
    fi
    if [ "${#texto}" -gt "$limite_changelog" ]; then
        echo "Aviso: el changelog en $idioma tiene ${#texto} caracteres; F-Droid admite $limite_changelog como máximo." >&2
    fi
    printf '%s' "$texto"
}

texto_es="$(pedir_changelog español)"
texto_en="$(pedir_changelog inglés)"

# Edición de build.gradle.kts: a un temporal, se comprueba, y se renombra encima.
temporal="$(mktemp "$gradle.XXXXXX")"
trap 'rm -f "$temporal"' EXIT
sed -e "s/^\([[:space:]]*versionCode = \)[0-9][0-9]*/\1$codigo/" \
    -e "s/^\([[:space:]]*versionName = \"\)[^\"]*/\1$nombre/" \
    "$gradle" > "$temporal"
if [ "$(grep -c "^[[:space:]]*versionCode = $codigo[[:space:]]*$" "$temporal")" -ne 1 ] \
    || [ "$(grep -c "^[[:space:]]*versionName = \"$nombre\"[[:space:]]*$" "$temporal")" -ne 1 ]; then
    echo "La edición no ha dejado los valores esperados. No se cambia nada." >&2
    exit 1
fi
chmod --reference="$gradle" "$temporal"
mv "$temporal" "$gradle"

mkdir -p "$carpeta_es" "$carpeta_en"
printf '%s\n' "$texto_es" > "$cambios_es"
printf '%s\n' "$texto_en" > "$cambios_en"

echo
git status --short
git diff --stat
echo
echo "Hecho: versionName $nombre, versionCode $codigo."
if grep -q "^$marca_pendiente" "$cambios_es" "$cambios_en"; then
    echo "Quedan changelogs pendientes: edita $cambios_es y $cambios_en."
fi
echo "Revisa 'git diff' y commitea. Cuando esté en main: scripts/release.sh $nombre"
