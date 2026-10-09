# Entorno de build fijado para compilar Tu guiador de forma reproducible.
# Imagen base por digest: eclipse-temurin:21.0.9_10-jdk-noble (Ubuntu 24.04, JDK 21.0.9+10).
FROM eclipse-temurin@sha256:f3f6ba56a9ab68c1e99310d3b06bd1279ed03db00c581d8e22bb2e65d6ce8fa4

ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    LC_ALL=C.UTF-8 \
    TZ=UTC

# Herramientas de línea de comandos del SDK 23.0 (build 16111833), con comprobación de hash.
ARG CMDLINE_TOOLS=commandlinetools-linux-16111833_latest.zip
ARG CMDLINE_TOOLS_SHA256=0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip git \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && curl -fsSL -o /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/$CMDLINE_TOOLS" \
    && echo "$CMDLINE_TOOLS_SHA256  /tmp/cmdline-tools.zip" | sha256sum -c - \
    && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools" \
    && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip

# Versiones concretas: coinciden con compileSdk = 37 y buildToolsVersion de app/build.gradle.kts.
RUN yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null \
    && "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
        "platforms;android-37.0" "build-tools;36.0.0" \
    # sdkmanager deja algunos archivos (apksigner) sin permiso de lectura/ejecución para otros usuarios,
    # y firmar.sh corre el contenedor con el usuario del anfitrión.
    && chmod -R a+rX "$ANDROID_HOME"

WORKDIR /build
