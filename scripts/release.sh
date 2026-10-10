#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Saca una release firmada y verificable de una versión que ya está en main: tag local,
# comprobación de reproducibilidad, compilar, firmar, hash, verificar el firmado, subir a
# origin y publicar en GitHub con gh. Las dos acciones irreversibles (subir y publicar) piden
# confirmación; todo lo anterior se puede repetir. Nunca borra el tag local ni se salta la
# comprobación de reproducibilidad.
# Estado de pasos en build-reproducible/.release-estado: si falla, vuelve a lanzar el mismo
# comando y retoma donde se quedó (si HEAD sigue igual). --desde-cero lo descarta.
# Requiere Docker, apksigcopier y gh (gh solo si no usas --solo-local).
# Uso: scripts/release.sh [--solo-local] [--desde-cero] <versionName>
# Variables: KEYSTORE (~/claves/tuguiador.jks), ALIAS (tuguiador), CARPETA_RELEASE (~/release),
#            KS_PASS (contraseña del keystore; si falta, se pide una vez)
# --solo-local: llega hasta verificar el firmado y para, sin tocar la red.
set -euo pipefail

solo_local=0
desde_cero=0
version=""
for argumento in "$@"; do
    case "$argumento" in
        --solo-local) solo_local=1 ;;
        --desde-cero) desde_cero=1 ;;
        -*) echo "Opción desconocida: $argumento" >&2; exit 2 ;;
        *) [ -z "$version" ] || { echo "Solo se admite una versión." >&2; exit 2; }; version="$argumento" ;;
    esac
done
if [ -z "$version" ]; then
    echo "Uso: $0 [--solo-local] [--desde-cero] <versionName>" >&2
    exit 2
fi
[[ "$version" =~ ^[0-9]+(\.[0-9]+){1,2}$ ]] || { echo "Versión no válida: '$version'. Ejemplo: 1.1" >&2; exit 2; }

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
scripts="$raiz/scripts"
gradle="$raiz/app/build.gradle.kts"
tag="v$version"
keystore="${KEYSTORE:-$HOME/claves/tuguiador.jks}"
alias_="${ALIAS:-tuguiador}"
carpeta_release="${CARPETA_RELEASE:-$HOME/release}"
estado="$raiz/build-reproducible/.release-estado"
sin_firmar="$raiz/build-reproducible/release/app-release-unsigned.apk"
firmado="$raiz/build-reproducible/release/app-release.apk"
apk="$carpeta_release/tuguiador-$version.apk"
sha="$apk.sha256"
temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT

paso() { echo; echo "== $* =="; }
fallo() { echo "$*" >&2; exit 1; }
confirmar() {
    local respuesta
    read -r -p "$1 [s/N] " respuesta
    [ "$respuesta" = s ] || [ "$respuesta" = S ] || fallo "Cancelado. Vuelve a lanzar el script para retomar."
}
hecho() { [ -f "$estado" ] && grep -qx "hecho=$1" "$estado"; }
marcar() { echo "hecho=$1" >> "$estado"; }

cd "$raiz"

# --- Comprobaciones previas: nada se ha modificado todavía ---
paso "Comprobaciones previas"

git rev-parse --is-inside-work-tree > /dev/null 2>&1 || fallo "Esto no es un repositorio git."
[ "$(git symbolic-ref --short -q HEAD || true)" = main ] || fallo "Hay que estar en main."
[ -z "$(git status --porcelain)" ] || fallo "El árbol de trabajo no está limpio."

nombre_gradle="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"[[:space:]]*$/\1/p' "$gradle")"
codigo="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)[[:space:]]*$/\1/p' "$gradle")"
[ -n "$nombre_gradle" ] && [ -n "$codigo" ] || fallo "No puedo leer versionName/versionCode de $gradle."
[ "$nombre_gradle" = "$version" ] || fallo "build.gradle.kts tiene versionName $nombre_gradle, no $version. ¿Falta scripts/subir-version.sh o el merge a main?"

