param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
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
# does. The dev profile points the examples at the reactor. The tests read
# their keys from the environment, where common-ci has put them under their
# _51DEGREES_ names: _51DEGREES_RESOURCE_KEY, _51DEGREES_RESOURCE_KEY_BESPOKE
# for the Location and the TAC and native model tests, and
# _51DEGREES_LICENSE_KEY for the Device Detection data file update example.
# A pull request at the standard depth leaves out the tests in
# standard-excludes.txt.
$extraArgs = @(
    '-Pdev'
)
if ($Depth -eq 'standard') {
    $extraArgs += "-Dsurefire.excludesFile=$PSScriptRoot/standard-excludes.txt"
}
./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs $extraArgs

exit $LASTEXITCODE
