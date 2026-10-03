#!/usr/bin/env bash
set -e

if ! command -v java &> /dev/null; then
    echo "ERROR: 'java' runtime was not found in PATH. Please install Java 8+ and add it to your PATH."
    exit 1
fi

if [ ! -f "bin/SRC/LibraryServer.class" ]; then
    echo "Binaries not found. Running build.sh first..."
    ./build.sh
fi

echo "Starting Smart Library Management System on http://localhost:8080 ..."
java -cp bin SRC.LibraryServer
