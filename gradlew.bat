@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

SET DIRNAME=%~dp0
IF "%DIRNAME%"=="" SET DIRNAME=.
SET APP_HOME=%DIRNAME%
SET GRADLE_VERSION=8.14
SET DIST_NAME=gradle-%GRADLE_VERSION%-bin
SET DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%.zip
SET CACHE_DIR=%APP_HOME%\.gradle\wrapper\dists\%DIST_NAME%
SET INSTALL_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%
SET GRADLE_BIN=%INSTALL_DIR%\bin\gradle.bat

IF NOT EXIST "%JAVA_HOME%\bin\java.exe" (
  ECHO JAVA_HOME is not set to a valid JDK. 1>&2
  EXIT /B 1
)

IF NOT EXIST "%GRADLE_BIN%" (
  IF NOT EXIST "%CACHE_DIR%" MKDIR "%CACHE_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%CACHE_DIR%\%DIST_NAME%.zip'; Expand-Archive -Path '%CACHE_DIR%\%DIST_NAME%.zip' -DestinationPath '%CACHE_DIR%' -Force"
  IF ERRORLEVEL 1 EXIT /B 1
)

CALL "%GRADLE_BIN%" %*