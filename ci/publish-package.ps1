param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys
)

# Uploads the one publishing bundle build-package.ps1 made, which holds the
# packages of every tree that publishes.
./java/publish-package-maven.ps1 -RepoName $RepoName -MavenSettings $Keys['MavenSettings'] -Version $Version

exit $LASTEXITCODE
