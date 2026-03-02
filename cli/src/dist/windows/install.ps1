param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo"
)

$BinDir = "$InstallDir\bin"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

New-Item -ItemType Directory -Force -Path $BinDir | Out-Null

Copy-Item -Path "$ScriptDir\melo.jar" -Destination "$InstallDir\melo.jar" -Force

Set-Content "$BinDir\melo.bat" "@echo off`r`njava -jar `"$InstallDir\melo.jar`" %*`r`n"
Set-Content "$BinDir\melo.ps1" "& java -jar `"$InstallDir\melo.jar`" @args`r`n"

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$BinDir*") {
    [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$BinDir", "User")
    Write-Host "✓ Added $BinDir to your user PATH."
    Write-Host "  Restart your terminal for the change to take effect."
} else {
    Write-Host "  PATH already contains $BinDir, skipping."
}

Write-Host ""
Write-Host "✓ Melo installed to $InstallDir"
Write-Host "  Run: melo search"

