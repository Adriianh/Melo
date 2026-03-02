param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo"
)

$BinDir = "$InstallDir\bin"

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
$newPath = ($currentPath -split ";" | Where-Object { $_ -ne $BinDir }) -join ";"
[Environment]::SetEnvironmentVariable("PATH", $newPath, "User")

Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $InstallDir

Write-Host "✓ Melo uninstalled."

