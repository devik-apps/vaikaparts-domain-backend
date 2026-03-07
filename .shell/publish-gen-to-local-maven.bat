@echo off
setlocal enableextensions enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "GENERATED_DIR=%PROJECT_ROOT%\build\generated\openapi-client"
set "POM_FILE=%GENERATED_DIR%\pom.xml"
set "LOCAL_VERSION=0.0.0-LOCAL-SNAPSHOT"

call :log_info "Validating prerequisites..."

if not exist "%POM_FILE%" (
    call :log_error "Generated pom.xml not found at '%POM_FILE%'."
    call :log_error "Run './gradlew generateJavaClient' first."
    exit /b 1
)

where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    call :log_error "Maven (mvn) is not installed or not on PATH."
    exit /b 1
)

call :log_info "Prerequisites validated."

call :log_info "Setting pom.xml version to '%LOCAL_VERSION%'..."

mvn versions:set ^
    -DnewVersion="%LOCAL_VERSION%" ^
    --file "%POM_FILE%" ^
    --batch-mode ^
    --quiet

if %ERRORLEVEL% neq 0 (
    call :log_error "Failed to set version. Exit code: %ERRORLEVEL%."
    exit /b %ERRORLEVEL%
)

mvn versions:commit ^
    --file "%POM_FILE%" ^
    --batch-mode ^
    --quiet

if %ERRORLEVEL% neq 0 (
    call :log_error "Failed to commit version. Exit code: %ERRORLEVEL%."
    exit /b %ERRORLEVEL%
)

call :log_info "pom.xml version set and committed."

call :log_info "Installing generated client to Maven local repository..."

mvn install ^
    --file "%POM_FILE%" ^
    --batch-mode ^
    --quiet ^
    -DskipTests

if %ERRORLEVEL% neq 0 (
    call :log_error "Maven install failed. Exit code: %ERRORLEVEL%."
    exit /b %ERRORLEVEL%
)

call :log_info "Client installed successfully as version '%LOCAL_VERSION%'."
call :log_info "Ensure your build.gradle dependency resolves to: com.devikapps:vaikaparts-gen:%LOCAL_VERSION%"

endlocal
exit /b 0

:log_info
echo [INFO]  %~1
exit /b 0

:log_error
echo [ERROR] %~1 1>&2
exit /b 0