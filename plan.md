# Plan de migración: Tu guiador iOS → Android

Origen (solo lectura): el repo `Tu-guiador`, clonado junto a este (`../Tu-guiador`) (SwiftUI, iOS 16+).
Destino: este repo. Android nativo, Kotlin, Jetpack Compose.

## Objetivo

Paridad funcional con la app iOS, para **personas ciegas que usan TalkBack** (equivalente de VoiceOver). La accesibilidad es el requisito principal, no un añadido final. Proyecto pequeño y deliberadamente simple: mantenerlo así.

## Qué hace la app iOS (inventario)

Son 7 archivos Swift, unas 400 líneas. Toda la funcionalidad:

| Función | iOS | Archivo origen |
|---|---|---|
| Listar recorridos (por nombre) | `List` + `NavigationLink` | `ContentView.swift` |
| Estado vacío con "Añadir" e "Importar" | `VStack` | `ContentView.swift` |
| Ver detalle (nombre, descripción, pasos) | `RecorridoDetalles` | `Vistas/RecorridoDetalles.swift` |
| Crear recorrido (nombre, descripción, pasos dinámicos) | `NuevoRecorrido` | `Vistas/NuevoRecorrido.swift` |
| Editar recorrido (recorta espacios, descarta pasos vacíos) | `EditarRecorridoView` | `Vistas/EditarRecorrido.swift` |
| Eliminar con confirmación | `Alert` | `ContentView.swift` |
| Compartir recorrido como JSON (texto) | `ShareLink` | `ContentView.swift`, `RecorridoDetalles.swift` |
| Importar JSON desde portapapeles (éxito/error) | `UIPasteboard` + `Alert` | `Vistas/Importar.swift` |
| Persistencia | `Documents/recorridos.json` en iCloud Drive | `Controlador.swift` |
| Localización es (base) + en | `en.lproj/Localizable.strings` | — |
| Acciones VoiceOver Editar/Eliminar en cada fila | `.accessibilityAction(named:)` | `ContentView.swift` |

## Mapa de equivalencias

| iOS | Android |
|---|---|
| SwiftUI | Jetpack Compose + Material 3 |
| `Controlador: ObservableObject` | `Controlador : ViewModel` con `StateFlow<List<Recorrido>>` |
| `struct Recorrido: Codable` | `@Serializable data class Recorrido` (kotlinx.serialization) |
| `NavigationView` / `NavigationLink` | Navigation Compose |
| `.sheet` (crear/editar) | Pantallas de navegación a pantalla completa (ver decisiones) |
| `Alert` | `AlertDialog` |
| `.contextMenu` | `combinedClickable(onLongClick, onLongClickLabel)` + menú |
| `.accessibilityAction(named:)` | `semantics { customActions = ... }` |
| `ShareLink` | `Intent.ACTION_SEND` (`text/plain`) con `createChooser` |
| `UIPasteboard.general.string` | `ClipboardManager` |
| `Localizable.strings` (es base, en) | `res/values/strings.xml` (es) + `res/values-en/strings.xml` |
| Persistencia en iCloud Drive | No se migra. Archivo local en `filesDir` (decisión A) |
| Dynamic Type | Tamaños en `sp` / `MaterialTheme.typography`, sin tamaños fijos |
| VoiceOver | TalkBack |

## Decisiones (todas cerradas)

**A. Almacenamiento: decidido, local.** `recorridos.json` se guarda en el dispositivo (`filesDir`), mismo formato que iOS. Nada de nube ni copia de seguridad automática (desactivada en el manifiesto). En su lugar, **exportación e importación manual** de todos los recorridos como archivo `recorridos.json` (fase 4), mediante el selector de archivos del sistema (Storage Access Framework, sin permisos). `Almacen` queda como único punto de lectura/escritura para poder cambiarlo luego.

**B. `applicationId`: decidido.** `es.jesuspavonabian.tuguiador` (coincide con iOS).

**C. Versión mínima: decidido.** `minSdk 29` (Android 10). `targetSdk` y `compileSdk` la última estable.

**D. Botón Atrás en crear/editar: decidido.** iOS bloquea el cierre por gesto (`interactiveDismissDisabled`). En Android, Atrás equivale a "Cerrar"; si hay cambios sin guardar, diálogo de confirmación para descartar.

**F. Crear/editar: decidido.** Pantallas completas de navegación, no hojas modales.

**E. Compatibilidad iOS ⇄ Android: comprobada.** El JSON de un recorrido compartido desde iOS se importa en Android y al revés, sin conversión.

## Contrato de datos (no negociable)

Los recorridos se comparten entre personas como JSON, también entre iOS y Android.

