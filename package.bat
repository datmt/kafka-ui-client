@echo off
setlocal EnableDelayedExpansion

REM Build the project with Maven
call mvn clean package

REM Get the version from pom.xml
for /f "tokens=*" %%a in ('mvn help:evaluate -Dexpression^=project.version -q -DforceStdout') do set VERSION=%%a

REM Convert SNAPSHOT version to valid format for jpackage
set PACKAGE_VERSION=%VERSION:-SNAPSHOT=.0%
set PACKAGE_VERSION=%PACKAGE_VERSION:-=.%

set APP_NAME=Kafka UI
set MAIN_JAR=kafka-ui-%VERSION%-jar-with-dependencies.jar
set MAIN_CLASS=com.kafka.ui.KafkaUIMain
set ICON_PATH=src\main\resources\icon.ico

REM Create the package using jpackage
jpackage ^
  --input target ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --name "%APP_NAME%" ^
  --app-version %PACKAGE_VERSION% ^
  --type msi ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --java-options "-Xms256m" ^
  --java-options "-Xmx512m" ^
  --vendor "Kafka UI Team" ^
  --copyright "Copyright 2024" ^
  --description "A user-friendly UI for Apache Kafka" ^
  --icon %ICON_PATH% ^
  --verbose

echo Package created successfully!
pause
