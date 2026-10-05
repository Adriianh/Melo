param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo-tui",
    [string]$ConfigDir  = "$env:APPDATA\Melo",
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

$configDirs = @($ConfigDir)
$legacyDir = Join-Path $env:USERPROFILE ".melo"
if (Test-Path $legacyDir) {
    $configDirs += $legacyDir
}
$existingDirs = $configDirs | Where-Object { Test-Path $_ }
if ($existingDirs) {
    $deleteConfig = $Force
    if (-not $deleteConfig) {
        $displayDirs = $existingDirs -join ", "
        $answer = Read-Host "Remove config directory $displayDirs? [y/N]"
        if ($answer -match '^[yY]') {
            $deleteConfig = $true
        }
    }
    if ($deleteConfig) {
        foreach ($dir in $existingDirs) {
            Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $dir
        }
        Write-Host "✓ Config removed."
    } else {
        Write-Host "  Config kept at $($existingDirs -join ', ')"
    }
}

Write-Host "✓ Melo TUI uninstalled successfully."
