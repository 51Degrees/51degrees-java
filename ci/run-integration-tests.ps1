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
$versionArgs = if ($Version) {
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

# The shared Selenium contract tests, run against the Device Detection cloud
# web example.
function Invoke-ContractTests {
    $port = 8099
    $example = $null
    Push-Location "$RepoName/examples/device-detection"
    try {
        mvn @MavenOptions -pl web/getting-started.cloud -am package -DskipTests @versionArgs
        if ($LASTEXITCODE -ne 0) { throw "Building the cloud web example failed." }
        $jar = (Get-ChildItem web/getting-started.cloud/target/*-jar-with-dependencies.jar | Select-Object -First 1).FullName
        $env:PORT = $port
        $env:TestCloudEndpoint = "https://cloud.51degrees.com/api/v4"
        $env:TestResourceKey = $TestResourceKey
        $example = java -jar $jar 2>&1 &
    } finally {
        Pop-Location
    }

    try {
        # SELENIUM_TESTS_REF picks a branch of the suite, so a change there
        # can be tried here before it is merged.
        $seleniumRef = if ($env:SELENIUM_TESTS_REF) { $env:SELENIUM_TESTS_REF } else { 'main' }
        if (-not (Test-Path selenium-api-tests)) {
            Write-Host "Cloning the Selenium contract tests from '$seleniumRef'"
            git clone --depth 1 --branch $seleniumRef https://github.com/51Degrees/selenium-api-tests.git
        }

        # Wait for the example to come up.
        curl -sS -o $(if ($IsWindows) { 'NUL' } else { '/dev/null' }) --retry 20 --retry-delay 3 --retry-max-time 120 --retry-connrefused "http://localhost:$port"
        if ($LASTEXITCODE -ne 0) { throw "The cloud web example did not start on port $port." }

        $env:CLOUD_ROOT_URL = "https://cloud.51degrees.com/"
        $env:PAID_RESOURCE_KEY = $TestResourceKey
        $env:EXAMPLE_URL = "http://localhost:$port"
        $env:EXAMPLE_LANG = 'java'
        dotnet test selenium-api-tests -c Release --filter TestCategory=Contract
        # PowerShell does not stop for a native command that returns non-zero,
        # so without this the browser tests could fail every case and the job
        # would still be reported as a success.
        if ($LASTEXITCODE -ne 0) {
            throw "The Selenium contract tests failed, returning $LASTEXITCODE. Their output is above."
        }
    } catch {
        if ($example) { Write-Host '>>> example app output >>>'; Receive-Job $example | Out-Host; Write-Host '<<< app output <<<' }
        throw
    } finally {
        if ($example) { Remove-Job -Force $example }
    }
}

Write-Host "::group::Selenium contract tests"
if (-not $TestResourceKey) {
    Write-Host "::warning title=Selenium skipped::No resource key, so the Selenium contract tests were not run against the cloud example."
} elseif ($IsLinux -and [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture -eq [System.Runtime.InteropServices.Architecture]::Arm64) {
    Write-Host "::warning title=Selenium skipped::Selenium contract skipped on linux-arm64, which has no selenium-manager build. It runs on x64 and macOS arm64."
} else {
    try {
        Invoke-ContractTests
    } catch {
        Write-Host "::error title=Selenium contract failed::$_"
        $failed += 'Selenium contract tests'
    }
}
Write-Host "::endgroup::"

if ($failed) {
    Write-Host "Failed: $($failed -join ', ')"
    exit 1
}
exit 0
