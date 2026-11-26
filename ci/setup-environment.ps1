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

# This build is ours, so any warning fails it. The poms and the native CMake
# files read this, and the later steps run in this process. A build anyone
# else runs leaves it unset and only shows its warnings.
$env:_51DEGREES_WARNINGS_AS_ERRORS = 'true'

# Every live cloud test that reads a key skips without its resource key, and
# the later steps run in this process, so removing the keys here leaves them
# out of the whole job. The license key of the same account goes too, as the
# 51Did tests fail when it is set without its resource key. The tests that
# call the cloud with a fixed key skip when LIVE_CLOUD_TESTS is false.
if (-not $LiveCloudTests) {
    $resourceKeys = @(Get-ChildItem Env:_51DEGREES_RESOURCE_KEY*)
    $licenseKeys = @($resourceKeys | ForEach-Object {
        $_.Name -replace '^_51DEGREES_RESOURCE_KEY', '_51DEGREES_LICENSE_KEY'
    } | Where-Object { $_ -ne '_51DEGREES_LICENSE_KEY' })
    $names = @($resourceKeys.Name) + $licenseKeys + 'RESOURCE_KEY_BESPOKE_STAGING'
    foreach ($key in @(Get-ChildItem Env: | Where-Object { $names -ccontains $_.Name })) {
        Remove-Item "Env:$($key.Name)"
        Write-Host "Live cloud tests are not run here, so $($key.Name) is removed"
    }
    $env:LIVE_CLOUD_TESTS = 'false'
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
