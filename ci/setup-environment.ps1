param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$JavaSDKEnvVar,
    # standard, from select-options.ps1, leaves out the tests WMIC is for.
    [string]$Depth,
    # False on the Intel macOS entries, as live cloud tests run once per
    # operating system and the arm64 entries run them for macOS.
    [bool]$LiveCloudTests = $true
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

./java/setup-enviroment.ps1 -RepoName $RepoName -JavaSDKEnvVar $JavaSDKEnvVar

# Every live cloud test skips without a resource key, and the later steps run
# in this process, so removing the keys here leaves them out of the whole job.
if (-not $LiveCloudTests) {
    foreach ($key in @(Get-ChildItem Env:_51DEGREES_RESOURCE_KEY*)) {
        Remove-Item "Env:$($key.Name)"
        Write-Host "Live cloud tests are not run here, so $($key.Name) is removed"
    }
}

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
