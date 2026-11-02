param (
    [Parameter(Mandatory)][string]$RepoName,
    [string]$VariableName = "Version"
)
./java/get-next-package-version.ps1 -RepoName:$RepoName -VariableName:$VariableName
