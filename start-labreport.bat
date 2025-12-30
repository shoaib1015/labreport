@echo off
setlocal

REM Resolve application directory
set APP_DIR=%~dp0
set JAR=%APP_DIR%target\labreport-1.0-SNAPSHOT.jar

echo Starting Lab Report System...

REM Start Java server in background (correct way)
start "LabReportServer" java -jar "%JAR%" REM Use javaw to avoid extra console window

REM Give server time to start
timeout /t 3 > nul

REM Launch Chrome APP MODE and WAIT for it to close
echo Launching application UI...
start "" /wait chrome --app=http://localhost:8080

REM When Chrome closes, stop ONLY our Java server
echo Application closed. Stopping server...
taskkill /f /fi "WINDOWTITLE eq LabReportServer*" > nul 2>&1

exit
