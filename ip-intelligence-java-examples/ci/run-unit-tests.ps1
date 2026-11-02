param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    [hashtable]$Keys
)

$RepoPath = [IO.Path]::Combine($pwd, $RepoName)

Write-Output "Entering '$RepoPath'"
Push-Location $RepoPath

try {

    Write-Output "Testing '$Name'"
    mvn clean test -f pom.xml -DXmx2048m --no-transfer-progress -DfailIfNoTests=false "-Dhttps.protocols=TLSv1.2" "-DTestResourceKey=$($Keys.TestResourceKey)" "-DLicenseKey=$($Keys.IpIntelligence)"
    # Kept here because the crash evidence collector below runs commands of
    # its own, which would replace Maven's exit code.
    $MavenExitCode = $LASTEXITCODE

    # Copy the test results into the test-results folder
    Get-ChildItem -Path . -Directory -Depth 1 |
    Where-Object { Test-Path "$($_.FullName)\pom.xml" } |
    ForEach-Object {
        $targetDir = "$($_.FullName)\target\surefire-reports"
        $destDir = ".\test-results\unit"
        if(!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir }
        if(Test-Path $targetDir) {
            Get-ChildItem -Path $targetDir |
            Where-Object { $_.Name -notlike "*ExampleTests*"} |
            ForEach-Object {
                Copy-Item -Path $_.FullName -Destination $destDir
            }
        }
    }
} finally {
    Write-Output "Leaving '$RepoPath'"
    Pop-Location
}

# A forked JVM that dies leaves why it died only on the runner, so common-ci
# logs it. The collector is only in branches of common-ci that have it, so it
# is called only when present. It does not change the outcome.
$Collector = [IO.Path]::Combine($pwd, "java", "collect-jvm-crash-evidence.ps1")
if ($MavenExitCode -ne 0 -and (Test-Path $Collector) -eq $true) {
    & $Collector -RepoName $RepoName
}

exit $MavenExitCode
