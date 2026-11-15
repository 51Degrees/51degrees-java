param (
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Version
)
$ErrorActionPreference = "Stop"
. "$PSScriptRoot/release.ps1"

# Called by common-ci's publish in place of its own check, because a plain tag
# lookup cannot tell this repository's packages apart. Exits 0 when the release
# plan has something in it and 1 when it is empty, as common-ci's check does.
# $Version is common-ci's label for the run and is not used here. Each
# package's version is checked by check-version-bump.ps1.

$plan = @(Get-ReleasePlan $RepoName)
if ($plan.Count -eq 0) {
    Write-Output "No package changed since its last release, so there is nothing to publish"
    exit 1
}

Write-Output "This publish releases $($plan.Count) packages, in this order:"
$plan | Format-Table Name, Previous, Version, Reason -AutoSize | Out-String -Width 200 |
    Write-Output
exit 0
