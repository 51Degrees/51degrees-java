param (
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Version
)
$ErrorActionPreference = "Stop"
. "$PSScriptRoot/release.ps1"

# The shared publish runs this in place of its own check, because each package
# has its own version (see release.ps1). Every package about to be published
# goes through the shared steps/check-version-bump.ps1, run from the common-ci
# root, against its own '<package>/<version>' tags. That refuses a major or
# minor move unless ALLOW_VERSION_BUMP is 'true'. $Version is common-ci's label
# for the run.

Write-Host "The publish worked out $Version, its highest. Each package is checked on its own."

$plan = @(Get-ReleasePlan $RepoName)
if ($plan.Count -eq 0) {
    Write-Host "No package is to be published, so there is nothing to check."
    exit 0
}

$refused = @()
foreach ($release in $plan) {
    $global:LASTEXITCODE = 0
    ./steps/check-version-bump.ps1 -Version $release.Version -RepoName $RepoName -TagPrefix $release.Name
    if ($LASTEXITCODE -ne 0) {
        $refused += $release.Name
    }
}

if ($refused) {
    Write-Host "Nothing is published, because these packages were refused: $($refused -join ', ')."
    exit 1
}
exit 0
