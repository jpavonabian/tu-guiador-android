@echo off
rem Compila, pasa tests unitarios y lint. No necesita el movil.
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

if not exist gradlew.bat (
    echo No existe gradlew.bat. Mira el README, paso "Wrapper".
    pause
    exit /b 1
)

call gradlew.bat assembleDebug testDebugUnitTest lint
if errorlevel 1 (
    echo.
    echo FALLO la compilacion o la verificacion.
    pause
    exit /b 1
)

echo.
echo OK. APK: app\build\outputs\apk\debug\app-debug.apk
pause