cambios_es="$raiz/fastlane/metadata/android/es-ES/changelogs/$codigo.txt"
cambios_en="$raiz/fastlane/metadata/android/en-US/changelogs/$codigo.txt"
for archivo in "$cambios_es" "$cambios_en"; do
    [ -s "$archivo" ] || fallo "Falta el changelog (o está vacío): $archivo"
    ! grep -q '^PENDIENTE' "$archivo" || fallo "El changelog sigue pendiente: $archivo"
done

command -v docker > /dev/null && docker info > /dev/null 2>&1 || fallo "Docker no responde. En WSL, activa la integración de Docker Desktop o arranca el servicio."
command -v apksigcopier > /dev/null || fallo "Falta apksigcopier. Instálalo con: pipx install apksigcopier"
if [ "$solo_local" -eq 0 ]; then
    command -v gh > /dev/null || fallo "Falta la CLI gh."
    gh auth status > /dev/null 2>&1 || fallo "gh no tiene sesión. Ejecuta: gh auth login"
    git fetch --quiet origin
    atras="$(git rev-list --count main..origin/main)"
    [ "$atras" -eq 0 ] || fallo "main va $atras commits por detrás de origin/main. Haz pull antes."
fi

if [ ! -f "$keystore" ]; then
    read -r -e -p "No existe $keystore. Ruta del keystore: " keystore
    [ -f "$keystore" ] || fallo "No existe el keystore: $keystore"
fi

# --- Estado: se descarta si el código cambió o se pide desde cero ---
mkdir -p "$raiz/build-reproducible"
commit="$(git rev-parse HEAD)"
if [ -f "$estado" ]; then
    if [ "$desde_cero" -eq 1 ] || ! grep -qx "version=$version" "$estado" || ! grep -qx "commit=$commit" "$estado"; then
        echo "Descartando estado anterior (otra versión, otro commit u opción --desde-cero)."
        rm -f "$estado"
    fi
fi
if [ ! -f "$estado" ]; then
    printf 'version=%s\ncommit=%s\n' "$version" "$commit" > "$estado"
fi

if hecho comprobado; then
    echo "La release $tag ya está publicada y comprobada. Nada que hacer."
    exit 0
fi

# El tag publicado no se mueve: solo es válido si lo subimos nosotros en una ejecución anterior.
if [ "$solo_local" -eq 0 ] && ! hecho subido; then
    if git ls-remote --exit-code --tags origin "refs/tags/$tag" > /dev/null 2>&1; then
        fallo "El tag $tag ya existe en origin y no lo subió esta ejecución. Un tag publicado no se mueve."
    fi
fi
if [ "$solo_local" -eq 0 ] && ! hecho publicado; then
    if gh release view "$tag" > /dev/null 2>&1; then
        fallo "Ya existe una release $tag en GitHub. Míralo con: gh release view $tag"
    fi
fi

# --- 1. Tag local ---
paso "1. Tag local $tag"
if git rev-parse -q --verify "refs/tags/$tag" > /dev/null; then
    [ "$(git rev-parse "refs/tags/$tag^{commit}")" = "$commit" ] \
        || fallo "El tag local $tag apunta a otro commit que HEAD. Revísalo; el script no lo borra."
    echo "El tag $tag ya existe y apunta a HEAD."
else
    git tag -a "$tag" -m "$tag"
    echo "Creado $tag en $commit."
fi
marcar tag

# --- 2. Reproducibilidad (nunca se omite) ---
paso "2. Comprobar que el build es reproducible"
if hecho reproducible; then
    echo "Ya hecho, se omite."
else
    "$scripts/verificar-reproducible.sh" "$tag"
    marcar reproducible
fi

# --- 3. Compilar y firmar ---
paso "3. Compilar y firmar"
if hecho firmado && [ -f "$firmado" ]; then
    echo "Ya hecho, se omite."
