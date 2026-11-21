param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$JavaSDKEnvVar,
    # standard, from select-options.ps1, leaves out the tests WMIC is for.
    [string]$Depth
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

./java/setup-enviroment.ps1 @PSBoundParameters

# Without WMIC a forked test JVM on Windows can end itself part way through
# the long running data file tests. The script explains why.
if ($Depth -ne 'standard') {
    & "$PSScriptRoot/install-wmic.ps1"
}

# The native builds run PreBuild.sh, which has to be executable whatever the
# checkout did to its mode.
if ($IsLinux -or $IsMacOS) {
    foreach ($package in $NativePackages) {
        $preBuild = "$RepoName/$($package.Path)/src/main/cxx/PreBuild.sh"
        Write-Host "Marking '$preBuild' as executable"
        chmod +x $preBuild
    }
}
