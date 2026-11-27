# Pester 5 tests of publish-package.ps1. The upload script is a stub, and git,
# gh and the release plan are mocked, so each test reads only what the publish
# would tag, push and release.

BeforeAll {
    # common-ci runs the publish from its own root, with this repository
    # checked out inside it. release.ps1 finds common-ci's
    # steps/release-plan.ps1 from where it is, and the publish runs
    # ./java/publish-package-maven.ps1 from the folder it is run in. So the
    # scripts are copied into a folder laid out the same way, with a stub for
    # each of the two.
    $Root = Join-Path ([IO.Path]::GetTempPath()) "publish-package-$PID"
    $null = New-Item -ItemType Directory -Force `
        (Join-Path $Root 'steps'), (Join-Path $Root 'java'),
        (Join-Path $Root 'repo')
    Copy-Item -Recurse $PSScriptRoot (Join-Path $Root 'repo' 'ci')
    $Script = Join-Path $Root 'repo' 'ci' 'publish-package.ps1'
    Set-Content (Join-Path $Root 'steps' 'release-plan.ps1') `
        'function Get-ReleasePlan([string]$RepoPath) { }'
    # The stub takes only the parameters common-ci's script takes, and binds
    # no others, so a parameter the publish should not give stops the test.
    Set-Content (Join-Path $Root 'java' 'publish-package-maven.ps1') `
        '[CmdletBinding()] param($RepoName, $Version, [bool]$DryRun)'

    # Pester mocks only a command it can find, so the plan is defined here
    # as well as in the stub.
    function Get-ReleasePlan([string]$RepoPath) { }

    function Invoke-Publish([bool]$DryRun = $false) {
        Push-Location $Root
        try {
            & $Script -RepoName 'repo' -OrgName 'org' -Version '0' `
                -DryRun $DryRun
        } finally {
            Pop-Location
        }
    }
}

AfterAll {
    Remove-Item -Recurse -Force $Root
}

Describe 'publish-package.ps1' {
    BeforeEach {
        $global:SkipUpdateTag = $false
        Mock git { }
        Mock gh { }
        Mock Get-ReleasePlan {
            [pscustomobject]@{
                Name = 'pipeline.core'; Version = '4.5.131'
                Previous = $null; Reason = 'changed'
            }
            [pscustomobject]@{
                Name = 'pipeline.engines'; Version = '4.5.131'
                Previous = $null; Reason = 'depends on pipeline.core'
            }
        }
    }

    It 'takes no keys, because the upload reads its token from the environment' {
        (Get-Command $Script).Parameters.ContainsKey('Keys') | Should -BeFalse
    }

    It 'pushes every tag in one atomic push' {
        Invoke-Publish
        Should -Invoke git -Times 1 -Exactly -ParameterFilter {
            $args -contains 'push'
        }
        Should -Invoke git -Times 1 -Exactly -ParameterFilter {
            $args -contains 'push' -and $args -contains '--atomic' -and
                $args -contains 'pipeline.core/4.5.131' -and
                $args -contains 'pipeline.engines/4.5.131'
        }
    }

    It 'makes one release for each tag' {
        Invoke-Publish
        Should -Invoke gh -Times 2 -Exactly
        $global:SkipUpdateTag | Should -BeTrue
    }

    It 'makes no release when the push fails' {
        Mock git { if ($args -contains 'push') { throw 'push refused' } }
        { Invoke-Publish } | Should -Throw '*push refused*'
        Should -Invoke gh -Times 0 -Exactly
    }

    It 'pushes nothing and makes no release in a dry run' {
        Invoke-Publish -DryRun $true
        Should -Invoke git -Times 2 -Exactly -ParameterFilter {
            $args -contains 'tag'
        }
        Should -Invoke git -Times 0 -Exactly -ParameterFilter {
            $args -contains 'push'
        }
        Should -Invoke gh -Times 0 -Exactly
        $global:SkipUpdateTag | Should -BeTrue
    }

    It 'pushes nothing when the plan is empty' {
        Mock Get-ReleasePlan { }
        Invoke-Publish
        Should -Invoke git -Times 0 -Exactly -ParameterFilter {
            $args -contains 'push'
        }
        Should -Invoke gh -Times 0 -Exactly
    }
}
