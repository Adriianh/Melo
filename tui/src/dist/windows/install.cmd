@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Unblock-File -Path '%~dp0*' -ErrorAction SilentlyContinue; & '%~dp0install.ps1' %* }"
