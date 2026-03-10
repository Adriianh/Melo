param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo",
    [string]$ConfigDir  = "$env:APPDATA\melo"
)

$BinDir = "$InstallDir\bin"

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
$newPath = ($currentPath -split ";" | Where-Object { $_ -ne $BinDir }) -join ";"
[Environment]::SetEnvironmentVariable("PATH", $newPath, "User")

Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $InstallDir

if (Test-Path $ConfigDir) {
    $answer = Read-Host "Remove config directory $ConfigDir? [y/N]"
    if ($answer -match '^[yY]') {
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $ConfigDir
        Write-Host "✓ Config removed."
    } else {
        Write-Host "  Config kept at $ConfigDir"
    }
}

Write-Host "✓ Melo native binary uninstalled."
