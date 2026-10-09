   # Tu guiador (Android)

App Android nativa (Kotlin + Jetpack Compose) para guardar recorridos, pensada para personas ciegas que usan TalkBack. Migración de la app iOS. Ver `plan.md`.

## Requisitos

- Windows con **Android Studio** instalado (trae su propio JDK y el SDK).
- Un móvil Android 10 o superior con cable USB.

## Primera vez

1. **Abrir el proyecto:** Android Studio > Open > carpeta del repo. Espera a que termine "Gradle sync".
2. **SDK:** si pide instalar `Android SDK Platform 36` u otros componentes, acepta. El SDK suele quedar en `%LOCALAPPDATA%\Android\Sdk`.
3. **Wrapper:** comprueba que existen `gradlew.bat` y `gradle\wrapper\` en la raíz. Si no:
   - Instala Gradle una vez (`winget install Gradle.Gradle`), abre una terminal nueva en la raíz del repo y ejecuta `gradle wrapper`. Después ya no hace falta Gradle instalado.
   - Sus ajustes de JDK: con `JAVA_HOME` apuntando al `jbr` de Android Studio (los `.bat` ya lo hacen).
4. **Si falla el sync:** las versiones de `gradle/libs.versions.toml` (AGP, Kotlin, Compose BOM) y `compileSdk`/`targetSdk` se escribieron sin poder comprobarlas. Studio suele sugerir la versión correcta; ajústalas y avisa.
5. **Móvil:** Ajustes > Acerca del teléfono > pulsa 7 veces "Número de compilación". Luego Ajustes > Sistema > Opciones de desarrollador > activa **Depuración USB**. Conecta el cable, acepta la huella RSA en el móvil y elige "Transferencia de archivos" si lo pregunta.

## Uso diario: los dos `.bat`

Doble clic, o desde la raíz del repo:

| Archivo | Qué hace |
|---|---|
| `construir.bat` | `assembleDebug testDebugUnitTest lint`. No necesita el móvil. APK en `app\build\outputs\apk\debug\app-debug.apk`. |
| `instalar-telefono.bat` | Compila, instala en el móvil conectado y abre la app. |

Ambos esperan a una tecla al terminar para que veas el resultado. Si `JAVA_HOME` o `ANDROID_HOME` no están definidos, usan el JDK de Android Studio (`C:\Program Files\Android\Android Studio\jbr`) y `%LOCALAPPDATA%\Android\Sdk`. Si los instalaste en otra ruta, define esas variables de entorno.

## Cómo probar en el móvil

Estado actual (fase 0): la app arranca con una pantalla en blanco. Comprueba:

1. `instalar-telefono.bat` termina con "Instalada y abierta".
2. La app se llama "Tu guiador" y se abre sin cerrarse.
3. Gira el móvil: no debe fallar.
4. Activa el modo oscuro del sistema y reabre: el fondo debe cambiar.

Con cada fase nueva, la prueba de TalkBack es manual: activa TalkBack (Ajustes > Accesibilidad > TalkBack) y sigue la checklist de `plan.md`.

## Problemas frecuentes

- `adb devices` no lista el móvil, o sale `unauthorized`: revisa el cable (que sea de datos), la depuración USB y acepta el aviso en el móvil.
- `SDK location not found`: define `ANDROID_HOME` o crea `local.properties` en la raíz con `sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk` (no se sube al repo).
- Error de Java / `Unsupported class file major version`: el JDK debe ser 17 o superior. Los `.bat` usan el de Android Studio.
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: hay una versión firmada con otra clave. Desinstala la app del móvil y reinstala.
- Licencias del SDK sin aceptar: Studio > Settings > Languages & Frameworks > Android SDK, o `sdkmanager --licenses`.

## Build reproducible

El APK de release se puede reproducir byte a byte a partir del código fuente. La firma se hace fuera del repositorio y no forma parte de esta comprobación.

Entorno fijado en `Dockerfile`: imagen `eclipse-temurin` 21.0.9+10 por digest, herramientas de línea de comandos del SDK 23.0 con hash, `platforms;android-37.0` y `build-tools;36.0.0`. Gradle 9.8.0 se descarga con `distributionSha256Sum`, y las dependencias se verifican con `gradle/verification-metadata.xml`. El APK no incluye el bloque de dependencias ni la información de control de versiones.

Requisitos: Linux o WSL con Docker. Para el segundo script, `apksigcopier` (`pip install apksigcopier`). Si hay diferencias hace falta `diffoscope` instalado (en Debian/Ubuntu: `sudo apt install diffoscope`).

Solo se compila lo versionado: el código sale de una referencia de git (commit o tag) con `git archive`, no del árbol de trabajo. Los cambios sin commit no se compilan. Cada compilación escribe también `commit.txt` con el hash compilado.

1. Comprobar que dos compilaciones limpias dan el mismo APK:
   `scripts/verificar-reproducible.sh [referencia_git]` (por defecto `HEAD`)
   Compila dos veces en contenedores nuevos y con entornos distintos (ruta de trabajo, zona horaria, umask, hostname y fecha adelantada 30 días en la segunda), y compara los APK. Si difieren, guarda el informe de diffoscope en texto en `build-reproducible/informe-diffoscope.txt`.
2. Comprobar un APK firmado por ti contra una compilación desde cero:
   `scripts/verificar-firmado.sh ruta/al/app-release.apk [referencia_git]`
   Usa `apksigcopier compare`. Si no coincide, guarda el informe en `build-reproducible/firmado/informe-diffoscope.txt`.

La carpeta `build-reproducible/` es salida generada y está en `.gitignore`.

Al cambiar una dependencia o un plugin, regenera `gradle/verification-metadata.xml` dentro del contenedor (no en Windows), para incluir también los artefactos de Linux, como `aapt2`.

## Compilar y firmar una release

Todo se hace en el contenedor del `Dockerfile`, a partir de una referencia de git (por defecto `HEAD`). Requiere Linux o WSL con Docker. La clave de firma nunca entra en el repo ni en la imagen: `build.gradle.kts` no tiene `signingConfigs`, se firma después sobre el APK sin firmar.

1. **Compilar sin firmar:** `scripts/compilar.sh [referencia_git]`
   Deja `build-reproducible/release/app-release-unsigned.apk` y `commit.txt`. Para publicar, usa un tag (por ejemplo `scripts/compilar.sh v1.0`) y comprueba antes con `scripts/verificar-reproducible.sh v1.0` que dos compilaciones coinciden.
2. **Crear la clave (solo la primera vez):** `scripts/crear-clave.sh ~/claves/tuguiador.jks [alias]`
   Hecho con el `keytool` del contenedor: RSA 4096, validez de 10000 días. Pide contraseña y datos del certificado. Debe quedar fuera del repo (el script se niega si la ruta está dentro). **Haz copia de seguridad**: si la pierdes no puedes actualizar la app instalada ni en F-Droid ni en el móvil. Si F-Droid firma con su propia clave, esta es solo para tus APK propios.
3. **Firmar:** `scripts/firmar.sh ~/claves/tuguiador.jks tuguiador`
   Pide la contraseña sin mostrarla (o usa la variable `KS_PASS`). Usa `apksigner` de `build-tools;36.0.0`, esquemas v2 y v3 (suficiente con `minSdk` 29), sin `zipalign` adicional porque el APK de Gradle ya sale alineado, y verifica la firma al terminar. Resultado: `build-reproducible/release/app-release.apk`.
4. **Comprobar que el firmado corresponde al código:** `scripts/verificar-firmado.sh build-reproducible/release/app-release.apk v1.0`

Notas:

- `verificar-reproducible.sh` y `verificar-firmado.sh` borran `build-reproducible/` (o `build-reproducible/firmado/`) al empezar. Mueve fuera el APK firmado que quieras conservar.
- Antes de una release sube `versionCode` y `versionName` en `app/build.gradle.kts` y añade `fastlane/metadata/android/*/changelogs/<versionCode>.txt`.
- Instalar el APK firmado en el móvil: `adb install -r build-reproducible/release/app-release.apk`. Si había una versión de depuración, desinstálala antes (firma distinta).
- Los scripts se escribieron sin poder ejecutarlos (sin Docker en la máquina de desarrollo): primera ejecución, revisa la salida.

## Reglas del proyecto

- Sin servicios de Google (Play Services, Firebase, ML Kit), sin analítica, publicidad ni informes de errores, y sin permiso `INTERNET`. Se publicará en F-Droid.
- Copia de seguridad en la nube desactivada; exportación manual de datos.
- Más detalle en `CLAUDE.md` y `plan.md`.
