param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][hashtable]$Keys,
    [string]$Name
)

./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs "-DSuperResourceKey=$($Keys.TestResourceKey)"
