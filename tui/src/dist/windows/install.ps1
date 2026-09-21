param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo-tui",
    [string]$ConfigDir  = "$env:APPDATA\melo"
)

$BinDir    = "$InstallDir\bin"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

New-Item -ItemType Directory -Force -Path $BinDir   | Out-Null
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null

Copy-Item -Path "$ScriptDir\melo.exe" -Destination "$InstallDir\melo.exe" -Force

# Copy any shared libraries (e.g., AWT .dll files) if they exist
if (Test-Path "$ScriptDir\*.dll") {
    Copy-Item -Path "$ScriptDir\*.dll" -Destination "$InstallDir\" -Force
}

Set-Content "$BinDir\melo-tui.bat" "@echo off`r`n`"$InstallDir\melo.exe`" %*`r`n"
Set-Content "$BinDir\melo-tui.cmd" "@echo off`r`n`"$InstallDir\melo.exe`" %*`r`n"
Set-Content "$BinDir\melo-tui.ps1" "& `"$InstallDir\melo.exe`" @args`r`n"
Set-Content "$BinDir\melo-cli.bat" "@echo off`r`n`"$InstallDir\melo.exe`" %*`r`n"
Set-Content "$BinDir\melo-cli.cmd" "@echo off`r`n`"$InstallDir\melo.exe`" %*`r`n"
Set-Content "$BinDir\melo-cli.ps1" "& `"$InstallDir\melo.exe`" @args`r`n"

$meloBat = @"
@echo off
if "%~1"=="--gui" (
    shift
    if exist "%LOCALAPPDATA%\Programs\Melo\Melo.exe" (
        start "" "%LOCALAPPDATA%\Programs\Melo\Melo.exe" %*
        exit /b 0
    ) else (
        echo Error: Melo GUI is not installed.
        exit /b 1
    )
)
if "%~1"=="-g" (
    shift
    if exist "%LOCALAPPDATA%\Programs\Melo\Melo.exe" (
        start "" "%LOCALAPPDATA%\Programs\Melo\Melo.exe" %*
        exit /b 0
    ) else (
        echo Error: Melo GUI is not installed.
        exit /b 1
    )
)
if "%~1"=="--tui" shift
if "%~1"=="-t" shift
"$InstallDir\melo.exe" %*
"@
Set-Content "$BinDir\melo.bat" $meloBat
Set-Content "$BinDir\melo.cmd" $meloBat

$meloPs1 = @"
param()
if (`$args.Count -gt 0 -and (`$args[0] -eq "--gui" -or `$args[0] -eq "-g")) {
    `$guiArgs = `$args | Select-Object -Skip 1
    `$guiPath = "`$env:LOCALAPPDATA\Programs\Melo\Melo.exe"
    if (Test-Path `$guiPath) {
        Start-Process `$guiPath -ArgumentList `$guiArgs
        exit 0
    } else {
        Write-Error "Error: Melo GUI is not installed."
        exit 1
    }
}
if (`$args.Count -gt 0 -and (`$args[0] -eq "--tui" -or `$args[0] -eq "-t")) {
    `$tuiArgs = `$args | Select-Object -Skip 1
    & "$InstallDir\melo.exe" @tuiArgs
    exit `$LASTEXITCODE
}
& "$InstallDir\melo.exe" @args
"@
Set-Content "$BinDir\melo.ps1" $meloPs1

$envFile = "$ConfigDir\.env"
if (-not (Test-Path $envFile)) {
    Set-Content $envFile @"
# Melo configuration
# Place your API keys here and restart the terminal.

# Last.fm API key (required for music discovery)
# Get yours at: https://www.last.fm/api/account/create
LASTFM_API_KEY=

# Spotify credentials (optional, improves search results)
# Get yours at: https://developer.spotify.com/dashboard
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=
"@
}

$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$BinDir*") {
    [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$BinDir", "User")
    Write-Host "✓ Added $BinDir to your user PATH."
    Write-Host "  Restart your terminal for the change to take effect."
} else {
    Write-Host "  PATH already contains $BinDir, skipping."
}

Write-Host ""
Write-Host "✓ Melo TUI native binary installed to $InstallDir"
Write-Host "✓ Dedicated launchers: melo-tui, melo-cli"
Write-Host "✓ Smart launcher: melo (use --gui to launch GUI if installed)"
Write-Host "✓ Config directory created at $ConfigDir"
Write-Host ""
Write-Host "Add your API keys to $ConfigDir\.env before running Melo."
Write-Host "  Run: melo search"
