@echo off
rem Usage: scripts\eval.bat <qrels file> <run file> [extra trec_eval args]
rem Windows wrapper: converts the two paths for WSL and runs trec_eval there.
if "%~2"=="" (echo usage: eval.bat ^<qrels^> ^<run^> & exit /b 1)
for /f "delims=" %%i in ('wsl wslpath -a "%~f1"') do set QRELS=%%i
for /f "delims=" %%i in ('wsl wslpath -a "%~f2"') do set RUN=%%i
wsl -- bash -lc "$HOME/trec_eval/trec_eval -m map '%QRELS%' '%RUN%'"
