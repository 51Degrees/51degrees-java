param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    [string]$TestResourceKey,
    [string]$DeviceDetection,
    # How deeply a pull request is tested, as select-options.ps1 chose.
    # standard leaves out the tests listed in standard-excludes.txt. Empty, for
    # the branch itself or a release, runs every test.
    [string]$Depth
)
$ErrorActionPreference = "Stop"

# One run of every test in the reactor that build-project.ps1 built, the
# examples included. common-ci runs Surefire from the root, copies the reports
# of every module at any depth into test-results/unit, leaving out the example
# tests the integration tests report, and logs why a forked JVM died when one
# does. The dev profile points the examples at the reactor. SuperResourceKey is
# read by the Location and Device Detection examples tests, and LicenseKey by
# the Device Detection data file update example. The IP Intelligence update
# example's test is ignored, so it does not need a key of its own. A pull
# request at the standard depth leaves out the tests in standard-excludes.txt.
$extraArgs = @(
    '-Pdev'
    "-DTestResourceKey=$TestResourceKey"
    "-DSuperResourceKey=$TestResourceKey"
    "-DLicenseKey=$DeviceDetection"
)
if ($Depth -eq 'standard') {
    $extraArgs += "-Dsurefire.excludesFile=$PSScriptRoot/standard-excludes.txt"
}
./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs $extraArgs

exit $LASTEXITCODE
