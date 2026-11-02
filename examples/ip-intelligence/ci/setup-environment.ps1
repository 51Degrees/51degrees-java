param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$JavaSDKEnvVar
)

./java/setup-enviroment.ps1 -RepoName $RepoName -JavaSDKEnvVar $JavaSDKEnvVar
