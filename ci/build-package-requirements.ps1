param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

# Builds the native libraries of both on-premise engines for this runner's
# platform, and the modules they depend on.
Write-Host "Entering '$RepoName'"
Push-Location $RepoName
try {
    Write-Host "Building '$Name'"
    mvn package @MavenOptions -DskipTests -pl ($NativePackages.Path -join ',') -am
} finally {
    Write-Host "Leaving '$RepoName'"
    Pop-Location
}

# The publish workflow uploads this folder as an artifact, and gives the
# folders from every platform to build-package.ps1.
$packageFolder = New-Item -ItemType Directory -Path package-files -Force
foreach ($package in $NativePackages) {
    Get-ChildItem -Path "$RepoName/$($package.Path)/target/classes/*" -Include "*.dll", "*.so", "*.dylib" |
        Copy-Item -Destination $packageFolder
}
Get-ChildItem $packageFolder
