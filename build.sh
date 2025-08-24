#!/bin/bash

# Set Java 21 as the JAVA_HOME for this project
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

echo "Using Java version:"
$JAVA_HOME/bin/java -version

echo ""
echo "Building project..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "JAR files created in build/libs/"
else
    echo "Build failed!"
    exit 1
fi
