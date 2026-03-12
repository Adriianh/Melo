param(
    [string]$InstallDir = "$env:LOCALAPPDATA\melo",
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

Set-Content "$BinDir\melo.bat" "@echo off`r`n\"$InstallDir\melo.exe\" %*`r`n"
Set-Content "$BinDir\melo.ps1" "& \"$InstallDir\melo.exe\" @args`r`n"

# Create a .env template only if one doesn't exist yet
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

# Add to user PATH if not already present
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$BinDir*") {
    [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$BinDir", "User")
    Write-Host "✓ Added $BinDir to your user PATH."
    Write-Host "  Restart your terminal for the change to take effect."
} else {
    Write-Host "  PATH already contains $BinDir, skipping."
}

Write-Host ""
Write-Host "✓ Melo native binary installed to $InstallDir"
Write-Host "✓ Config directory created at $ConfigDir"
Write-Host ""
Write-Host "Add your API keys to $ConfigDir\.env before running Melo."
Write-Host "  Run: melo search"
