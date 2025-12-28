@echo off
cd /d "%~dp0"
echo Building project...
mvn -q package
if errorlevel 1 (
  echo mvn package failed.
  pause
  exit /b 1
)

set "JAR="
for %%f in (target\*.jar) do set "JAR=%%f"

if defined JAR (
  echo Running %%JAR%%...
  java -jar "%JAR%" --open-browser
) else (
  echo No jar found in target\*.jar, trying compiled classes...
  java -cp target/classes labreport.Main --open-browser
)

pause