```json
[{ "id": 0, "nombre": "…", "descripcion": "…", "pasos": ["…", "…"] }]
```

- Mismos nombres y tipos: `id` Int, `nombre` String, `descripcion` String, `pasos` lista de String.
- `Json { ignoreUnknownKeys = true; encodeDefaults = true }`. Campos futuros: opcionales o con valor por defecto.
- Igualdad e `hashCode` de `Recorrido` solo por `nombre` + `descripcion` (sobrescribir `equals`/`hashCode`, no usar el de `data class`).
- Replicar `asignarIdentificadoresUnicos()`: al guardar, `id` = índice 0..n-1.
- Replicar `importarRecorrido`: si ya existe uno con mismo nombre y descripción, no se duplica y cuenta como éxito; JSON inválido devuelve error.
- Un único recorrido se comparte como objeto (`{…}`), el archivo completo es array (`[…]`).

## Estructura

```
app/src/main/java/es/jesuspavonabian/tuguiador/
  MainActivity.kt          Punto de entrada, NavHost
  Controlador.kt           ViewModel: carga, guardado, importación
  Recorrido.kt             Modelo
  Almacen.kt               Lectura/escritura de recorridos.json (solo `filesDir`)
  ui/theme/Tema.kt         Tema Material 3 (colores dinámicos, modo oscuro)
  vistas/
    ListaRecorridos.kt     Listado + estado vacío
    RecorridoDetalles.kt
    NuevoRecorrido.kt      Incluye el formulario común de Nuevo y Editar
    EditarRecorrido.kt
    Importar.kt            Portapapeles y archivos (importar/exportar)
app/src/main/res/values/strings.xml       Español (base)
app/src/main/res/values-en/strings.xml    Inglés ("pathway")
app/src/main/res/xml/                      Reglas de copia de seguridad (todo excluido)
app/src/main/res/{drawable,mipmap-anydpi}/ Icono adaptativo
app/src/test/                              Tests unitarios (modelo, almacén y controlador)
app/src/androidTest/                       Tests de UI / accesibilidad
icono/                                     Fuente SVG del icono y cómo se hizo
```

Nombres en español, como en el origen. Nada de capas extra (sin Clean Architecture, sin repositorios, sin casos de uso).

## Dependencias permitidas

Solo estas, vía version catalog (`gradle/libs.versions.toml`):
- Compose BOM, `material3`, `activity-compose`, `navigation-compose`, `lifecycle-viewmodel-compose`
- `kotlinx-serialization-json`
- Tests: JUnit, `compose-ui-test-junit4`, `androidx.test` básicos

Sin Hilt/Koin, Room/DataStore, Retrofit, Coil, Firebase, analítica ni anuncios. El origen no usa ninguna dependencia.

**Publicación en F-Droid:** se permite AndroidX y Kotlin. Prohibido: Play Services, Firebase, ML Kit y cualquier SDK de analítica, publicidad o informes de errores (también transitivos). Sin permiso `INTERNET` salvo que se justifique. Copia de seguridad en la nube desactivada; exportación manual de datos en su lugar. No se publicará en Play Store.

## Estado

- Fases 0 a 4: hechas. `assembleDebug testDebugUnitTest lint` pasa sin avisos (23 tests unitarios). La compatibilidad de compartir recorridos entre iOS y Android está comprobada con la app real.
- Fase 5: escritos los tests de semántica (`AccesibilidadTest`, 10 tests). Falta ejecutarlos en un dispositivo (`connectedDebugAndroidTest`) y hacer la auditoría manual con TalkBack real y el Accessibility Scanner. Se está probando con un APK de depuración entre unos testers.
- Fase 6: ver abajo. Hecho: licencia, icono, metadatos de F-Droid. Falta: repo público, versión, APK de release y ficha en `fdroiddata`.
- Compilación: AGP 9.4.1, Kotlin 2.4.20, Gradle 9.8.0, `compileSdk` y `targetSdk` 37, `minSdk` 29. Con `targetSdk` 37 hay que repetir las pruebas manuales en el móvil.
- Decisiones abiertas:
  - Como en iOS, un fallo al guardar no se notifica al usuario. Pendiente de una fase de corrección de errores.
  - `versionName`: `1.0` o `0.1` hasta terminar las pruebas (hoy `1.0` / `versionCode` 1).
  - Aviso de Gradle `Configuration.setVisible` (de un plugin, no del proyecto): se resuelve al actualizar el plugin.

## Fases

### Fase 0: esqueleto
- `git init`, `.gitignore` de Android (`build/`, `.gradle/`, `local.properties`, `*.keystore`, `.idea/`), rama de trabajo.
- Proyecto Gradle (Kotlin DSL), módulo `app`, tema Material 3 con colores dinámicos y modo oscuro.
- `MainActivity` con pantalla vacía. Verificar que compila.
- Configurar `applicationId` y `minSdk` según decisiones B y C.

