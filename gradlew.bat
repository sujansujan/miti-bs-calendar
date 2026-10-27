@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
set WRAPPER_URL=https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    if not exist "%APP_HOME%\gradle\wrapper" mkdir "%APP_HOME%\gradle\wrapper"
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

if defined JAVA_HOME (
    set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_EXE=java.exe
)

%JAVA_EXE% -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
