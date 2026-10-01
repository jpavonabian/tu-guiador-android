# CLAUDE.md

Reglas para Claude en este repositorio. Son obligatorias. Si una petición entra en conflicto con ellas, **para y pregunta** antes de actuar.

## Contexto

Tu guiador es una app Android nativa (Kotlin + Jetpack Compose) dirigida a **personas ciegas que usan TalkBack**. Guarda recorridos (nombre, descripción, pasos) en un archivo `recorridos.json`. Es la migración de la app iOS (SwiftUI, VoiceOver). Es un proyecto pequeño y deliberadamente simple: mantenlo así.

- El plan de migración está en `plan.md`. Síguelo; si algo no está decidido allí, pregunta.
- Android Studio reciente, Gradle con Kotlin DSL, Kotlin, Jetpack Compose, Material 3.
- Interfaz en español (base); traducción al inglés en `values-en`.
- El repo iOS de origen está en `C:\Users\Usuario\Desktop\repos\Tu-guiador`. **Solo lectura**: nunca lo modifiques, ni siquiera para formatear o corregir.

## Alcance: haz solo lo que se pide

- Cambia únicamente lo necesario para la tarea. **Nada de refactors, renombrados, reformateos, "mejoras" ni limpieza** que no se hayan pedido explícitamente.
- No crees archivos nuevos salvo que la tarea lo requiera. No añadas documentación, tests ni ejemplos no solicitados.
- No cambies la arquitectura: `Controlador` como `ViewModel`, modelo `Recorrido`, vistas en `vistas/`. No introduzcas patrones nuevos (Clean Architecture, repositorios, casos de uso, MVI, inyección de dependencias, etc.).
- Si ves un problema fuera del alcance, **menciónalo**, no lo arregles.

## Accesibilidad: requisito principal

Todo cambio de interfaz debe seguir siendo completamente usable con TalkBack. Un cambio que degrade TalkBack es un bug, aunque se vea bien.

- Todo elemento interactivo necesita nombre accesible con sentido. Los botones solo con icono llevan `contentDescription`; los decorativos, `null`.
- Cualquier acción disponible por toque largo o menú debe estar también como acción accesible (`semantics { customActions }`) en el mismo elemento. Ver la lista de recorridos.
- No elimines ni cambies `customActions`, etiquetas, `heading()` o el orden de lectura existentes sin que se pida.
- Cada elemento de lista es un único nodo accesible (`mergeDescendants`).
- Áreas táctiles de al menos 48 dp. Contraste de texto 4.5:1.
- No uses información transmitida solo por color, posición o animación.
- No añadas gestos personalizados, temporizadores, snackbars que desaparecen solos ni elementos efímeros.
- Respeta el tamaño de fuente del sistema: `sp` y tipografía del tema, nada de tamaños fijos en dp ni alturas fijas que recorten texto.
- Campos de texto con `label`, no solo `placeholder`.
- Gestiona el foco: tras abrir/cerrar diálogos y pantallas, o añadir/quitar pasos, el foco debe quedar en un elemento lógico.
- Al terminar un cambio de UI, indica explícitamente qué debe comprobar el usuario con TalkBack activado (usa la checklist de `plan.md`).

## Datos: no romper compatibilidad

`recorridos.json` contiene datos reales de usuarios, y los recorridos se comparten entre personas, y entre iOS y Android, como JSON. Un cambio incompatible borra o corrompe datos.

- **No cambies** las propiedades de `Recorrido` (`id`, `nombre`, `descripcion`, `pasos`), sus nombres, tipos ni su serialización, salvo petición explícita.
- Si se pide añadir un campo, debe ser opcional o tener valor por defecto, para que los JSON antiguos y los de iOS sigan cargando.
- No cambies el nombre del archivo (`recorridos.json`) ni su formato (array de objetos).
- No cambies la igualdad/`hashCode` de `Recorrido` (solo `nombre` + `descripcion`) ni `asignarIdentificadoresUnicos()` sin petición explícita.
- Nunca sobrescribas un `recorridos.json` que no se haya podido leer. Guarda de forma atómica (temporal + renombrar).
- Cualquier cambio de formato debe probarse con un JSON generado por la app iOS.

## Textos e idioma

- Los textos visibles van en `res/values/strings.xml` (español, base); no los escribas literales en el código Compose.
- **Cada texto visible nuevo o modificado debe añadirse también a `res/values-en/strings.xml`.** En inglés se usa "pathway" para "recorrido".
- Descripciones accesibles (`contentDescription`, etiquetas de acciones) también son texto visible: mismas reglas.
- Identificadores, comentarios y nombres en español, como el código de origen.

## Archivos y valores prohibidos

No modifiques estos archivos o valores salvo petición explícita y específica:

- `applicationId`, `namespace`, `versionCode`, `versionName`, `minSdk`, `targetSdk` y `compileSdk` en `app/build.gradle.kts`.
- Configuración de firma (`signingConfigs`), `gradle.properties` con credenciales, `local.properties`.
- `AndroidManifest.xml`: permisos, `exported`, reglas de backup.
- `.gitignore`, `plan.md` y este `CLAUDE.md`.

Nunca añadas dependencias ni plugins fuera de la lista permitida en `plan.md` (Compose, Navigation, ViewModel, kotlinx.serialization y librerías de test básicas). Nada de red, analítica, publicidad, Firebase, ni permisos nuevos (la app no necesita `INTERNET`).

**F-Droid:** la app se publicará en F-Droid. Se permite AndroidX y Kotlin. Prohibido: Play Services, Firebase, ML Kit y cualquier SDK de analítica, publicidad o informes de errores (incluidos los que entren de forma transitiva). Sin permiso `INTERNET` salvo que se justifique. Copia de seguridad en la nube desactivada; exportación manual de datos en su lugar. Ante la duda sobre una dependencia, para y pregunta.

## Verificación honesta

- Si no has podido compilar ni ejecutar (no hay SDK de Android, emulador o dispositivo), dilo claramente. **No afirmes que algo funciona sin haberlo comprobado.**
- Con el SDK disponible, antes de dar una tarea por terminada:
  `./gradlew assembleDebug testDebugUnitTest lint`
- TalkBack no se puede verificar desde aquí: nunca digas que la accesibilidad "está comprobada"; di qué debe probar el usuario.
- No desactives, borres ni debilites tests ni reglas de lint para que pasen.

## Git

- Trabaja siempre en una rama. **Nunca hagas commit ni push directamente a `main`.**
- Nunca uses `push --force`, `reset --hard`, `rebase` sobre ramas compartidas ni reescribas historial.
- No hagas commit ni push sin que el usuario lo pida.
- Commits pequeños y centrados en un solo cambio.
- Antes de hacer commit, revisa el diff: no debe incluir `build/`, `.gradle/`, `.idea/`, `local.properties` ni cambios de firma o de `applicationId`.
- Nunca incluyas en el repo secretos, keystores (`.jks`, `.keystore`), contraseñas de firma, ni datos personales (ni JSON de recorridos reales en fixtures de test).
