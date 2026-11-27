param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    # The runner variable naming the entry's JDK. The build compiles with JDK
    # 17 of the same architecture, whichever JDK the entry tests on.
    [Parameter(Mandatory)][string]$JavaSDKEnvVar
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

$compileJavaHome = Get-CompileJavaHome $JavaSDKEnvVar
Write-Host "Compiling with $compileJavaHome, testing with $ENV:JAVA_HOME"

# A publish releases a package's dependents only when packages.json names
# them, so a module or dependency it leaves out fails here, before the change
# is merged, rather than at the publish.
$gaps = @(Get-PackagesJsonGaps $RepoName)
if ($gaps.Count -gt 0) {
    throw "ci/packages.json does not match the poms:`n$($gaps -join "`n")"
}

# One build of the whole reactor, native libraries included. The dev profile
# builds the examples against the libraries here rather than the versions
# published to Maven Central, so a change to a library is tested by the
# examples in the same run. The unit tests use the same profile.
Push-Location $RepoName
try {
    Write-Host "Building $Name..."
    Invoke-WithJavaHome $compileJavaHome {
        mvn install @MavenOptions -DskipTests -Pdev
    }
} finally {
    Pop-Location
}
