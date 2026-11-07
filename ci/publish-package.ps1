param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$OrgName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys,
    [bool]$DryRun = $false
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/release.ps1"

# The plan is read before anything is tagged, because the tags made below
# change what the plan would say.
$plan = @(Get-ReleasePlan $RepoName)

# Uploads the one publishing bundle build-package.ps1 made, which holds every
# package this publish releases.
./java/publish-package-maven.ps1 -RepoName $RepoName -MavenSettings $Keys['MavenSettings'] -Version $Version -DryRun $DryRun

# One tag and one GitHub release per package, '<package>/<version>', in place
# of common-ci's single tag, which would name no package. The notes list the
# commits that touched the package's files since its last release.
foreach ($release in $plan) {
    $tag = "$($release.Name)/$($release.Version)"
    git -C $RepoName tag $tag
    if ($DryRun -eq $true) {
        Write-Output "Dry run - tagged '$tag' locally, not pushed, no release made"
        continue
    }
    git -C $RepoName push origin $tag

    $notes = "$($release.Name) $($release.Version): $($release.Reason)."
    if ($null -ne $release.Previous) {
        $package = Get-PublishedPackages $RepoName |
            Where-Object { $_.Name -eq $release.Name }
        $range = "$($release.Name)/$($release.Previous)..HEAD"
        $commits = git -C $RepoName log --format='- %s (%h)' $range `
            -- (Get-PackagePaths $package)
        if ($commits) {
            $notes += "`n`n" + ($commits -join "`n")
        }
    }
    gh -R "$OrgName/$RepoName" release create $tag --verify-tag `
        --title "$($release.Name) $($release.Version)" --notes $notes
}

# Tells common-ci's publish not to add its own single tag and release.
$global:SkipUpdateTag = $true
