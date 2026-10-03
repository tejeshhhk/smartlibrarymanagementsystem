@echo off
setlocal

echo Resetting SRC/data/ from SRC/data.sample/...
if not exist SRC\data mkdir SRC\data

copy /Y SRC\data.sample\admins.txt SRC\data\admins.txt >nul
copy /Y SRC\data.sample\books.txt SRC\data\books.txt >nul
copy /Y SRC\data.sample\students.txt SRC\data\students.txt >nul
copy /Y SRC\data.sample\issues.txt SRC\data\issues.txt >nul
copy /Y SRC\data.sample\logs.txt SRC\data\logs.txt >nul

echo Data directory reset to initial sample state successfully!
endlocal
