# Icono de Tu guiador (Android)

Cómo se hizo el icono de la app Android y cómo volver a tocarlo.

## Resumen

- Un marcador de mapa blanco con un agujero circular, sobre una línea ondulada que hace de ruta.
- Todo en blanco sobre un fondo azul liso `#0B57D0`.
- Es un **icono adaptativo** de Android (fondo + primer plano + capa monocromo). Android lo recorta con la forma que use el lanzador (círculo, cuadrado redondeado, etc.).
- **No sale del icono de iOS.** Se dibujó desde cero en vectores. Mantiene la idea (marcador + ruta + azul), pero el archivo es independiente.

## Archivos

| Archivo | Qué es |
|---|---|
| `icono/primer_plano.svg` | **Fuente de verdad.** SVG estándar de 108×108, solo el primer plano, blanco sobre transparente. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | El mismo dibujo como vector de Android (mismas coordenadas, sin escalar). |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Rectángulo liso `#0B57D0` de 108×108. |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | Icono adaptativo: `background` + `foreground` + `monochrome` (el mismo primer plano). |
| `app/src/main/AndroidManifest.xml` | `android:icon="@mipmap/ic_launcher"`. Sin `roundIcon`. |

No hay PNG ni `.webp` por densidad: con `minSdk 29` todos los móviles usan el icono adaptativo.

## Geometría (viewBox 0 0 108 108)

- **Zona segura:** círculo de radio 33 centrado en (54, 54), es decir, los 66 dp centrales. Android nunca recorta ahí. Todo el dibujo está dentro (la ruta toca el borde pero no lo cruza).
- **Ruta:** `M36 76 C46 67 58 85 72 75`, trazo blanco de 6, extremos redondeados.
- **Marcador:** `M54 66 C54 66 40 52 40 42 A14 14 0 0 1 68 42 C68 52 54 66 54 66 Z`, con un agujero circular de radio 5 en (54, 42) (regla `evenodd`).
- **Colores:** blanco `#FFFFFF` y azul `#0B57D0`. Contraste blanco sobre ese azul: unos 6,4:1.
- Una sola tinta en el primer plano, así que sirve tal cual como capa monocromo (iconos con tema de Android 13+).

## Cómo se hizo

1. Se dibujó `icono/primer_plano.svg` a mano (coordenadas de arriba).
2. Se renderizó en Edge sin interfaz sobre fondo azul, con máscara de círculo y de cuadrado redondeado y con el círculo de zona segura marcado, para comprobar que se ve bien a 48 px y que no se recorta. Se descartaron dos versiones (una ruta de puntos, demasiado pequeña, y una ruta en S, que quedaba como un borrón).
3. Se importó en **Android Studio > New > Image Asset** (Launcher Icons, Adaptive and Legacy), nombre `ic_launcher`.
4. Se corrigió a mano lo que generó Studio (ver "Trampas").

No se usó ninguna skill ni herramienta de terceros.

## Trampas de Image Asset Studio

Cosas que hizo mal la primera vez y que hay que revisar siempre que se regenere:

- **Fondo verde de Android con cuadrícula** (`#3DDC84`) aunque se pidió Color `0B57D0`. Hay que dejar `ic_launcher_background.xml` como rectángulo liso azul.
- **Primer plano ampliado un 122 %** (`<group scaleX="1.2222222" …>`), con lo que la ruta se salía de la zona segura. Hay que quitar el grupo y dejar las rutas a escala 1.
- **Iconos antiguos y redondos** (`mipmap-*/ic_launcher*.webp`, `ic_launcher_round.xml`) aunque se desmarcaron. Sobran con `minSdk 29`; hay que borrarlos.
- **Carpeta `mipmap-anydpi-v26`**: lint pide llamarla `mipmap-anydpi` con `minSdk 29`.
- **`ic_launcher-playstore.png`** se genera en `app/src/main/` con el fondo equivocado. No va ahí (ver "Icono de 512 px").
- Studio ofrece añadir los archivos nuevos a git. Mejor cancelar y commitear a mano.

## Cómo modificarlo

1. Edita `icono/primer_plano.svg` (mantén todo dentro del círculo de radio 33 y una sola tinta).
2. Copia los mismos `d` de las rutas a `app/src/main/res/drawable/ic_launcher_foreground.xml`, en `android:pathData`. Las coordenadas son idénticas, no hay que convertir ni escalar. Para otro color de fondo, cambia `android:fillColor` en `ic_launcher_background.xml`.
3. Comprueba con `.\gradlew.bat assembleDebug lint` (lint debe seguir sin avisos).
4. Instala en el móvil y mira el icono en el lanzador y con "iconos con tema" activado.

Si se usa Image Asset Studio otra vez en vez de editar a mano, repasa la lista de trampas.

## Si cambia el icono de iOS

El icono de iOS (`Tu guiador/Assets.xcassets/AppIcon.appiconset/` en el repo `Tu-guiador`) **no se usa para nada** en Android, así que no hay que sincronizarlo. La versión original que había era una imagen con el icono en el centro, texto suelto y tres iconitos debajo, no un icono limpio.

Si se quiere que el icono de Android siga al de iOS:
- Cambia el azul en `ic_launcher_background.xml` y, si hace falta, la forma del marcador y de la ruta en el SVG.
- Lo que no se puede hacer es recortar el PNG de iOS: lleva ya la forma del icono de iOS y Android lo recortaría otra vez.
- Si el icono de iOS se convierte en un vector limpio o en un PNG de 1024 px con el primer plano separado del fondo, ese archivo sirve como nuevo `primer_plano` para Image Asset Studio.

## Icono de 512 px para F-Droid (pendiente, fase 6)

F-Droid usa un `icon.png` en los metadatos de fastlane (por ejemplo `fastlane/metadata/android/es-ES/images/icon.png`). Pendiente de generar: el primer plano blanco sobre el fondo azul, a 512×512 y sin transparencia. Se puede renderizar con el mismo método de Edge sin interfaz que se usó para comprobar el diseño.

## Cómo previsualizar

Una página HTML con el SVG dentro de un `div` azul con `border-radius` (50 % = círculo, 28 % = cuadrado redondeado) y capturarla:

```
msedge --headless --disable-gpu --screenshot=vista.png --window-size=1100,280 file:///ruta/vista.html
```

Para ver la zona segura, añade un `<circle cx="54" cy="54" r="33" fill="none" stroke="red" stroke-width="0.5"/>` encima.
