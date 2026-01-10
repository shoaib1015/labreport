@echo off
setlocal
cd /d "%~dp0"

set JAR=target\labreport-1.0-SNAPSHOT.jar

start "LabReportServer" javaw -jar "%JAR%"

timeout /t 2 > nul

start "" chrome --app=http://localhost:8080/app.html
exit
