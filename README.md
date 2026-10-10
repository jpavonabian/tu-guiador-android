# Tu guiador (Android)

App Android nativa (Kotlin + Jetpack Compose) para guardar recorridos, pensada para personas ciegas que usan TalkBack. Es la migración de la app de iOS. El plan está en `plan.md`.

## Requisitos

- Windows con Android Studio instalado. Trae su propio JDK y el SDK.
- Un móvil Android 10 o superior con cable USB.

## Primera vez

1. Abre el proyecto: Android Studio > Open > carpeta del repo. Espera a que termine el "Gradle sync".
2. Si pide instalar `Android SDK Platform 37` u otros componentes, acepta. El SDK suele quedar en `%LOCALAPPDATA%\Android\Sdk`.
3. El wrapper de Gradle (`gradlew.bat` y `gradle\wrapper\`) ya está en el repo. Si falta, instala Gradle una vez con `winget install Gradle.Gradle` y ejecuta `gradle wrapper` en la raíz. Después ya no hace falta tenerlo instalado. `JAVA_HOME` debe apuntar al `jbr` de Android Studio, como hacen los `.bat`.
4. En el móvil: Ajustes > Acerca del teléfono > pulsa 7 veces "Número de compilación". Luego Ajustes > Sistema > Opciones de desarrollador > activa Depuración USB. Conecta el cable, acepta la huella RSA y elige "Transferencia de archivos" si lo pregunta.

## Uso diario

Hay dos `.bat` en la raíz. Doble clic, o desde una terminal en la raíz del repo:

- `construir.bat` ejecuta `assembleDebug testDebugUnitTest lint`. No necesita el móvil. El APK queda en `app\build\outputs\apk\debug\app-debug.apk`.
- `instalar-telefono.bat` compila, instala en el móvil conectado y abre la app.

Los dos esperan una tecla al terminar para que puedas leer el resultado. Si `JAVA_HOME` o `ANDROID_HOME` no están definidos, usan el JDK de Android Studio (`C:\Program Files\Android\Android Studio\jbr`) y `%LOCALAPPDATA%\Android\Sdk`. Si los tienes en otra ruta, define esas variables de entorno.

## Probar en el móvil

Tras instalar, comprueba que la app se llama "Tu guiador", que se abre sin cerrarse, que aguanta girar el móvil y que cambia de fondo con el modo oscuro del sistema.

La prueba de accesibilidad es manual. Activa TalkBack (Ajustes > Accesibilidad > TalkBack) y sigue la checklist de `plan.md`.

## Problemas frecuentes

- `adb devices` no lista el móvil, o sale `unauthorized`: revisa que el cable sea de datos y que la depuración USB esté activa, y acepta el aviso en el móvil.
- `SDK location not found`: define `ANDROID_HOME` o crea `local.properties` en la raíz con `sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk`. Ese archivo no se sube al repo.
- `Unsupported class file major version`: el JDK debe ser el 17 o uno posterior. Los `.bat` usan el de Android Studio.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: la versión instalada está firmada con otra clave. Desinstala la app del móvil y vuelve a instalar.
- Licencias del SDK sin aceptar: Studio > Settings > Languages & Frameworks > Android SDK, o `sdkmanager --licenses`.

## Build reproducible

El APK de release se puede reproducir byte a byte a partir del código fuente. La firma se hace aparte, fuera del repo, y no entra en la comprobación.

El entorno está fijado en el `Dockerfile`: imagen `eclipse-temurin` 21.0.9+10 por digest, herramientas de línea de comandos del SDK 23.0 con hash, `platforms;android-37.0` y `build-tools;36.0.0`. Gradle 9.8.0 se descarga con `distributionSha256Sum` y las dependencias se verifican con `gradle/verification-metadata.xml`. El APK no lleva el bloque de dependencias ni la información de control de versiones.

Necesitas Linux o WSL con Docker. Para `verificar-firmado.sh` hace falta además `apksigcopier` en tu máquina (`pip install apksigcopier`), y `diffoscope` si quieres el informe cuando haya diferencias (en Debian y Ubuntu: `sudo apt install diffoscope`).

Solo se compila lo que está en git. El código sale de una referencia (commit o tag) con `git archive`, no del árbol de trabajo, así que los cambios sin commit no entran. Cada compilación guarda el hash compilado en `commit.txt`.

### Los scripts

Los scripts de `scripts/` lanzan el contenedor ellos solos, no hay que arrancar nada antes. Todos hacen `docker build -t tu-guiador-build .` (la primera vez construye la imagen y después Docker usa la caché mientras no cambie el `Dockerfile`) y `docker run --rm`, así que el contenedor desaparece al terminar.

- `compilar.sh [referencia]` compila el release sin firmar en un contenedor y deja `app-release-unsigned.apk` y `commit.txt` en `build-reproducible/release/`.
- `crear-clave.sh <ruta> [alias]` crea un keystore con el `keytool` del contenedor. Es interactivo: pide contraseñas y datos del certificado. Se niega a escribir dentro del repo o a sobrescribir un archivo.
- `firmar.sh <keystore> <alias>` firma el APK sin firmar con el `apksigner` del contenedor, con el keystore montado en solo lectura, y verifica la firma. Deja `app-release.apk`.
- `verificar-reproducible.sh [referencia]` compila dos veces en dos contenedores con entornos distintos (ruta de trabajo, zona horaria, umask y hostname) y compara los APK en tu máquina. Si difieren, guarda el informe de diffoscope en `build-reproducible/informe-diffoscope.txt`.
- `verificar-firmado.sh <apk> [referencia]` compila de cero en un contenedor y compara el resultado con tu APK firmado usando `apksigcopier compare`. Si no coincide, guarda el informe en `build-reproducible/firmado/informe-diffoscope.txt`.
- `compilar-release.sh` es el script interno que corre dentro del contenedor. Lo usan `compilar.sh` y los dos de verificación. No se ejecuta a mano: sin argumentos solo muestra cómo se usa.

La carpeta `build-reproducible/` es salida generada y está en `.gitignore`. Cada script de verificación solo borra su propia salida al empezar: `verificar-reproducible.sh` las carpetas `1/` y `2/` y su informe, y `verificar-firmado.sh` la carpeta `firmado/`. `build-reproducible/release/` (el APK sin firmar y el firmado) lo reescriben solo `compilar.sh` y `firmar.sh`. Aun así, copia fuera el APK firmado que quieras conservar (paso 6 de la release).

Si cambias una dependencia o un plugin, ejecuta `scripts/regenerar-verificacion.sh`. Regenera `gradle/verification-metadata.xml` dentro del contenedor y no en Windows, para que incluya también los artefactos de Linux, como `aapt2`, y conserva las entradas que ya había. Revisa el diff antes de commitear.

## Sacar una release firmada y verificable

La clave de firma nunca entra en el repo ni en la imagen. `build.gradle.kts` no tiene `signingConfigs`: se compila sin firmar y se firma después. Compila siempre desde un tag.

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts` y añade `fastlane/metadata/android/{es-ES,en-US}/changelogs/<versionCode>.txt`. Haz commit.
2. Crea el tag solo en local, sin subirlo todavía: `git tag -a v1.1 -m "v1.1"`. Mientras no esté publicado puedes borrarlo y recrearlo (`git tag -d v1.1`) si hay que corregir algo. Los scripts leen el tag de tu repositorio local.
3. Comprueba que el build es reproducible con `scripts/verificar-reproducible.sh v1.1`. Si los APK difieren, no sigas.
4. Crea la clave, solo la primera vez: `scripts/crear-clave.sh ~/claves/tuguiador.jks`. Es RSA 4096 con validez de 10000 días, y tiene que quedar fuera del repo. Haz copia de seguridad: sin ella no podrás actualizar la app ya instalada, ni en el móvil ni en F-Droid. Si F-Droid firma con su propia clave, esta solo vale para tus APK.
5. Compila el APK sin firmar y fírmalo, en dos comandos seguidos:
   `scripts/compilar.sh v1.1`
   `scripts/firmar.sh ~/claves/tuguiador.jks tuguiador`
   - `compilar.sh` recibe la referencia de git que quieres compilar, aquí el tag `v1.1`. Extrae ese código con `git archive`, lo compila en el contenedor y deja el APK sin firmar en `build-reproducible/release/app-release-unsigned.apk`, junto a `commit.txt` con el hash compilado.
   - `firmar.sh` recibe primero la ruta del keystore que creaste en el paso 4 (`~/claves/tuguiador.jks`) y después el alias de la clave dentro de ese keystore (`tuguiador`, el que usó `crear-clave.sh`). Por defecto firma el APK que dejó `compilar.sh`.
   - `firmar.sh` pide la contraseña del keystore sin mostrarla. Para no interactivo, tómala de la variable de entorno `KS_PASS`. El keystore se monta en el contenedor en solo lectura y la contraseña no aparece en la línea de comandos.
   - Usa los esquemas de firma v2 y v3, que bastan con `minSdk` 29. No hace `zipalign` porque el APK de Gradle ya sale alineado, y alinearlo otra vez rompería la comprobación con `apksigcopier`.
   - El resultado es `build-reproducible/release/app-release.apk`. Al terminar, `apksigner` muestra el certificado con el que quedó firmado.
6. Copia el APK firmado fuera de `build-reproducible/`:
   `cp build-reproducible/release/app-release.apk ~/release/tuguiador-1.1.apk`
7. Verifica que sale de ese tag: `scripts/verificar-firmado.sh ~/release/tuguiador-1.1.apk v1.1`. Debe acabar con `VERIFICADO`.
8. Solo ahora, con todo verificado, sube el código y el tag: `git push origin main v1.1`. Un tag publicado no se mueve.
9. Publica el APK en una release asociada al tag, en GitHub, en GitLab o en ambos. Antes, calcula el hash junto al APK: `cd ~/release && sha256sum tuguiador-1.1.apk > tuguiador-1.1.apk.sha256`. Es el mismo archivo y el mismo hash en los dos sitios: no vuelvas a compilar ni a firmar para cada uno.

   **GitHub.** La release aloja el APK directamente.
   1. Inicia sesión una vez con la CLI: `gh auth login`. El token lo guarda `gh`, fuera del repo.
   2. Crea la release y sube el APK y su hash a la vez. Pega el hash en las notas para poder comprobarlo sin descargar nada:
      `gh release create v1.1 ~/release/tuguiador-1.1.apk ~/release/tuguiador-1.1.apk.sha256 --title "Tu guiador 1.1" --notes "Notas de la versión. SHA-256: <hash>"`
   3. Comprueba que el APK publicado es el verificado en el paso 7: `gh release download v1.1 --pattern '*.apk' --dir /tmp/comprobar && sha256sum /tmp/comprobar/tuguiador-1.1.apk`.

   Si ya existe una release para ese tag, `gh release create` falla. Mira qué contiene con `gh release view v1.1` antes de tocarla.

   **GitLab.** GitLab no aloja el APK dentro de la release: la release solo enlaza a un archivo, así que primero hay que subirlo a algún sitio con URL estable. Lo más sencillo es el registro de paquetes genéricos del propio proyecto:
   1. Crea un token de acceso personal en GitLab (Preferencias > Tokens de acceso) con el permiso `api`, y guárdalo en una variable de entorno de esa sesión, sin escribirlo en la línea de comandos ni en ningún archivo del repo: `read -r -s GITLAB_TOKEN && export GITLAB_TOKEN`.
   2. Sube el APK y el hash. Sustituye `ID` por el ID numérico del proyecto, que aparece en la página principal del proyecto en GitLab:
      `curl --header "PRIVATE-TOKEN: $GITLAB_TOKEN" --upload-file ~/release/tuguiador-1.1.apk "https://gitlab.com/api/v4/projects/ID/packages/generic/tuguiador/1.1/tuguiador-1.1.apk"`
      `curl --header "PRIVATE-TOKEN: $GITLAB_TOKEN" --upload-file ~/release/tuguiador-1.1.apk.sha256 "https://gitlab.com/api/v4/projects/ID/packages/generic/tuguiador/1.1/tuguiador-1.1.apk.sha256"`
   3. En GitLab, Deploy > Releases > New release: elige el tag `v1.1`, escribe las notas y, en "Release assets", añade dos enlaces con las URLs de arriba (el APK y su `.sha256`). Pega también el hash en las notas.
   4. Revoca el token cuando termines, o pon una fecha de caducidad corta.

   Si instalas la CLI `glab`, `glab release create v1.1 ~/release/tuguiador-1.1.apk` hace los pasos de subida y de la release de una vez. Comprueba antes `glab release create --help`, porque el comportamiento con archivos puede variar entre versiones.

   Si ya subiste un archivo con ese nombre y versión, GitLab puede rechazar o duplicar la subida. Comprueba en Deploy > Package registry que el APK publicado es el verificado en el paso 7.

Para instalar el APK firmado en el móvil: `adb install -r ~/release/tuguiador-1.1.apk`. Si había una versión de depuración, desinstálala antes, porque la firma es distinta.

Cualquiera con Docker y `apksigcopier` puede repetir el paso 7 con el tag y tu APK, y comprobar que corresponde al código fuente. Eso solo funciona mientras el entorno del `Dockerfile` (imagen por digest, SDK con hash) siga disponible.

## Reglas del proyecto

- Sin servicios de Google (Play Services, Firebase, ML Kit), sin analítica, publicidad ni informes de errores, y sin permiso `INTERNET`. Se publicará en F-Droid.
- Copia de seguridad en la nube desactivada. Los datos se exportan a mano.
- Más detalle en `CLAUDE.md` y `plan.md`.
