param (
    # The data file change workflow passes the tools repository here, as the
    # generator runs there. This repository is found from this script instead.
    [Parameter(Mandatory)][string]$RepoName,
    # The workflow passes one data type, but this repository holds both
    # products. The accessors of both are generated in the one run, so they
    # arrive in one pull request rather than two runs pushing to the same
    # branch. Accepted so the workflow can still pass it.
    [string]$DataType
)
$ErrorActionPreference = "Stop"

. "$PSScriptRoot/packages.ps1"

$repoPath = "$PSScriptRoot/.."

foreach ($accessor in $Accessors) {
    Write-Host "Generating the $($accessor.DataType) accessors"
    ./tools/ci/generate-accessors.ps1 -DataType:$($accessor.DataType)

    foreach ($file in $accessor.Files) {
        Copy-Item "tools/Java/$file" "$repoPath/$($accessor.Destination)/"
    }
}
