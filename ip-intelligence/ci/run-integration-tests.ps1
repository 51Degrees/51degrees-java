param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$OrgName,
    [Parameter(Mandatory)][hashtable]$Keys,
    [string]$IpIntelligenceUrl,
    [string]$Name,
    [string]$Version,
    [string]$Branch = "main", # this is actually the examples branch, but the name has to just be 'Branch' to be recognized by run-repo-script.ps1
    [string]$ExamplesRepo = "$RepoName-examples",
    [string]$ExamplesBranch = $Branch
)

# Skip integration tests if IpIntelligenceUrl is not provided
if (!$IpIntelligenceUrl) {
    Write-Output "Skipping integration tests - IpIntelligenceUrl not provided"
    exit 0
}

$RepoPath = [IO.Path]::Combine($pwd, $RepoName)
$mavenOpts = '--batch-mode', '--no-transfer-progress'

try {
    # The branch under test usually has no twin in the examples repo
    if (!(git ls-remote --heads "https://github.com/$OrgName/$ExamplesRepo" "refs/heads/$Branch")) {
        Write-Output "No '$Branch' branch in '$ExamplesRepo', using 'main'"
        $Branch = "main"
    }

    Write-Output "Cloning '$ExamplesRepo'"
    ./steps/clone-repo.ps1 -RepoName $ExamplesRepo -OrgName $OrgName -Branch $Branch
    & "./$ExamplesRepo/ci/fetch-assets.ps1" -IpIntelligenceUrl $IpIntelligenceUrl

    Write-Output "Entering ip-intelligence-java directory"
    Push-Location $RepoPath
    # If Version parameter is not set, get it from pom.xml
    if (!$Version) {
        $Version = mvn @mavenOpts org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression="project.version" -q -DforceStdout
    }

    Pop-Location

    Write-Output "Entering ip-intelligence-examples directory"
    Push-Location $ExamplesRepo


    Write-Output "Setting examples ip-intelligence package dependency to version '$Version'"
    mvn @mavenOpts versions:set-property -Dproperty="ip-intelligence.version" "-DnewVersion=$Version"

    Write-Output "Testing Examples"
    mvn @mavenOpts clean test "-DTestResourceKey=$($Keys.TestResourceKey)" "-DSuperResourceKey=$($Keys.TestResourceKey)" "-DLicenseKey=$($Keys.IPIntelligence)"

    Write-Output "Copying test results".
    # Copy the test results into the test-results folder
    Get-ChildItem -Path . -Directory -Depth 1 |
    Where-Object { Test-Path "$($_.FullName)\pom.xml" } |
    ForEach-Object {
        $targetDir = "$($_.FullName)\target\surefire-reports"
        $destDir = "..\$RepoName\test-results\integration"
        if(!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir }
        if(Test-Path $targetDir) {
            Get-ChildItem -Path $targetDir |
            ForEach-Object {
                Copy-Item -Path $_.FullName -Destination $destDir
            }
        }
    }
}

finally {

    Write-Output "Leaving '$ExamplesRepo'"
    Pop-Location

}

exit $LASTEXITCODE
