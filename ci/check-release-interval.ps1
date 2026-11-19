param (
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Repository,
    [string]$HowToAllow
)
$ErrorActionPreference = "Stop"
. "$PSScriptRoot/release.ps1"

# The shared publish runs this in place of its own check, because each package
# has its own version and tags (see release.ps1). Every package about to be
# published goes through the shared steps/check-release-interval.ps1, run from
# the common-ci root, against its own '<package>/<version>' tags, so each
# package gets at most one new version a week. That refuses a package
# released less than a week ago unless ALLOW_EARLY_RELEASE is 'true', from the
# date in WEEKLY_RELEASES_FROM.

$plan = @(Get-ReleasePlan $RepoName)
if ($plan.Count -eq 0) {
    Write-Host "No package is to be published, so there is nothing to check."
    exit 0
}

$arguments = @{ RepoName = $RepoName; Repository = $Repository }
if ($HowToAllow) {
    $arguments.HowToAllow = $HowToAllow
}

$refused = @()
foreach ($release in $plan) {
    Write-Host "$($release.Name):"
    $global:LASTEXITCODE = 0
    ./steps/check-release-interval.ps1 @arguments -TagPrefix $release.Name
    if ($LASTEXITCODE -ne 0) {
        $refused += $release.Name
    }
}

if ($refused) {
    Write-Host "Nothing is released, because these packages were released less than a week ago: $($refused -join ', ')."
    exit 1
}
exit 0
