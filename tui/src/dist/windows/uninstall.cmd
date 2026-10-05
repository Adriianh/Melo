@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& { Unblock-File -Path '%~dp0*' -ErrorAction SilentlyContinue; & '%~dp0uninstall.ps1' %* }"
