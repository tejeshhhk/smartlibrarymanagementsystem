@echo off
setlocal

echo Checking Java runtime environment...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: 'java' runtime was not found in PATH. Please install Java 8+ and add it to your PATH environment variable.
    exit /b 1
)

if not exist bin\SRC\LibraryServer.class (
    echo Binaries not found. Running build.bat first...
    call build.bat
    if %errorlevel% neq 0 exit /b 1
)

echo Starting Smart Library Management System on http://localhost:8080 ...
java -cp bin SRC.LibraryServer
endlocal
