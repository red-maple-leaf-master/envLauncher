@echo off
setlocal

cd /d "%~dp0"

set "OUTPUT_DIR=target\portable\env-launcher"

call mvn -version
if errorlevel 1 (
    echo [ERROR] mvn command not found. Please ensure Maven is available in PATH.
    exit /b 1
)

echo [INFO] Building portable bundle...
call mvn clean package -Pportable-app-image
if errorlevel 1 (
    echo [ERROR] Portable package build failed.
    exit /b 1
)

if not exist "%OUTPUT_DIR%\env-launcher.exe" (
    echo [ERROR] Portable package build finished, but exe was not found: %OUTPUT_DIR%\env-launcher.exe
    exit /b 1
)

echo [INFO] Portable bundle created successfully.
echo [INFO] Output: %OUTPUT_DIR%
endlocal
