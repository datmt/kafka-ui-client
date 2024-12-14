#!/bin/bash

# Exit on error
set -e

# Build the project with Maven
mvn clean package -DskipTests

# Get the version from pom.xml
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
# Convert SNAPSHOT version to valid format for jpackage (e.g., 1.0-SNAPSHOT -> 1.0.0)
PACKAGE_VERSION=$(echo $VERSION | sed 's/-SNAPSHOT/.0/g' | sed 's/-/./')
APP_NAME="Kafka UI"
MAIN_JAR="kafka-ui-${VERSION}-jar-with-dependencies.jar"
MAIN_CLASS="com.kafka.ui.KafkaUIMain"

# Set icon path based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    ICON_PATH="src/main/resources/icon.icns"
else
    ICON_PATH="src/main/resources/icon.png"
fi

# Detect OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    INSTALLER_TYPE="dmg"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    if command -v dpkg >/dev/null 2>&1; then
        INSTALLER_TYPE="deb"
    elif command -v rpm >/dev/null 2>&1; then
        INSTALLER_TYPE="rpm"
    else
        echo "Unable to determine Linux package type. Defaulting to deb."
        INSTALLER_TYPE="deb"
    fi
else
    echo "Unsupported operating system"
    exit 1
fi

# Create the package using jpackage
jpackage \
  --input target \
  --main-jar ${MAIN_JAR} \
  --main-class ${MAIN_CLASS} \
  --name "${APP_NAME}" \
  --app-version ${PACKAGE_VERSION} \
  --type ${INSTALLER_TYPE} \
  --java-options "-Xms256m" \
  --java-options "-Xmx512m" \
  --vendor "Kafka UI Team" \
  --copyright "Copyright 2024" \
  --description "A user-friendly UI for Apache Kafka" \
  --icon ${ICON_PATH} \
  --verbose

echo "Package created successfully!"
