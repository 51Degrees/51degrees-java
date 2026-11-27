param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    # How deeply a pull request is tested, as select-options.ps1 chose.
    # standard leaves out the tests listed in standard-excludes.txt. Empty, for
    # the branch itself or a release, runs every test.
    [string]$Depth
)
$ErrorActionPreference = "Stop"

# Sets _51DEGREES_WARNINGS_AS_ERRORS, so the test build makes every warning an
# error.
. $PSScriptRoot/packages.ps1

# One run of every test in the reactor that build-project.ps1 built, the
# examples included. build-project.ps1 compiled them with JDK 17, and Surefire
# runs them here with Maven on the entry's own JDK, which setup-environment.ps1
# made JAVA_HOME, without compiling. common-ci runs Surefire from the root,
# copies the reports of every module at any depth into test-results/unit,
# leaving out the example tests the integration tests report, and logs why a
# forked JVM died when one does. The dev profile points the examples at the
# reactor. The tests read their keys from the environment, each under its own
# _51DEGREES_ name. A test that needs every property reads
# _51DEGREES_RESOURCE_KEY_BESPOKE. The data file update example tests read no
# key: they give the example a made-up key and a stand-in for the Distributor
# on this machine. No secret fills _51DEGREES_RESOURCE_KEY, which is the name
# a person running an example sets. A test that runs an example gives the
# example its key under that name. A pull request at the standard depth leaves out the tests in
# standard-excludes.txt.
$extraArgs = @(
    '-Pdev'
)
if ($Depth -eq 'standard') {
    $extraArgs += "-Dsurefire.excludesFile=$PSScriptRoot/standard-excludes.txt"
}
./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs $extraArgs

exit $LASTEXITCODE
