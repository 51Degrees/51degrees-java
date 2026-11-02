param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][hashtable]$Keys,
    [string]$Name
)

& ./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs @(
    "-DTestResourceKey=$($Keys.TestResourceKey)"
)
Write-Output "[ip-intelligence-java/ci/run-unit-tests.ps1] LASTEXITCODE = $LASTEXITCODE"

exit $LASTEXITCODE