### Fase 1: modelo, datos y localización
- `Recorrido` con serialización y `equals`/`hashCode` por nombre + descripción.
- `Almacen` + `Controlador`: cargar, guardar, añadir, eliminar, editar, importar, `obtenerJSONRecorrido`, `asignarIdentificadoresUnicos`.
- E/S fuera del hilo principal (corrutinas con `Dispatchers.IO`); guardado atómico (escribir a temporal y renombrar) para no corromper el JSON.
- Si el archivo no existe: lista vacía. Si está corrupto: lista vacía **sin sobrescribir** el archivo hasta que el usuario cambie algo; dejar copia `recorridos.json.corrupto`.
- Tests unitarios: decodificar JSON generado por iOS (fixture copiado de un archivo real, sin datos personales), ida y vuelta, JSON sin campos extra, igualdad, importación duplicada, ids.
- `strings.xml` (es) y `values-en` con las 19 cadenas existentes en `en.lproj` (cubren todos los textos de las vistas de iOS).

### Fase 2: lista y detalle (solo lectura)
- Estado vacío: texto "No hay recorridos disponibles." / "Añade o importa un recorrido." + botones.
- Lista con cabecera como encabezado accesible (`heading()`) y texto "Mantén pulsado un recorrido para ver opciones".
- Detalle: nombre como encabezado, descripción, pasos en orden de lectura, botón compartir.
- Primera pasada manual con TalkBack (checklist abajo).

### Fase 3: crear, editar, eliminar
- Pantallas Nuevo/Editar con campos etiquetados, pasos dinámicos, añadir y quitar paso.
- Editar recorta espacios y descarta pasos vacíos, igual que iOS.
- Diálogo de confirmar eliminación.
- Gestión del foco: al añadir un paso, foco al nuevo campo; al quitar uno, foco a un elemento lógico cercano.
- Decisión D (Atrás).

### Fase 4: compartir e importar
- Compartir (exportar un recorrido): `ACTION_SEND` con el JSON como texto, igual que `ShareLink` en iOS. El selector de Android incluye "Copiar" al portapapeles, así que se puede compartir por cualquier app o copiar y pegar.
- Exportar todos los recorridos a archivo: `ACTION_CREATE_DOCUMENT` (`application/json`, nombre sugerido `recorridos.json`), mismo formato array que el archivo interno. Importar desde archivo: `ACTION_OPEN_DOCUMENT`, con la misma lógica de no duplicar que el importar del portapapeles. Error claro si el archivo no es válido; nunca sobrescribir los datos actuales con un archivo inválido. Es la sustitución de la copia de seguridad en la nube.
- Importar: leer portapapeles, diálogos de éxito/error como los de iOS. Portapapeles vacío cuenta como error.

### Fase 5: accesibilidad a fondo
- Auditoría contra la checklist de abajo con TalkBack real (dispositivo, no solo emulador).
- Accessibility Scanner de Google.
- Tests Compose de semántica.
- Probar con fuente al 200 %, tamaño de pantalla pequeño, modo oscuro, orientación horizontal y Cambio de acceso con teclado/switch.

### Fase 6: publicación (F-Droid)
F-Droid compila el código fuente público y firma el APK con su propia clave.
- Hecho: licencia GPL-3.0-or-later (`LICENSE` y cabecera SPDX en cada `.kt`); icono adaptativo propio, no derivado del PNG de iOS (ver `icono/icono.md`).
- Hecho: metadatos fastlane en `fastlane/metadata/android/es-ES` y `en-US`: título, descripción corta (80 caracteres como máximo), descripción larga, novedades (`changelogs/1.txt`, que se enlaza con el `versionCode`) e `images/icon.png` de 512 px sin transparencia. Opcional, sin hacer: capturas en `images/phoneScreenshots/`.
- Hecho: repositorio en GitLab (`gitlab.com/jpavonabian/tu-guiador-android`), con `main` subido. Falta hacerlo **público**.
- Falta: confirmar `versionCode`/`versionName` y crear una etiqueta git por versión (`v1.0`…), que es lo que F-Droid compila. Si cambia el `versionCode`, se renombra `changelogs/1.txt`.
- Comprobar el ajuste de F-Droid sobre el bloque de metadatos de dependencias (`dependenciesInfo`) del APK de release, y que la compilación sale igual desde el código fuente.
- Ficha de la app en el repositorio `fdroiddata` de F-Droid (merge request).
- Firma propia solo para los APK que se repartan a mano (el keystore **nunca** al repo). Cambiar de firma obliga a desinstalar, así que se exportan los datos antes.
- Pruebas internas cerradas con usuarios ciegos antes de producción.

