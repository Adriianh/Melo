param(
    [Parameter(Mandatory = $true)]
    [int]$ParentPid,

    [Parameter(Mandatory = $true)]
    [string]$InstallerPath,

    [Parameter(Mandatory = $true)]
    [ValidateSet("exe", "msi")]
    [string]$InstallerType,

    [Parameter(Mandatory = $false)]
    [string]$AppExecutable = ""
)

$ErrorActionPreference = "Stop"

function Resolve-MeloExecutable {
    param([string]$PreferredPath)

    if (-not [string]::IsNullOrWhiteSpace($PreferredPath) -and (Test-Path -LiteralPath $PreferredPath -PathType Leaf)) {
        return $PreferredPath
    }

    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Programs\Melo\Melo.exe"),
        (Join-Path $env:ProgramFiles "Melo\Melo.exe")
    )

    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            return $candidate
        }
    }

    return $null
}

function Start-Melo {
    param([string]$PreferredPath)

    $target = Resolve-MeloExecutable -PreferredPath $PreferredPath
    if ($null -eq $target) {
        Write-Output "Unable to locate Melo.exe to relaunch."
        return
    }

    try {
        Start-Process -FilePath $target -WorkingDirectory (Split-Path -Parent $target)
    } catch {
        Write-Output "Failed to launch ${target}: $($_.Exception.Message)"
    }
}

# Wait for the running Melo process to exit so JVM and native DLL locks are released.
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    $running = Get-Process -Id $ParentPid -ErrorAction SilentlyContinue
    if ($null -eq $running) {
        break
    }
    Start-Sleep -Milliseconds 500
}

if (-not (Test-Path -LiteralPath $InstallerPath -PathType Leaf)) {
    Write-Output "Downloaded installer not found: $InstallerPath"
    Start-Melo -PreferredPath $AppExecutable
    exit 1
}

try {
    if ($InstallerType -eq "msi") {
        $msiArgs = @("/i", "`"$InstallerPath`"", "/passive")
        $installerProcess = Start-Process -FilePath "msiexec.exe" -ArgumentList $msiArgs -Wait -PassThru
    } else {
        $exeArgs = @("/SILENT", "/CLOSEAPPLICATIONS")
        $installerProcess = Start-Process -FilePath $InstallerPath -ArgumentList $exeArgs -Wait -PassThru
    }

    $exitCode = $installerProcess.ExitCode
    if ($exitCode -eq 0 -or $exitCode -eq 3010) {
        Remove-Item -LiteralPath $InstallerPath -Force -ErrorAction SilentlyContinue
        Start-Melo -PreferredPath $AppExecutable
        exit 0
    } else {
        Write-Output "Installer exited with code $exitCode; relaunching previous version."
        Start-Melo -PreferredPath $AppExecutable
        exit $exitCode
    }
} catch {
    Write-Output "Installer execution failed: $($_.Exception.Message)"
    Start-Melo -PreferredPath $AppExecutable
    exit 1
}
