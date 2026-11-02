param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Name,
    # Set by the publish workflow to the version it built and installed. Not
    # set when a pull request is tested, which builds the reactor itself.
    [string]$Version,
    [string]$IpIntelligenceUrl
)
$ErrorActionPreference = "Stop"
# Each product's exit code is checked below, so one failing product does not
# stop the others from being measured.
$PSNativeCommandUseErrorActionPreference = $false

. "$PSScriptRoot/packages.ps1"

# The performance figures of the three products that measure any go in one
# results file, because common-ci reads one file per configuration. Device
# Detection and IP Intelligence measure the same things under the same names,
# so their metric names are prefixed with the product.
$summary = @{ HigherIsBetter = [ordered]@{}; LowerIsBetter = [ordered]@{} }
$results = "$RepoName/test-results/performance"
$failed = @()

[string[]]$versionArgs = if ($Version) {
    "-Ddevice-detection.version=$Version", "-Dip-intelligence.version=$Version"
} else {
    , '-Pdev'
}

# Pipeline API. The test writes its figures as JSON itself.
Write-Host "::group::Pipeline API"
$pipelineResults = Join-Path $PWD "pipeline-performance-$Name.json"
Remove-Item -Path $pipelineResults -Force -ErrorAction SilentlyContinue
Push-Location "$RepoName/pipeline"
try {
    mvn test @MavenOptions -DfailIfNoTests=false "-Dsurefire.failIfNoSpecifiedTests=false" `
        "-Dtest=*ShareUsageOverheadTests*" "-Dfiftyone.performance.json=$pipelineResults"
    if ($LASTEXITCODE -ne 0) { $failed += 'Pipeline API' }
} finally {
    Pop-Location
}
& "$PSScriptRoot/copy-test-results.ps1" -Path "$RepoName/pipeline" -Destination $results -Filter '*ShareUsageOverheadTests*'
if (Test-Path $pipelineResults) {
    $figures = Get-Content -Raw $pipelineResults | ConvertFrom-Json -AsHashtable
    foreach ($section in 'HigherIsBetter', 'LowerIsBetter') {
        if ($figures[$section]) {
            foreach ($metric in $figures[$section].Keys) {
                $summary[$section][$metric] = $figures[$section][$metric]
            }
        }
    }
} else {
    Write-Warning "The Pipeline API performance test wrote no results"
    $failed += 'Pipeline API results'
}
Write-Host "::endgroup::"

# Reads the figures the Device Detection and IP Intelligence benchmark examples
# print. They do not write JSON yet, so the output is read as the separate
# repositories read it.
function Read-Benchmark {
    param (
        [Parameter(Mandatory)][string]$OutputFile,
        [Parameter(Mandatory)][string]$ProfileHeader,
        [Parameter(Mandatory)][string]$ProfileName
    )
    $profiles = @{}
    $currentProfile = $null
    $currentThreadNum = 1
    Get-Content $OutputFile | ForEach-Object {
        # If the line matches a profile header, start a new profile named after
        # the values it captures.
        if ($_ -match $ProfileHeader) {
            $key = 'MaxPerformance-' + ((1..($Matches.Count - 1) | ForEach-Object { $Matches[$_] }) -join '-')
            $currentProfile = $profiles[$key] = @{ "Threads" = @{} }
            $currentThreadNum = 1
        }
        # If the line matches a thread data line, add it to the current profile.
        elseif ($_ -match "Thread:  ([\d,]+) detections, elapsed ([\d.]+) seconds, ([\d,]+) Detections per second") {
            $currentProfile["Threads"]["Thread_$currentThreadNum"] = @{
                "Detections" = [int]($Matches[1].Replace(",", ""))
                "ElapsedSeconds" = [double]$Matches[2]
                "DetectionsPerSecond" = [int]($Matches[3].Replace(",", ""))
            }
            $currentThreadNum++
        }
        # If the line matches an overall data line, add it to the current profile.
        elseif ($_ -match "Overall: ([\d,]+) detections, Average millisecs per detection: ([\d.]+), Detections per second: ([\d,]+)") {
            $currentProfile["Overall"] = @{
                "Detections" = [int]($Matches[1].Replace(",", ""))
                "AvgMillisecsPerDetection" = [double]$Matches[2]
                "DetectionsPerSecond" = [int]($Matches[3].Replace(",", ""))
            }
        }
    }
    return $profiles[$ProfileName]
}

$benchmarks = @(
    @{
        Product = 'DeviceDetection'
        Path = 'examples/device-detection'
        Output = 'console/target/surefire-reports/fiftyone.devicedetection.examples.console.PerformanceBenchmarkTest-output.txt'
        ProfileHeader = "MaxPerformance AllProperties: (true|false), performanceGraph: (true|false), predictiveGraph (true|false)"
        ProfileName = 'MaxPerformance-false-true-false'
        Run = $true
    }
    @{
        Product = 'IpIntelligence'
        Path = 'examples/ip-intelligence'
        Output = 'console/target/surefire-reports/fiftyone.ipintelligence.examples.console.PerformanceBenchmarkTest-output.txt'
        ProfileHeader = "MaxPerformance AllProperties: (true|false)"
        ProfileName = 'MaxPerformance-false'
        # Measured with the Enterprise data file, which is only fetched when
        # its URL is given.
        Run = [bool]$IpIntelligenceUrl
    }
)

foreach ($benchmark in $benchmarks) {
    if (-not $benchmark.Run) {
        Write-Host "::warning title=Performance skipped::Skipping the $($benchmark.Product) benchmark, as IpIntelligenceUrl was not provided."
        continue
    }
    Write-Host "::group::$($benchmark.Product)"
    Push-Location "$RepoName/$($benchmark.Path)"
    try {
        mvn clean test @MavenOptions @versionArgs -DfailIfNoTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=*Performance*"
        if ($LASTEXITCODE -ne 0) { $failed += $benchmark.Product }
    } finally {
        Pop-Location
    }
    & "$PSScriptRoot/copy-test-results.ps1" -Path "$RepoName/$($benchmark.Path)" -Destination $results -Filter '*Performance*'

    $outputFile = "$RepoName/$($benchmark.Path)/$($benchmark.Output)"
    $overall = if (Test-Path $outputFile) {
        (Read-Benchmark -OutputFile $outputFile -ProfileHeader $benchmark.ProfileHeader -ProfileName $benchmark.ProfileName).Overall
    }
    if ($overall) {
        $summary.HigherIsBetter["$($benchmark.Product)_DetectionsPerSecond"] = $overall.DetectionsPerSecond
        $summary.LowerIsBetter["$($benchmark.Product)_AvgMillisecsPerDetection"] = $overall.AvgMillisecsPerDetection
    } else {
        Write-Warning "No $($benchmark.ProfileName) figures found in '$outputFile'"
        $failed += "$($benchmark.Product) results"
    }
    Write-Host "::endgroup::"
}

if ($failed) {
    # Stop here, so a test failure is not reported as a missing results file.
    Write-Warning "Performance tests failed, so the results are not published: $($failed -join ', ')"
    exit 1
}

$summaryFile = Join-Path $PWD "performance-summary-$Name.json"
$summary | ConvertTo-Json -Depth 5 | Set-Content -Path $summaryFile
& ./steps/publish-performance-results.ps1 -SourceFile $summaryFile -Name $Name -RepoName $RepoName
