# Dropper CLI Installation Script for Windows
# Usage:
#   iwr https://raw.githubusercontent.com/sebastianstupak/dropper/main/scripts/install.ps1 -useb | iex
#   .\install.ps1 [version]
#   .\install.ps1 v1.0.0  # Install specific version
#   .\install.ps1 draft   # Install latest draft release

param(
    [string]$Version = "latest"
)

$ErrorActionPreference = "Stop"

$Repo = "sebastianstupak/dropper"
$InstallDir = if ($env:DROPPER_INSTALL_DIR) { $env:DROPPER_INSTALL_DIR } else { "$env:LOCALAPPDATA\Dropper" }
$BinDir = "$InstallDir\bin"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
    exit 1
}

function Get-Platform {
    $arch = if ([Environment]::Is64BitOperatingSystem) { "amd64" } else { "386" }

    # Check if ARM64
    $proc = (Get-WmiObject -Class Win32_Processor).Architecture
    if ($proc -eq 12) { # ARM64
        $arch = "arm64"
    }

    return "dropper-windows-$arch.exe"
}

function Get-ReleaseInfo {
    Write-Info "Fetching release information..."

    $releaseUrl = if ($Version -eq "latest") {
        "https://api.github.com/repos/$Repo/releases/latest"
    } elseif ($Version -eq "draft") {
        # Get latest draft release
        $releases = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases"
        $draftRelease = $releases | Where-Object { $_.draft -eq $true } | Select-Object -First 1
        if (-not $draftRelease) {
            Write-Error-Custom "No draft releases found"
        }
        return $draftRelease
    } else {
        "https://api.github.com/repos/$Repo/releases/tags/$Version"
    }

    try {
        $release = Invoke-RestMethod -Uri $releaseUrl
        return $release
    } catch {
        Write-Error-Custom "Could not fetch release: $_"
    }
}

function Download-Binary {
    param(
        [string]$DownloadUrl,
        [string]$OutputPath
    )

    Write-Info "Downloading from $DownloadUrl..."

    try {
        # Create directory if it doesn't exist
        $dir = Split-Path -Parent $OutputPath
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
        }

        # Download
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $OutputPath -UseBasicParsing

        if (-not (Test-Path $OutputPath)) {
            Write-Error-Custom "Download failed"
        }
    } catch {
        Write-Error-Custom "Download failed: $_"
    }
}

function Add-ToPath {
    param([string]$PathToAdd)

    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")

    if ($userPath -notlike "*$PathToAdd*") {
        $newPath = if ($userPath) { "$userPath;$PathToAdd" } else { $PathToAdd }
        [Environment]::SetEnvironmentVariable("Path", $newPath, "User")

        # Update current session
        $env:Path = "$env:Path;$PathToAdd"

        Write-Warn "Added $PathToAdd to user PATH"
        Write-Warn "Restart your terminal for PATH changes to take effect"
    }
}

function Verify-Installation {
    param([string]$BinaryPath)

    if (Test-Path $BinaryPath) {
        Write-Info "✓ Installation successful!"
        Write-Host ""
        Write-Host "Dropper installed to: $BinaryPath"
        Write-Host ""
        Write-Host "Get started:"
        Write-Host "  dropper init my-mod"
        Write-Host ""
        Write-Host "For help:"
        Write-Host "  dropper --help"
        Write-Host ""

        # Try to run --version
        try {
            & $BinaryPath --version 2>&1 | Out-Null
        } catch {
            Write-Warn "Binary installed but may need permissions or dependencies"
        }
    } else {
        Write-Error-Custom "Installation verification failed"
    }
}

# Main installation flow
function Main {
    Write-Info "Installing Dropper CLI for Windows..."

    $binaryName = Get-Platform
    $release = Get-ReleaseInfo
    $releaseTag = $release.tag_name

    Write-Info "Installing Dropper $releaseTag"

    $downloadUrl = "https://github.com/$Repo/releases/download/$releaseTag/$binaryName"
    $binaryPath = "$BinDir\dropper.exe"

    Download-Binary -DownloadUrl $downloadUrl -OutputPath $binaryPath
    Add-ToPath -PathToAdd $BinDir
    Verify-Installation -BinaryPath $binaryPath
}

Main
