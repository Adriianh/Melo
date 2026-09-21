<#
.SYNOPSIS
    Melo TUI & CLI One-Line Installer for Windows (PowerShell)

.DESCRIPTION
    Installs or updates Melo TUI from official GitHub Releases.

.EXAMPLE
    irm https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.ps1 | iex
#>

[CmdletBinding()]
param (
    [string]$Version = "",
    [string]$InstallDir = "$env:LOCALAPPDATA\melo-tui"
)

$ErrorActionPreference = "Stop"
$Repo = "Adriianh/Melo"

# Enable VT100 / UTF-8 output where possible
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

Write-Host ""
Write-Host "    __  __      _       " -ForegroundColor Magenta
Write-Host "   |  \/  | ___| | ___  " -ForegroundColor Magenta
Write-Host "   | |\/| |/ _ \ |/ _ \ " -ForegroundColor Magenta
Write-Host "   | |  | |  __/ | (_) |" -ForegroundColor Magenta
Write-Host "   |_|  |_|\___|_|\___/ " -ForegroundColor Magenta
Write-Host "   Terminal Audio Player & CLI • https://github.com/$Repo" -ForegroundColor DarkGray
Write-Host ""

# Resolve version
if (-not $Version) {
    Write-Host "  ● Resolving target release..." -ForegroundColor Magenta
    try {
        $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ "User-Agent" = "Melo-Installer" }
        $Version = $release.tag_name.TrimStart('v')
    } catch {
        $Version = "2.1.1"
    }
} else {
    $Version = $Version.TrimStart('v')
}

$BinDir = Join-Path $InstallDir "bin"

Write-Host "  ┌────────────────────────────────────────────────────────┐" -ForegroundColor DarkGray
Write-Host "  │  Platform:  Windows (x86_64)                           │" -ForegroundColor DarkGray
Write-Host "  │  Version:   v$($Version.PadRight(42))│" -ForegroundColor DarkGray
Write-Host "  │  Target:    $($InstallDir.PadRight(43))│" -ForegroundColor DarkGray
Write-Host "  │  Binaries:  $($BinDir.PadRight(43))│" -ForegroundColor DarkGray
Write-Host "  └────────────────────────────────────────────────────────┘" -ForegroundColor DarkGray
Write-Host ""

$AssetName = "melo-$Version-windows.zip"
$DownloadUrl = "https://github.com/$Repo/releases/download/v$Version/$AssetName"

$TempZip = Join-Path $env:TEMP $AssetName
$TempExtract = Join-Path $env:TEMP "melo_extract_$([System.IO.Path]::GetRandomFileName())"

try {
    Write-Host "  ● Downloading $AssetName..." -ForegroundColor Magenta
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $TempZip -UseBasicParsing
    Write-Host "  ✔ Package downloaded successfully." -ForegroundColor Green

    Write-Host "  ● Extracting package..." -ForegroundColor Magenta
    Expand-Archive -Path $TempZip -DestinationPath $TempExtract -Force

    $innerFolder = Get-ChildItem -Path $TempExtract -Directory | Select-Object -First 1
    $sourceDir = if ($innerFolder) { $innerFolder.FullName } else { $TempExtract }

    if (-not (Test-Path (Join-Path $sourceDir "melo.exe"))) {
        throw "Could not find melo.exe in downloaded archive."
    }

    Write-Host "  ● Installing binaries and wrappers..." -ForegroundColor Magenta
    if (-not (Test-Path $InstallDir)) {
        New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
    }

    Copy-Item -Path "$sourceDir\*" -Destination $InstallDir -Recurse -Force

    if (-not (Test-Path $BinDir)) {
        New-Item -ItemType Directory -Path $BinDir -Force | Out-Null
    }

    $wrapperCmd = @"
@echo off
"$InstallDir\melo.exe" %*
"@
    $wrapperPs1 = @"
& "$InstallDir\melo.exe" @args
"@
    Set-Content -Path (Join-Path $BinDir "melo.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo.bat") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo.ps1") -Value $wrapperPs1

    Set-Content -Path (Join-Path $BinDir "melo-tui.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-tui.bat") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-tui.ps1") -Value $wrapperPs1

    Set-Content -Path (Join-Path $BinDir "melo-cli.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-cli.bat") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-cli.ps1") -Value $wrapperPs1

    # User PATH check
    $userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
    $pathUpdated = $false
    if ($userPath -notlike "*$BinDir*") {
        Write-Host "  ● Adding $BinDir to User PATH..." -ForegroundColor Magenta
        [Environment]::SetEnvironmentVariable("Path", "$userPath;$BinDir", [EnvironmentVariableTarget]::User)
        $env:Path = "$env:Path;$BinDir"
        $pathUpdated = $true
        Write-Host "  ✔ Added to User PATH." -ForegroundColor Green
    }

    Write-Host ""
    if ($pathUpdated) {
        Write-Host "  ┌────────────────────────────────────────────────────────┐" -ForegroundColor Yellow
        Write-Host "  │  Notice: User PATH was updated.                        │" -ForegroundColor Yellow
        Write-Host "  │  Restart your terminal or PowerShell session to use.   │" -ForegroundColor Yellow
        Write-Host "  └────────────────────────────────────────────────────────┘" -ForegroundColor Yellow
        Write-Host ""
    }

    Write-Host "  ┌────────────────────────────────────────────────────────┐" -ForegroundColor Green
    Write-Host "  │  ✦ Installation complete!                              │" -ForegroundColor Green
    Write-Host "  │                                                        │" -ForegroundColor Green
    Write-Host "  │  Run melo              Start the interactive TUI player│" -ForegroundColor Green
    Write-Host "  │  Run melo play <song>  Instant playback from terminal  │" -ForegroundColor Green
    Write-Host "  │  Run melo --help       Explore full CLI commands       │" -ForegroundColor Green
    Write-Host "  └────────────────────────────────────────────────────────┘" -ForegroundColor Green
    Write-Host ""
}
finally {
    if (Test-Path $TempZip) { Remove-Item -Path $TempZip -Force -ErrorAction SilentlyContinue }
    if (Test-Path $TempExtract) { Remove-Item -Path $TempExtract -Recurse -Force -ErrorAction SilentlyContinue }
}
