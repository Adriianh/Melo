<#
.SYNOPSIS
    Melo TUI & CLI Uninstaller for Windows (PowerShell)

.DESCRIPTION
    Uninstalls Melo TUI binaries, command wrappers, and removes it from PATH.

.EXAMPLE
    irm https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/uninstall.ps1 | iex
    powershell -ExecutionPolicy Bypass -File scripts\uninstall.ps1 -Force
#>

[CmdletBinding()]
param (
    [string]$InstallDir = "$env:LOCALAPPDATA\melo-tui",
    [string]$ConfigDir  = "$env:APPDATA\Melo",
    [switch]$Force      = $false
)

$ErrorActionPreference = "Stop"

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

Write-Host ""
Write-Host "  Uninstalling Melo TUI..." -ForegroundColor Red
Write-Host ""

$BinDir = Join-Path $InstallDir "bin"

# 1. Clean from PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -like "*$BinDir*") {
    Write-Host "  ● Removing $BinDir from User PATH..." -ForegroundColor Magenta
    $newPath = ($currentPath -split ";" | Where-Object { $_ -and $_.TrimEnd('\') -ne $BinDir.TrimEnd('\') }) -join ";"
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    $env:Path = ($env:Path -split ";" | Where-Object { $_ -and $_.TrimEnd('\') -ne $BinDir.TrimEnd('\') }) -join ";"
    Write-Host "  ✔ Removed from User PATH." -ForegroundColor Green
}

# 2. Remove Install directory
if (Test-Path $InstallDir) {
    Write-Host "  ● Removing installation directory: $InstallDir..." -ForegroundColor Magenta
    Remove-Item -Path $InstallDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  ✔ Binaries removed." -ForegroundColor Green
}

# 3. Optional Config directory cleanup
$configDirs = @($ConfigDir)
$legacyDir = Join-Path $env:USERPROFILE ".melo"
if (Test-Path $legacyDir) {
    $configDirs += $legacyDir
}
$existingDirs = $configDirs | Where-Object { Test-Path $_ }
if ($existingDirs) {
    $deleteConfig = $Force
    if (-not $deleteConfig) {
        Write-Host ""
        $displayDirs = $existingDirs -join ", "
        $answer = Read-Host "  ? Remove user configuration & cache at $displayDirs? [y/N]"
        if ($answer -match '^[yY]') {
            $deleteConfig = $true
        }
    }

    if ($deleteConfig) {
        foreach ($dir in $existingDirs) {
            Remove-Item -Path $dir -Recurse -Force -ErrorAction SilentlyContinue
        }
        Write-Host "  ✔ Configuration directory removed." -ForegroundColor Green
    } else {
        Write-Host "  ℹ Configuration preserved at $($existingDirs -join ', ')" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "  ┌────────────────────────────────────────────────────────┐" -ForegroundColor Green
Write-Host "  │  ✔ Melo TUI was successfully uninstalled.              │" -ForegroundColor Green
Write-Host "  │  Thank you for using Melo!                             │" -ForegroundColor Green
Write-Host "  └────────────────────────────────────────────────────────┘" -ForegroundColor Green
Write-Host ""
