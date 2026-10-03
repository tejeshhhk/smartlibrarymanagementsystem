#!/usr/bin/env bash
set -e

echo "Resetting SRC/data/ from SRC/data.sample/..."
mkdir -p SRC/data

cp SRC/data.sample/admins.txt SRC/data/admins.txt
cp SRC/data.sample/books.txt SRC/data/books.txt
cp SRC/data.sample/students.txt SRC/data/students.txt
cp SRC/data.sample/issues.txt SRC/data/issues.txt
cp SRC/data.sample/logs.txt SRC/data/logs.txt

echo "Data directory reset to initial sample state successfully!"
