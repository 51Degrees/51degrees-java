# Pester 5 tests of build-package.ps1. common-ci's build script is a stub that
# writes down what it was given, and the release plan is mocked, so each test
# reads only how the build is called.

BeforeAll {
    # common-ci runs the build from its own root, with this repository checked
    # out inside it. So the scripts are copied into a folder laid out the same
    # way, with a stub for common-ci's release plan and for its build script.
    $Root = Join-Path ([IO.Path]::GetTempPath()) "build-package-$PID"
    $null = New-Item -ItemType Directory -Force `
        (Join-Path $Root 'steps'), (Join-Path $Root 'java'),
        (Join-Path $Root 'repo'), (Join-Path $Root 'package'),
        (Join-Path $Root 'package-files')
    Copy-Item -Recurse $PSScriptRoot (Join-Path $Root 'repo' 'ci')
    $Script = Join-Path $Root 'repo' 'ci' 'build-package.ps1'
    $Given = Join-Path $Root 'given.json'
    Set-Content (Join-Path $Root 'steps' 'release-plan.ps1') `
        'function Get-ReleasePlan([string]$RepoPath) { }'

    # The stub takes only the parameters common-ci's script takes, and binds
    # no others, so a parameter the build should not give stops the test.
    Set-Content (Join-Path $Root 'java' 'build-package.ps1') @'
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [string]$Name,
    [string[]]$ExtraArgs,
    [string[]]$VersionPaths = @('.')
)
$PSBoundParameters | ConvertTo-Json | Set-Content (Join-Path $PWD 'given.json')
$global:LASTEXITCODE = 0
'@

    # The build ends by reading the publishing bundle common-ci's script made.
    $entry = Join-Path $Root 'entry.txt'
    Set-Content $entry 'entry'
    Compress-Archive -Path $entry `
        -DestinationPath (Join-Path $Root 'package' 'central-bundle.zip')

    # Pester mocks only a command it can find, so the functions the build
    # takes from release.ps1 are defined here as well.
    function Get-BuildPlan([string]$RepoPath) { }
    function Get-BuildVersions([string]$RepoPath, $Plan) { }
    function Set-PomVersions([string]$RepoPath, $Versions) { }

    function Invoke-Build {
        Push-Location $Root
        try {
            # The script ends with exit, so it runs as a command of its own.
            & $Script -RepoName 'repo' -Version '0' -Name 'Windows_Java_8' 6>$null
        } finally {
            Pop-Location
        }
    }
}

AfterAll {
    Remove-Item -Recurse -Force $Root
}

Describe 'build-package.ps1' {
    BeforeEach {
        Remove-Item $Given -ErrorAction SilentlyContinue
        Mock Get-BuildPlan {
            [pscustomobject]@{
                Name = 'pipeline.core'; Path = 'pipeline/pipeline.core'
                Version = '4.5.131'; Previous = $null; Reason = 'changed'
            }
        }
        Mock Get-BuildVersions { @{ 'pipeline.core' = '4.5.131' } }
        Mock Set-PomVersions { }
    }

    It 'takes no keys, because the signing reads its keys from the environment' {
        (Get-Command $Script).Parameters.ContainsKey('Keys') | Should -BeFalse
    }

    It 'gives the shared build the modules of the plan and nothing to sign with' {
        Invoke-Build
        $Given | Should -Exist
        $given = Get-Content -Raw $Given | ConvertFrom-Json -AsHashtable
        $given.Keys | Sort-Object | Should -Be @(
            'ExtraArgs', 'Name', 'RepoName', 'Version', 'VersionPaths')
        $given.RepoName | Should -Be 'repo'
        $given.Name | Should -Be 'Windows_Java_8'
        $given.ExtraArgs | Should -Be @(
            '-DskipNativeBuild=true', '-pl', 'pipeline/pipeline.core')
        $given.VersionPaths | Should -BeNullOrEmpty
    }
}
