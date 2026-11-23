param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    # Set by the publish workflow, which built and installed the packages, to
    # its label for the run. Not set when a pull request is tested, which
    # builds the reactor itself.
    [string]$Version,
    # False, from select-options.ps1, on every entry of a pull request but
    # one, so the browser contract runs once.
    [bool]$SeleniumContract = $true,
    # The branch of the shared Selenium contract tests to run, so a change
    # there can be tried here before it is merged.
    [string]$SeleniumTestsRef = 'main'
)
$ErrorActionPreference = "Stop"
# Each part's exit code is checked below, so one failing part does not stop
# the others from running.
$PSNativeCommandUseErrorActionPreference = $false

. "$PSScriptRoot/packages.ps1"

$results = "$RepoName/test-results/integration"
$failed = @()

# The examples take the libraries from Maven Central unless told otherwise.
# Pointing them at the versions the publish workflow built and installed, each
# package's own, tests the packages as a project of your own would use them.
# For a pull request the dev profile points them at the reactor
# build-project.ps1 installed.
if ($Version) {
    . "$PSScriptRoot/release.ps1"
    [string[]]$versionArgs = Get-ExampleVersionArguments $RepoName
} else {
    [string[]]$versionArgs = '-Pdev'
}

# The Pipeline API developer examples, as the pipeline repository ran them.
Write-Host "::group::Pipeline developer examples"
Push-Location "$RepoName/pipeline"
try {
    # The cloud engine example calls the live cloud service. Its test reads
    # the key from _51DEGREES_RESOURCE_KEY_BESPOKE and gives it to the example
    # as _51DEGREES_RESOURCE_KEY.
    mvn test @MavenOptions -DfailIfNoTests=false "-Dsurefire.failIfNoSpecifiedTests=false" -Dtest=ExampleTests
    if ($LASTEXITCODE -ne 0) { $failed += 'pipeline developer examples' }
} finally {
    Pop-Location
}
& "$PSScriptRoot/copy-test-results.ps1" -Path "$RepoName/pipeline" -Destination $results -Filter '*ExampleTests*'
Write-Host "::endgroup::"

# The product examples. A pull request has already run them in the unit tests,
# against the reactor, so they are run here only against a published version.
if ($Version) {
    # The tests read their keys from the _51DEGREES_ environment variables,
    # and the Enterprise data files were fetched with _51DEGREES_LICENSE_KEY.
    $examples = @('examples/device-detection', 'examples/ip-intelligence')
    foreach ($example in $examples) {
        Write-Host "::group::$example against $($versionArgs -join ' ')"
        Push-Location "$RepoName/$example"
        try {
            mvn clean test @MavenOptions @versionArgs
            if ($LASTEXITCODE -ne 0) { $failed += $example }
        } finally {
            Pop-Location
        }
        & "$PSScriptRoot/copy-test-results.ps1" -Path "$RepoName/$example" -Destination $results
        Write-Host "::endgroup::"
    }
} else {
    Write-Host "No version given, so the examples were tested by the unit tests against the reactor."
}

