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
        $Version = "2.1.5"
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

    # Ensure DLLs (e.g. SMTCAdapter.dll, jnidispatch.dll) exist in $BinDir as well
    Get-ChildItem -Path $InstallDir -Filter "*.dll" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $BinDir -Force -ErrorAction SilentlyContinue
    }

    # Remove any legacy or stale .ps1 files so PowerShell executes .cmd wrappers directly
    # without triggering PSSecurityException / ExecutionPolicy restrictions.
    Remove-Item (Join-Path $BinDir "*.ps1") -Force -ErrorAction SilentlyContinue

    $wrapperCmd = @"
@echo off
"%~dp0..\melo.exe" %*
"@
    Set-Content -Path (Join-Path $BinDir "melo.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo.bat") -Value $wrapperCmd

    Set-Content -Path (Join-Path $BinDir "melo-tui.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-tui.bat") -Value $wrapperCmd

    Set-Content -Path (Join-Path $BinDir "melo-cli.cmd") -Value $wrapperCmd
    Set-Content -Path (Join-Path $BinDir "melo-cli.bat") -Value $wrapperCmd

    $gitBashWrapper = @"
#!/usr/bin/env sh
MELO_HOME="`$(cd "`$(dirname "`$0")/.." && pwd)"
exec "`$MELO_HOME/melo.exe" "`$@"
"@
    Set-Content -Path (Join-Path $BinDir "melo") -Value $gitBashWrapper
    Set-Content -Path (Join-Path $BinDir "melo-tui") -Value $gitBashWrapper
    Set-Content -Path (Join-Path $BinDir "melo-cli") -Value $gitBashWrapper

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

    # Check for ffplay (required for audio playback)
    $hasFfplay = $null -ne (Get-Command "ffplay" -ErrorAction SilentlyContinue)
    if (-not $hasFfplay) {
        $commonFfplayPaths = @(
            "$InstallDir\ffplay.exe",
            "$BinDir\ffplay.exe",
            "$env:LOCALAPPDATA\Microsoft\WinGet\Links\ffplay.exe",
            "$env:USERPROFILE\scoop\shims\ffplay.exe",
            "$env:ProgramFiles\ffmpeg\bin\ffplay.exe",
            "C:\ffmpeg\bin\ffplay.exe"
        )
        foreach ($p in $commonFfplayPaths) {
            if (Test-Path $p) {
                $hasFfplay = $true
                break
            }
        }
    }

    if (-not $hasFfplay) {
        Write-Host "  ● FFmpeg (ffplay) is required for audio playback." -ForegroundColor Yellow
        $winget = Get-Command "winget" -ErrorAction SilentlyContinue
        if ($winget) {
            Write-Host "  ● Attempting to install FFmpeg via winget..." -ForegroundColor Magenta
            try {
                & winget install -e --id Gyan.FFmpeg --accept-source-agreements --accept-package-agreements
                Write-Host "  ✔ FFmpeg installed via winget." -ForegroundColor Green
            } catch {
                Write-Host "  ⚠ Winget installation failed. Please install FFmpeg manually: winget install Gyan.FFmpeg" -ForegroundColor Yellow
            }
        } else {
            Write-Host "  ⚠ winget not found. Please install FFmpeg (ffplay) manually: https://ffmpeg.org/download.html" -ForegroundColor Yellow
        }
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
