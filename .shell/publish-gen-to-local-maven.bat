@echo off
setlocal enableextensions enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "GENERATED_DIR=%PROJECT_ROOT%\build\generated\openapi-client"
set "POM_FILE=%GENERATED_DIR%\pom.xml"

if not exist "%POM_FILE%" (
    echo ERROR: Generated pom.xml not found at %POM_FILE% 1>&2
    echo ERROR: Run generateJavaClient task first. 1>&2
    exit /b 1
)

mvn install ^
    --file "%POM_FILE%" ^
    --batch-mode ^
    --quiet ^
    -DskipTests

if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven install failed with exit code %ERRORLEVEL% 1>&2
    exit /b %ERRORLEVEL%
)

endlocal