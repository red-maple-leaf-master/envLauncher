@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME=D:\environment\JDK\jdk-17.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_REPO=C:\Users\yi.wan\.m2\repository"
set "OUTPUT_DIR=target\portable\env-launcher"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 17 not found: %JAVA_HOME%
    exit /b 1
)

echo [INFO] Using JAVA_HOME=%JAVA_HOME%
java -version
if errorlevel 1 exit /b 1

javac -version
if errorlevel 1 exit /b 1

call mvn -version
if errorlevel 1 exit /b 1

echo [INFO] Building portable bundle...
call mvn clean package -Pportable-app-image "-Dmaven.repo.local=%MAVEN_REPO%"
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