else
    if [ -z "${KS_PASS:-}" ]; then
        read -r -s -p "Contraseña del keystore: " KS_PASS
        echo
    fi
    export KS_PASS
    "$scripts/compilar.sh" "$tag"
    "$scripts/firmar.sh" "$keystore" "$alias_"
    marcar firmado
fi

# --- 4. Copiar y calcular el hash ---
paso "4. Copiar el APK a $carpeta_release y calcular el SHA-256"
if hecho copiado && [ -f "$apk" ] && [ -f "$sha" ]; then
    echo "Ya hecho, se omite."
else
    [ ! -e "$apk" ] || fallo "Ya existe $apk. No se sobrescribe (podría ser el APK ya publicado). Bórralo a mano si es un resto de un intento."
    [ ! -e "$sha" ] || fallo "Ya existe $sha. Bórralo a mano si es un resto de un intento."
    mkdir -p "$carpeta_release"
    cp "$firmado" "$apk"
    cmp -s "$firmado" "$apk" || { rm -f "$apk"; fallo "La copia no coincide con el APK firmado."; }
    "$scripts/hash-apk.sh" "$apk" || { rm -f "$apk" "$sha"; fallo "No se pudo calcular el hash."; }
    marcar copiado
fi
hash="$(cut -d' ' -f1 "$sha")"
[[ "$hash" =~ ^[0-9a-f]{64}$ ]] || fallo "El archivo $sha no tiene un SHA-256 válido."

# --- 5. Verificar que el firmado sale del tag ---
paso "5. Verificar que el APK firmado sale de $tag"
if hecho verificado; then
    echo "Ya hecho, se omite."
else
    "$scripts/verificar-firmado.sh" "$apk" "$tag"
    marcar verificado
fi

if [ "$solo_local" -eq 1 ]; then
    echo
    echo "Ensayo local terminado. APK verificado: $apk"
    echo "SHA-256: $hash"
    echo "El tag local $tag sigue ahí; el script no lo borra. Si fue un ensayo: git tag -d $tag"
    exit 0
fi

# --- 6. Subir código y tag (irreversible) ---
paso "6. Subir main y $tag a origin"
if hecho subido; then
    echo "Ya hecho, se omite."
else
    echo "Versión:  $version (versionCode $codigo)"
    echo "Commit:   $commit"
    echo "APK:      $apk"
    echo "SHA-256:  $hash"
    echo "Remoto:   $(git remote get-url origin)"
    confirmar "¿Subir main y $tag a origin? Un tag publicado no se mueve."
    git push origin main "$tag"
    marcar subido
fi

# --- 7. Notas y publicación (irreversible) ---
paso "7. Publicar la release en GitHub"
if hecho publicado; then
    echo "Ya hecho, se omite."
else
    notas="$temporal/notas.md"
    {
        cat "$cambios_es"
        echo
        cat "$cambios_en"
        echo
        echo "SHA-256: $hash"
    } > "$notas"
    echo "Notas de la release:"
    echo "-----"
    cat "$notas"
    echo "-----"
    confirmar "¿Crear la release $tag con el APK y su .sha256?"
    gh release create "$tag" "$apk" "$sha" --title "Tu guiador $version" --notes-file "$notas" --verify-tag
    marcar publicado
fi

# --- 8. Comprobar lo publicado ---
paso "8. Comprobar que el APK publicado es el verificado"
gh release download "$tag" --pattern '*.apk' --dir "$temporal/descarga"
publicado="$(sha256sum "$temporal/descarga/tuguiador-$version.apk" | cut -d' ' -f1)"
if [ "$publicado" != "$hash" ]; then
    fallo "NO COINCIDE: el APK publicado tiene SHA-256 $publicado y el verificado $hash. Revisa la release: gh release view $tag"
fi
marcar comprobado

echo
echo "Release $tag publicada y comprobada."
echo "APK:     $apk"
echo "SHA-256: $hash"
echo "URL:     $(gh release view "$tag" --json url --jq .url)"
echo "Instalar en el móvil: adb install -r $apk"
