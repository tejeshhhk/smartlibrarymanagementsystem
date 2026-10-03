@echo off
setlocal

echo Checking Java installation...
where javac >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: 'javac' compiler was not found in PATH. Please install JDK 8+ and add it to your PATH environment variable.
    exit /b 1
)

if not exist bin mkdir bin

echo Compiling Smart Library Management System (Java 8 compatibility mode)...
dir /b /s SRC\*.java > sources.txt
javac --release 8 -d bin -sourcepath SRC @sources.txt
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed!
    if exist sources.txt del sources.txt
    exit /b 1
)
if exist sources.txt del sources.txt

echo Compilation successful! Class files built in bin\
endlocal
