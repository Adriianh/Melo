param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo-tui",
    [string]$ConfigDir  = "$env:APPDATA\melo",
    [switch]$Force      = $false
)

$BinDir = "$InstallDir\bin"

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -like "*$BinDir*") {
    $newPath = ($currentPath -split ";" | Where-Object { $_ -and $_.TrimEnd('\') -ne $BinDir.TrimEnd('\') }) -join ";"
    [Environment]::SetEnvironmentVariable("PATH", $newPath, "User")
    $env:Path = ($env:Path -split ";" | Where-Object { $_ -and $_.TrimEnd('\') -ne $BinDir.TrimEnd('\') }) -join ";"
}

Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $InstallDir

if (Test-Path $ConfigDir) {
    $deleteConfig = $Force
    if (-not $deleteConfig) {
        $answer = Read-Host "Remove config directory $ConfigDir? [y/N]"
        if ($answer -match '^[yY]') {
            $deleteConfig = $true
        }
    }
    if ($deleteConfig) {
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $ConfigDir
        Write-Host "✓ Config removed."
    } else {
        Write-Host "  Config kept at $ConfigDir"
    }
}

Write-Host "✓ Melo TUI uninstalled successfully."
