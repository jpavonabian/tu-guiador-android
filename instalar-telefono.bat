@echo off
rem Compila, instala en el movil conectado por USB y abre la app.
cd /d "%~dp0"

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

if not exist gradlew.bat (
    echo No existe gradlew.bat. Mira el README, paso "Wrapper".
    pause
    exit /b 1
)

set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
if not exist "%ADB%" (
    echo No encuentro adb en "%ADB%".
    pause
    exit /b 1
)

"%ADB%" devices
call gradlew.bat installDebug
if errorlevel 1 (
    echo.
    echo FALLO la instalacion. Revisa que el movil este conectado y autorizado.
    pause
    exit /b 1
)

"%ADB%" shell am start -n es.jesuspavonabian.tuguiador/.MainActivity
echo.
echo Instalada y abierta.
pause
