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

La carpeta `build-reproducible/` es salida generada y está en `.gitignore`. Los dos scripts de verificación la borran al empezar (`verificar-firmado.sh`, solo `build-reproducible/firmado/`), así que mueve fuera el APK firmado que quieras conservar.

Si cambias una dependencia o un plugin, regenera `gradle/verification-metadata.xml` dentro del contenedor y no en Windows, para que incluya también los artefactos de Linux, como `aapt2`.

## Sacar una release firmada y verificable

La clave de firma nunca entra en el repo ni en la imagen. `build.gradle.kts` no tiene `signingConfigs`: se compila sin firmar y se firma después. Compila siempre desde un tag.

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts` y añade `fastlane/metadata/android/{es-ES,en-US}/changelogs/<versionCode>.txt`. Haz commit.
2. Crea el tag y súbelo. Un tag publicado no se mueve.
   `git tag -a v1.1 -m "v1.1" && git push origin main v1.1`
3. Comprueba que el build es reproducible con `scripts/verificar-reproducible.sh v1.1`. Si los APK difieren, no sigas.
4. Crea la clave, solo la primera vez: `scripts/crear-clave.sh ~/claves/tuguiador.jks`. Es RSA 4096 con validez de 10000 días, y tiene que quedar fuera del repo. Haz copia de seguridad: sin ella no podrás actualizar la app ya instalada, ni en el móvil ni en F-Droid. Si F-Droid firma con su propia clave, esta solo vale para tus APK.
5. Compila y firma:
   `scripts/compilar.sh v1.1`
   `scripts/firmar.sh ~/claves/tuguiador.jks tuguiador`
   `firmar.sh` pide la contraseña sin mostrarla (o la toma de la variable `KS_PASS`). Usa los esquemas v2 y v3, que bastan con `minSdk` 29. No hace `zipalign` porque el APK de Gradle ya sale alineado, y alinearlo otra vez rompería la comprobación con `apksigcopier`. El resultado es `build-reproducible/release/app-release.apk`.
6. Copia el APK firmado fuera de `build-reproducible/`:
   `cp build-reproducible/release/app-release.apk ~/release/tuguiador-1.1.apk`
7. Verifica que sale de ese tag: `scripts/verificar-firmado.sh ~/release/tuguiador-1.1.apk v1.1`. Debe acabar con `VERIFICADO`.
8. Publica el APK en la release del tag, con su `sha256sum`.

Para instalar el APK firmado en el móvil: `adb install -r ~/release/tuguiador-1.1.apk`. Si había una versión de depuración, desinstálala antes, porque la firma es distinta.

Cualquiera con Docker y `apksigcopier` puede repetir el paso 7 con el tag y tu APK, y comprobar que corresponde al código fuente. Eso solo funciona mientras el entorno del `Dockerfile` (imagen por digest, SDK con hash) siga disponible.

## Reglas del proyecto

- Sin servicios de Google (Play Services, Firebase, ML Kit), sin analítica, publicidad ni informes de errores, y sin permiso `INTERNET`. Se publicará en F-Droid.
- Copia de seguridad en la nube desactivada. Los datos se exportan a mano.
- Más detalle en `CLAUDE.md` y `plan.md`.
