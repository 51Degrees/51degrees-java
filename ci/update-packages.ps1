param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

# Updates the dependency versions of each tree to their latest patch. The
# examples may take snapshots, as they did in their own repositories.
#
# pipeline.version is left alone. The libraries set it to ${project.version}
# so that they build against the Pipeline API in this repository, and it must
# not be replaced by the latest published version.
foreach ($package in $Packages) {
    $allowSnapshots = (-not $package.Publish).ToString().ToLower()
    Write-Host "Updating dependencies of '$($package.Path)'. Patch version only. Snapshots allowed: $allowSnapshots."
    Push-Location "$RepoName/$($package.Path)"
    try {
        mvn versions:update-properties @MavenOptions `
            "-DallowMinorUpdates=false" `
            "-DallowSnapshots=$allowSnapshots" `
            "-DexcludeProperties=pipeline.version" `
            "-DgenerateBackupPoms=false"
    } finally {
        Pop-Location
    }
}
