@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%APP_HOME%\.mvn\wrapper\maven-wrapper.properties

if "%JAVA_HOME%"=="" (
  set JAVA_EXE=java
) else (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

if not exist "%WRAPPER_JAR%" (
  if not exist "%APP_HOME%\.mvn\wrapper" mkdir "%APP_HOME%\.mvn\wrapper"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%WRAPPER_JAR%'"
)

if not exist "%JAVA_EXE%" (
  echo JAVA_HOME is not set correctly and java could not be found.
  exit /b 1
)

set MAVEN_PROJECTBASEDIR=%APP_HOME%
"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory=%APP_HOME% -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
