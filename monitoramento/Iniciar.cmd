@echo off
set "MONITOR_PYTHON=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
if not exist "%MONITOR_PYTHON%" set "MONITOR_PYTHON=python"
"%MONITOR_PYTHON%" -X utf8 "%~dp0monitor.py" start
pause
