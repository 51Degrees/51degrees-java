param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

Write-Host $ENV:JAVA_HOME

./java/build-project.ps1 @PSBoundParameters