# The shared Selenium contract tests, run against one web example. The example
# is built, started in the background on its own port and left to serve until
# the tests finish. Every web example, cloud and on-premise, is expected to give
# the same client-side results, so each is tested the same way.
function Invoke-ContractTests {
    param(
        [Parameter(Mandatory)][string]$Label,
        [Parameter(Mandatory)][string]$Module,
        [Parameter(Mandatory)][int]$Port,
        # Environment variables the example needs, removed again afterwards.
        [hashtable]$ExampleEnv = @{}
    )
    Write-Host "Running the Selenium contract tests against the $Label example on port $Port"
    $example = $null
    Push-Location "$RepoName/examples/device-detection"
    try {
        mvn @MavenOptions -pl $Module -am package -DskipTests @versionArgs
        if ($LASTEXITCODE -ne 0) { throw "Building the $Label web example failed." }
        $jar = (Get-ChildItem "$Module/target/*-jar-with-dependencies.jar" | Select-Object -First 1).FullName
        # The port the example listens on, under the name the shared suite
        # gives an example it starts itself.
        ${env:_51DEGREES_EXAMPLE_HTTP_PORTS} = $Port
        foreach ($name in $ExampleEnv.Keys) {
            Set-Item "env:$name" $ExampleEnv[$name]
        }
        $example = java -jar $jar 2>&1 &
    } finally {
        Pop-Location
    }

    try {
        # Wait for the example to come up.
        curl -sS -o $(if ($IsWindows) { 'NUL' } else { '/dev/null' }) --retry 20 --retry-delay 3 --retry-max-time 120 --retry-connrefused "http://localhost:$Port"
        if ($LASTEXITCODE -ne 0) { throw "The $Label web example did not start on port $Port." }

        # The suite reads the running example's address and language under
        # its _51DEGREES_ names. Without _51DEGREES_EXAMPLE_URL it launches
        # an example of its own.
        ${env:_51DEGREES_EXAMPLE_URL} = "http://localhost:$Port"
        ${env:_51DEGREES_EXAMPLE_LANG} = 'java'
        # Normal verbosity prints each test's time, passed or failed.
        dotnet test selenium-api-tests -c Release --filter TestCategory=Contract --logger "console;verbosity=normal"
        # PowerShell does not stop for a native command that returns non-zero,
        # so without this the browser tests could fail every case and the job
        # would still be reported as a success.
        if ($LASTEXITCODE -ne 0) {
            throw "The Selenium contract tests failed against the $Label example, returning $LASTEXITCODE. Their output is above."
        }
    } catch {
        if ($example) { Write-Host ">>> $Label example output >>>"; Receive-Job $example | Out-Host; Write-Host "<<< $Label example output <<<" }
        throw
    } finally {
        if ($example) { Remove-Job -Force $example }
        Remove-Item env:_51DEGREES_EXAMPLE_HTTP_PORTS, env:_51DEGREES_EXAMPLE_URL, env:_51DEGREES_EXAMPLE_LANG `
            -ErrorAction SilentlyContinue
        foreach ($name in $ExampleEnv.Keys) {
            Remove-Item "env:$name" -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "::group::Selenium contract tests"
if (-not $SeleniumContract) {
    Write-Host "Another entry runs the Selenium contract tests for this pull request."
} elseif (-not $env:_51DEGREES_RESOURCE_KEY_BESPOKE) {
    Write-Host "::warning title=Selenium skipped::_51DEGREES_RESOURCE_KEY_BESPOKE is not set, so the Selenium contract tests were not run against the web examples."
} elseif ($IsLinux -and [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture -eq [System.Runtime.InteropServices.Architecture]::Arm64) {
    Write-Host "::warning title=Selenium skipped::Selenium contract skipped on linux-arm64, which has no selenium-manager build. It runs on x64 and macOS arm64."
} else {
    # Get the shared contract tests.
    if (-not (Test-Path selenium-api-tests)) {
        Write-Host "Cloning the Selenium contract tests from '$SeleniumTestsRef'"
        git clone --depth 1 --branch $SeleniumTestsRef https://github.com/51Degrees/selenium-api-tests.git
    }

    # Chrome's first start on a fresh runner is many times slower than the
    # starts after it, so it is made here rather than inside the first test.
    # The tests still run when it cannot be made.
    try {
        & "$PSScriptRoot/start-chrome-once.ps1"
    } catch {
        Write-Host "::warning title=Chrome not started::$_"
    }

    $contracts = @(
        @{
            Label = 'cloud'
            Module = 'web/getting-started.cloud'
            # The example reads its key as _51DEGREES_RESOURCE_KEY, and the
            # production cloud unless _51DEGREES_CLOUD_ENDPOINT names another.
            # The contract needs every property, so it is given the key that
            # carries them all.
            Port = 8099
            ExampleEnv = @{
                '_51DEGREES_RESOURCE_KEY' = $env:_51DEGREES_RESOURCE_KEY_BESPOKE
            }
        }
        # The on-premise example, against the TAC data file that
        # fetch-assets.ps1 links into the data repository. The Lite data file
        # has neither DeviceType nor the JavaScript properties the contract
        # tests need.
        @{
            Label = 'on-premise'
            Module = 'web/getting-started.onprem'
            Port = 8098
            ExampleEnv = @{
                '_51DEGREES_DD_PATH' = (Resolve-Path "$RepoName/cxx/data/device-detection/TAC-HashV41.hash").Path
            }
        }
    )
    # Each example is tested whether or not the one before it failed.
    foreach ($contract in $contracts) {
        try {
            Invoke-ContractTests @contract
        } catch {
            Write-Host "::error title=Selenium contract failed::$_"
            $failed += "Selenium contract tests, $($contract.Label) example"
        }
    }
}
Write-Host "::endgroup::"

if ($failed) {
    Write-Host "Failed: $($failed -join ', ')"
    exit 1
}
exit 0
