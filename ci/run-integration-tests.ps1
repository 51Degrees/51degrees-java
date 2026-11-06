param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    # Set by the publish workflow to the version it built and installed. Not
    # set when a pull request is tested, which builds the reactor itself.
    [string]$Version,
    [string]$TestResourceKey,
    [string]$DeviceDetection,
    [string]$IpIntelligence,
    [string]$IpIntelligenceUrl
)
$ErrorActionPreference = "Stop"
# Each part's exit code is checked below, so one failing part does not stop
# the others from running.
$PSNativeCommandUseErrorActionPreference = $false

. "$PSScriptRoot/packages.ps1"

$results = "$RepoName/test-results/integration"
$failed = @()

# The examples take the libraries from Maven Central unless told otherwise.
# Pointing them at the version the publish workflow installed tests the
# packages as a project of your own would use them. For a pull request the
# dev profile points them at the reactor build-project.ps1 installed.
[string[]]$versionArgs = if ($Version) {
    "-Ddevice-detection.version=$Version", "-Dip-intelligence.version=$Version"
} else {
    , '-Pdev'
}

# The Pipeline API developer examples, as the pipeline repository ran them.
Write-Host "::group::Pipeline developer examples"
Push-Location "$RepoName/pipeline"
try {
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
    $examples = @(
        @{ Path = 'examples/device-detection'; LicenseKey = $DeviceDetection; Run = $true }
        # The IP Intelligence examples need the Enterprise data file, which is
        # only fetched when its URL is given.
        @{ Path = 'examples/ip-intelligence'; LicenseKey = $IpIntelligence; Run = [bool]$IpIntelligenceUrl }
    )
    foreach ($example in $examples) {
        if (-not $example.Run) {
            Write-Host "::warning title=Examples skipped::Skipping '$($example.Path)', as IpIntelligenceUrl was not provided."
            continue
        }
        Write-Host "::group::$($example.Path) against $Version"
        Push-Location "$RepoName/$($example.Path)"
        try {
            mvn clean test @MavenOptions @versionArgs `
                "-DTestResourceKey=$TestResourceKey" `
                "-DSuperResourceKey=$TestResourceKey" `
                "-DLicenseKey=$($example.LicenseKey)"
            if ($LASTEXITCODE -ne 0) { $failed += $example.Path }
        } finally {
            Pop-Location
        }
        & "$PSScriptRoot/copy-test-results.ps1" -Path "$RepoName/$($example.Path)" -Destination $results
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
        $env:PORT = $Port
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

        $env:EXAMPLE_URL = "http://localhost:$Port"
        $env:EXAMPLE_LANG = 'java'
        dotnet test selenium-api-tests -c Release --filter TestCategory=Contract
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
        Remove-Item env:PORT, env:EXAMPLE_URL -ErrorAction SilentlyContinue
        foreach ($name in $ExampleEnv.Keys) {
            Remove-Item "env:$name" -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "::group::Selenium contract tests"
if (-not $TestResourceKey) {
    Write-Host "::warning title=Selenium skipped::No resource key, so the Selenium contract tests were not run against the web examples."
} elseif ($IsLinux -and [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture -eq [System.Runtime.InteropServices.Architecture]::Arm64) {
    Write-Host "::warning title=Selenium skipped::Selenium contract skipped on linux-arm64, which has no selenium-manager build. It runs on x64 and macOS arm64."
} else {
    # Get the shared contract tests. SELENIUM_TESTS_REF picks a branch of the
    # suite, so a change there can be tried here before it is merged.
    $seleniumRef = if ($env:SELENIUM_TESTS_REF) { $env:SELENIUM_TESTS_REF } else { 'main' }
    if (-not (Test-Path selenium-api-tests)) {
        Write-Host "Cloning the Selenium contract tests from '$seleniumRef'"
        git clone --depth 1 --branch $seleniumRef https://github.com/51Degrees/selenium-api-tests.git
    }
    $env:CLOUD_ROOT_URL = "https://cloud.51degrees.com/"
    $env:PAID_RESOURCE_KEY = $TestResourceKey

    $contracts = @(
        @{
            Label = 'cloud'
            Module = 'web/getting-started.cloud'
            Port = 8099
            ExampleEnv = @{
                TestCloudEndpoint = "https://cloud.51degrees.com/api/v4"
                TestResourceKey = $TestResourceKey
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
                '51DEGREES_DD_PATH' = (Resolve-Path "$RepoName/cxx/data/device-detection/TAC-HashV41.hash").Path
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
