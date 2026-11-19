@echo off
rem Builds the code, builds the index, writes the run file and validates it.
rem Usage: scripts\run_all.bat <dataFolder> <indexFolder> <topicsFile> <outputFile> <runName> [model] [T|TD|TDN]
rem   e.g. scripts\run_all.bat "C:\data\Assignment Two" index "C:\data\topics" results\baseline_v1.run baseline_v1 bm25 TD
rem Set SKIP_INDEX=1 to reuse an existing index folder (search + validate only).
setlocal
if "%~5"=="" (echo usage: run_all.bat ^<dataFolder^> ^<indexFolder^> ^<topicsFile^> ^<outputFile^> ^<runName^> [model] [mode] & exit /b 1)
set "DATA=%~1"
set "INDEX=%~2"
set "TOPICS=%~3"
set "OUT=%~4"
set "RUN=%~5"
set "MODEL=%~6"
if "%MODEL%"=="" set "MODEL=bm25"
set "MODE=%~7"
if "%MODE%"=="" set "MODE=TD"

cd /d "%~dp0.."
echo == 1/4 build
call mvn -q compile || (echo BUILD FAILED & exit /b 1)
call mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt || (echo CLASSPATH FAILED & exit /b 1)
set /p DEPS=<cp.txt
set "CP=target\classes;%DEPS%"

if "%SKIP_INDEX%"=="1" (echo == 2/4 index skipped) else (
  echo == 2/4 index
  java -cp "%CP%" cs7is3.Main index "%DATA%" "%INDEX%" || (echo INDEXING FAILED & exit /b 1)
)
echo == 3/4 search
java -cp "%CP%" cs7is3.Main search "%INDEX%" "%TOPICS%" "%OUT%" "%RUN%" %MODEL% %MODE% || (echo SEARCH FAILED & exit /b 1)
echo == 4/4 validate
python scripts\validate_run.py "%OUT%" || (echo VALIDATION FAILED & exit /b 1)
echo DONE. Next: scripts\eval.bat ^<qrels^> %OUT%
