@echo off
REM ---------------------------------------------------------------------------
REM SAP Interface Cockpit - convenience runner (Windows)
REM
REM   run.cmd            build, test and start the cockpit
REM   run.cmd test       run the test suite only
REM   run.cmd package    build without starting
REM   run.cmd clean      remove build output
REM ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if "%~1"=="" goto run
if /I "%~1"=="test" goto test
if /I "%~1"=="package" goto package
if /I "%~1"=="clean" goto clean
goto run

:test
call mvn -B test
goto end

:package
call mvn -B clean package
goto end

:clean
call mvn -B clean
goto end

:run
echo.
echo Building the SAP Interface Cockpit...
call mvn -B clean package
if errorlevel 1 goto end

echo.
echo Starting on http://localhost:8082
echo Sign in with ops.lead / demo1234 for the full view.
echo.
java -jar target\sap-interface-cockpit.jar
goto end

:end
endlocal
