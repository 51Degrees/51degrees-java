param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    [string]$TestResourceKey,
    [string]$DeviceDetection
)
$PSNativeCommandUseErrorActionPreference = $false

. "$PSScriptRoot/packages.ps1"

# One run of every test in the reactor that build-project.ps1 built, the
# examples included. SuperResourceKey is read by the Location and Device
# Detection examples tests, and LicenseKey by the Device Detection data file
# update example. The IP Intelligence update example's test is ignored, so it
# does not need a key of its own.
Write-Host "Entering '$RepoName'"
Push-Location $RepoName
try {
    Write-Host "Testing '$Name'"
    mvn surefire:test @MavenOptions -Pdev -DfailIfNoTests=false `
        "-DTestResourceKey=$TestResourceKey" `
        "-DSuperResourceKey=$TestResourceKey" `
        "-DLicenseKey=$DeviceDetection"
    # Kept here because the commands below run commands of their own, which
    # would replace Maven's exit code.
    $MavenExitCode = $LASTEXITCODE
} finally {
    Write-Host "Leaving '$RepoName'"
    Pop-Location
}

& "$PSScriptRoot/copy-test-results.ps1" -Path $RepoName -Destination "$RepoName/test-results/unit" -Exclude '*ExampleTests*'

# A forked JVM that dies leaves why it died only on the runner, so common-ci
# logs it. The collector is only in branches of common-ci that have it, so it
# is called only when present. It does not change the outcome.
$Collector = [IO.Path]::Combine($pwd, "java", "collect-jvm-crash-evidence.ps1")
if ($MavenExitCode -ne 0 -and (Test-Path $Collector) -eq $true) {
    & $Collector -RepoName $RepoName
}

exit $MavenExitCode