## Accesibilidad con TalkBack: requisitos

Equivalentes a los de VoiceOver en la app iOS, más lo que Android exige.

- Todo elemento interactivo tiene nombre accesible. Botones solo con icono: `contentDescription`. Decorativos: `contentDescription = null`.
- **Acciones por gesto = acciones accesibles.** Cada fila de la lista expone `customActions` "Editar", "Eliminar" y "Compartir", además del toque largo. Nada solo por gesto.
- Toque largo con `onLongClickLabel` descriptivo ("Ver opciones").
- Orden de lectura lógico: cabecera → filas; nombre → descripción → pasos.
- Cada fila de lista es un solo elemento (`mergeDescendants`), no varios.
- Encabezados con `semantics { heading() }`.
- Áreas táctiles de al menos 48 dp.
- Campos de texto con etiqueta visible y accesible (`label`), no solo `placeholder`.
- Botón de quitar paso con descripción única: "Eliminar paso 3" (en iOS el botón con icono `minus.circle.fill` no lleva etiqueta en el código porque VoiceOver se la pone solo; TalkBack **no** hace eso con iconos de Compose, así que aquí la etiqueta es obligatoria).
- Nada transmitido solo por color. El rojo del botón de quitar paso debe ir acompañado de icono y texto/descripción.
- Contraste mínimo 4.5:1 en texto, 3:1 en componentes.
- Fuentes en `sp`, sin alturas fijas que recorten texto al ampliar.
- Sin temporizadores, sin elementos que desaparezcan solos, sin gestos personalizados. Si hace falta un aviso (p. ej. "Recorrido eliminado"), usar diálogo o `liveRegion`, no un snackbar que se esfuma.
- Diálogos: TalkBack los anuncia; el foco entra al diálogo y vuelve al elemento que lo abrió.
- Al cerrar una pantalla, devolver el foco al elemento lógico de la anterior.

### Checklist manual con TalkBack (al terminar cada pantalla)
1. Activar TalkBack, navegar de izquierda a derecha por toda la pantalla: ¿se lee todo y con sentido?
2. Desde una fila, abrir el menú de acciones (gesto de acciones): ¿están Editar, Eliminar y Compartir?
3. Crear un recorrido con 3 pasos usando solo TalkBack y teclado en pantalla.
4. Quitar el paso 2: ¿se anuncia cuál y dónde queda el foco?
5. Eliminar: ¿el diálogo se anuncia y el foco vuelve a la lista?
6. Importar con portapapeles válido, inválido y vacío.
7. Compartir un recorrido.
8. Girar la pantalla y ampliar la fuente: ¿se mantiene el estado y no se corta nada?

## Cadenas a traducir (origen: `en.lproj/Localizable.strings`)

Pasar las 19 entradas a `strings.xml` (es) y `values-en/strings.xml`. En inglés, "pathway" = "recorrido". Cadenas nuevas de Android (descripciones accesibles, "Compartir", "Ver opciones", confirmar descartar) también en ambos idiomas.

## Diferencias deliberadas respecto a iOS

- Almacenamiento local en el dispositivo en lugar de iCloud; copia de seguridad en la nube desactivada y exportación/importación manual de archivo.
- Compartir y portapapeles usan los mecanismos del sistema Android.
- Botón de quitar paso con etiqueta explícita (en iOS la pone VoiceOver).
- Atrás del sistema funciona (con confirmación si hay cambios).
- Acción "Compartir" disponible también como acción accesible en la fila.

## Problemas del origen detectados (no corregir en la migración sin decidirlo)

Los observo y los dejo anotados; la paridad manda salvo decisión explícita.
- `NuevoRecorrido` no recorta espacios ni descarta pasos vacíos; `EditarRecorrido` sí.
- `NuevoRecorrido` permite guardar con nombre vacío.
- `guardarRecorridos` falla en silencio (solo `print`) si la escritura falla.
- `RecorridoDetalles` declara `mostrarVistaEditarRecorrido` sin usarlo.
- `UIPasteboard` sin portapapeles de texto cuenta como error.

## Verificación

- `./gradlew assembleDebug testDebugUnitTest lint`
- `./gradlew connectedDebugAndroidTest` con emulador o dispositivo.
- La verificación de TalkBack es manual; un test automático no la sustituye.

## Criterio de terminado

- Todas las funciones del inventario operativas.
- JSON de iOS se importa sin pérdida y el de Android se importa en iOS.
- Checklist TalkBack superada en dispositivo real.
- Sin dependencias fuera de la lista permitida.
- Textos en es y en completos.
