@echo off
setlocal

set APP_HOME=%~dp0
if "%APP_HOME%"=="" set APP_HOME=.
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if exist "%WRAPPER_JAR%" goto runWrapper

where gradle >NUL 2>&1
if %ERRORLEVEL% equ 0 goto runSystemGradle

echo ERROR: gradle-wrapper.jar not found and system Gradle is unavailable. 1>&2
echo Install Java 21 and Gradle 8.14.3+ temporarily, then run gradlew wrapper once. 1>&2
exit /b 1

:runSystemGradle
echo gradle-wrapper.jar not found. Falling back to system Gradle. 1>&2
echo Tip: run gradlew wrapper once to materialize official wrapper files. 1>&2
gradle %*
exit /b %ERRORLEVEL%

:runWrapper
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
