#!/usr/bin/env bash
set -e

if ! command -v javac &> /dev/null; then
    echo "ERROR: 'javac' compiler was not found in PATH. Please install JDK 8+ and add it to your PATH."
    exit 1
fi

mkdir -p bin

echo "Compiling Smart Library Management System (Java 8 compatibility mode)..."
find SRC -name "*.java" > sources.txt
javac --release 8 -d bin -sourcepath SRC @sources.txt
rm -f sources.txt

echo "Compilation successful! Class files built in bin/"
