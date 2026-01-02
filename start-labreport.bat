@echo off
setlocal

REM Force working directory to script location (CRITICAL)
cd /d "%~dp0"

REM Resolve JAR path
set JAR=target\labreport-1.0-SNAPSHOT.jar

if not exist "%JAR%" (
    echo ERROR: JAR not found at %JAR%
    pause
    exit /b 1
)

echo Starting Lab Report System...

REM Start Java server (no console)
start "LabReportServer" javaw -jar "%JAR%"

REM Give server time to start
timeout /t 3 > nul

REM Launch Chrome APP MODE
echo Launching application UI...
start "" chrome --app=http://localhost:8080

exit
