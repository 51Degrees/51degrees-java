# Tests of the workflows in .github/workflows that set resource and license
# keys.

Describe 'the workflows' {

    # Each name holds one thing, so a workflow never fills one name from the
    # secret or variable of another. The 51Did license key, for one, unseals
    # only results made with the resource key of its own account. The one
    # setting a workflow writes as it is, rather than from a secret, is that
    # warnings are errors.
    It 'fill every _51DEGREES_ name from the secret or variable of the same name' {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        $lines = @(Get-ChildItem $workflows -Filter *.yml |
            Select-String -Pattern '^\s*(_51DEGREES_[A-Z0-9_]+)\s*:' |
            ForEach-Object {
                [pscustomobject]@{
                    Name = $_.Matches[0].Groups[1].Value
                    Text = "$($_.Filename):$($_.LineNumber): $($_.Line.Trim())"
                }
            })
        $lines | Should -Not -BeNullOrEmpty
        $lines | Where-Object {
            $_.Text -notmatch (':\s*\$\{\{\s*(?:secrets|vars)\.' +
                [regex]::Escape($_.Name) + '\s*\}\}\s*$') -and
            $_.Text -cnotmatch ':\s*_51DEGREES_WARNINGS_AS_ERRORS:\s*''true''$'
        } | ForEach-Object Text | Should -BeNullOrEmpty
    }

    It 'make every warning an error in each workflow that runs Maven' {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        foreach ($file in Get-ChildItem $workflows -Filter *.yml) {
            $text = Get-Content -Raw $file.FullName
            if ($text -match '(?m)^\s*(run:\s*)?mvn\s') {
                $text | Should -MatchExactly (
                    '(?m)^env[:]\s*\n\s+_51DEGREES_WARNINGS_AS_ERRORS:\s*''true''') `
                    -Because "$($file.Name) runs Maven"
            }
        }
    }

    # The resource keys and the license keys are organization secrets, and
    # the staging cloud's address an organization variable, so each comes
    # from the secret or variable of its own name.
    It 'fill <Name> from the organization <Kind> of the same name' -ForEach @(
        @{ Name = '_51DEGREES_RESOURCE_KEY_51DID'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = '_51DEGREES_RESOURCE_KEY_BESPOKE'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = '_51DEGREES_RESOURCE_KEY_BASIC'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = '_51DEGREES_LICENSE_KEY_51DID'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = '_51DEGREES_LICENSE_KEY_BESPOKE'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = 'RESOURCE_KEY_BESPOKE_STAGING'; Kind = 'secret'; Context = 'secrets' }
        @{ Name = 'CLOUD_STAGING_ENDPOINT'; Kind = 'variable'; Context = 'vars' }
    ) {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        $lines = @(Get-ChildItem $workflows -Filter *.yml |
            Select-String -Pattern "^\s*$Name\s*:" |
            ForEach-Object { "$($_.Filename):$($_.LineNumber): $($_.Line.Trim())" })
        $lines | Should -Not -BeNullOrEmpty
        $lines | Where-Object {
            $_ -notmatch (':\s*\$\{\{\s*' + $Context + '\.' + $Name + '\s*\}\}\s*$')
        } | Should -BeNullOrEmpty
    }

    # Every resource key is an organization secret, so no workflow reads a
    # _51DEGREES_RESOURCE_KEY name as an organization variable.
    It 'read no _51DEGREES_RESOURCE_KEY name as an organization variable' {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        @(Get-ChildItem $workflows -Filter *.yml |
            Select-String -Pattern '\bvars\._51DEGREES_RESOURCE_KEY' |
            ForEach-Object { "$($_.Filename):$($_.LineNumber): $($_.Line.Trim())" }) |
            Should -BeNullOrEmpty
    }

    # cloud-51did.yml runs the live tests once. The tests read each key
    # under its own name, so the step sets no name of its own. The step's
    # script runs here with mvn replaced by a function that records what it
    # is handed.
    Context 'cloud-51did.yml' {

        BeforeAll {
            $workflow = Join-Path $PSScriptRoot '../.github/workflows/cloud-51did.yml'
            $script:Lines = @(Get-Content $workflow)
            $start = $script:Lines | Select-String -Pattern '^(\s*)run: \|\s*$' |
                Select-Object -First 1
            $indent = $start.Matches[0].Groups[1].Value.Length
            $body = foreach ($line in $script:Lines[$start.LineNumber..($script:Lines.Count - 1)]) {
                if ($line.Trim() -and $line -notmatch "^\s{$($indent + 1),}") { break }
                if ($line.Length -gt $indent + 2) { $line.Substring($indent + 2) } else { '' }
            }
            $script:Step = [scriptblock]::Create($body -join "`n")
        }

        BeforeEach {
            $script:Saved = @(Get-ChildItem env: |
                Where-Object { $_.Name -like '_51DEGREES_*' })
            $script:Saved | ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $env:_51DEGREES_RESOURCE_KEY_51DID = 'resource-51did'
            $env:_51DEGREES_RESOURCE_KEY_BESPOKE = 'resource-bespoke'
            $env:_51DEGREES_LICENSE_KEY_51DID = 'license-51did'
            $env:_51DEGREES_LICENSE_KEY_BESPOKE = 'license-bespoke'
        }

        AfterEach {
            Get-ChildItem env: | Where-Object { $_.Name -like '_51DEGREES_*' } |
                ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $script:Saved | ForEach-Object { Set-Item "Env:$($_.Name)" $_.Value }
        }

        It 'runs the tests once, with each key under its own name and no other' {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $names = @(Get-ChildItem env: |
                    Where-Object { $_.Name -like '_51DEGREES_*' } |
                    ForEach-Object { "$($_.Name)=$($_.Value)" } | Sort-Object)
                $runs.Add($names)
                $global:LASTEXITCODE = 0
            }
            & $script:Step 6> $null

            $runs.Count | Should -Be 1
            $runs[0] | Should -Be @(
                '_51DEGREES_LICENSE_KEY_51DID=license-51did'
                '_51DEGREES_LICENSE_KEY_BESPOKE=license-bespoke'
                '_51DEGREES_RESOURCE_KEY_51DID=resource-51did'
                '_51DEGREES_RESOURCE_KEY_BESPOKE=resource-bespoke')
        }

        # A license key with no resource key of its own account means an
        # organization secret is missing. The licensed tests would then
        # never run, and the job would still pass, so the step fails naming
        # the missing secret.
        It 'fails naming <Resource> when <License> has a value and it is empty' -ForEach @(
            @{ Resource = '_51DEGREES_RESOURCE_KEY_51DID'; License = '_51DEGREES_LICENSE_KEY_51DID' }
            @{ Resource = '_51DEGREES_RESOURCE_KEY_BESPOKE'; License = '_51DEGREES_LICENSE_KEY_BESPOKE' }
        ) {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $runs.Add('run')
                $global:LASTEXITCODE = 0
            }
            Remove-Item "Env:$Resource"

            { & $script:Step 6> $null } | Should -Throw "*$Resource*"
            $runs | Should -BeNullOrEmpty
        }

        # A deployment without 51Did has neither key, and the runner then
        # sets both names empty. The tests must still run with the Bespoke
        # key, with no failure.
        It 'runs the tests when both 51Did keys are empty' {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $runs.Add('run')
                $global:LASTEXITCODE = 0
            }
            $env:_51DEGREES_RESOURCE_KEY_51DID = ''
            $env:_51DEGREES_LICENSE_KEY_51DID = ''

            { & $script:Step 6> $null } | Should -Not -Throw
            $runs.Count | Should -Be 1
        }

        # A run with no resource key would skip every live test and still
        # pass, having called nothing.
        It 'fails when no resource key has a value' {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $runs.Add('run')
                $global:LASTEXITCODE = 0
            }
            Get-ChildItem env: | Where-Object { $_.Name -like '_51DEGREES_*' } |
                ForEach-Object { Remove-Item "Env:$($_.Name)" }

            { & $script:Step 6> $null } | Should -Throw '*resource key*'
            $runs | Should -BeNullOrEmpty
        }

        # The cloud tests call the service the organization variable names,
        # and the public service when it names none.
        It 'takes the cloud endpoint from the variable of the same name' {
            $script:Lines | Where-Object {
                $_ -match '^\s*_51DEGREES_CLOUD_ENDPOINT:\s*\$\{\{\s*vars\._51DEGREES_CLOUD_ENDPOINT\s*\}\}\s*$'
            } | Should -Not -BeNullOrEmpty
        }
    }

    # cloud-human-confidence.yml runs the Human Confidence live tests once,
    # against the staging cloud. Each test reads its own key and the endpoint
    # under their own names, so the step sets no name of its own. The step's
    # script runs here with mvn replaced by a function that records what it
    # is handed and writes the report Maven would write.
    Context 'cloud-human-confidence.yml' {

        BeforeAll {
            $workflow = Join-Path $PSScriptRoot '../.github/workflows/cloud-human-confidence.yml'
            $script:Lines = @(Get-Content $workflow)
            $start = $script:Lines | Select-String -Pattern '^(\s*)run: \|\s*$' |
                Select-Object -First 1
            $indent = $start.Matches[0].Groups[1].Value.Length
            $body = foreach ($line in $script:Lines[$start.LineNumber..($script:Lines.Count - 1)]) {
                if ($line.Trim() -and $line -notmatch "^\s{$($indent + 1),}") { break }
                if ($line.Length -gt $indent + 2) { $line.Substring($indent + 2) } else { '' }
            }
            $script:Step = [scriptblock]::Create($body -join "`n")
            $script:Report = 'web/human-confidence/target/surefire-reports/' +
                'TEST-fiftyone.devicedetection.examples.web.HumanConfidenceLiveTest.xml'
            # The names the step and the tests read.
            $script:Read = '^(_51DEGREES_.*|CLOUD_STAGING_ENDPOINT|RESOURCE_KEY_BESPOKE_STAGING)$'

            # Writes the report of a run of the given number of tests, of
            # which the given number were skipped.
            function script:Write-Report([int]$Tests, [int]$Skipped) {
                New-Item -ItemType Directory -Force (Split-Path $script:Report) | Out-Null
                $cases = (1..$Tests | ForEach-Object {
                    if ($_ -le $Skipped) {
                        "<testcase name=`"t$_`"><skipped message=`"skipped`"/></testcase>"
                    } else {
                        "<testcase name=`"t$_`"/>"
                    }
                }) -join ''
                Set-Content $script:Report ("<testsuite tests=`"$Tests`" " +
                    "skipped=`"$Skipped`">$cases</testsuite>")
            }
        }

        BeforeEach {
            $script:Saved = @(Get-ChildItem env: |
                Where-Object { $_.Name -match $script:Read })
            $script:Saved | ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $env:RESOURCE_KEY_BESPOKE_STAGING = 'resource-bespoke-staging'
            $env:_51DEGREES_RESOURCE_KEY_BASIC = 'resource-basic'
            $env:CLOUD_STAGING_ENDPOINT = 'endpoint-staging'
            Push-Location TestDrive:
        }

        AfterEach {
            Pop-Location
            Remove-Item TestDrive:/web -Recurse -Force -ErrorAction SilentlyContinue
            Get-ChildItem env: | Where-Object { $_.Name -match $script:Read } |
                ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $script:Saved | ForEach-Object { Set-Item "Env:$($_.Name)" $_.Value }
        }

        It 'runs the tests once, with each name under its own name and no other' {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $names = @(Get-ChildItem env: |
                    Where-Object { $_.Name -match $script:Read } |
                    ForEach-Object { "$($_.Name)=$($_.Value)" })
                $runs.Add(@{ Names = $names; Arguments = "$args" })
                Write-Report -Tests 2 -Skipped 0
                $global:LASTEXITCODE = 0
            }
            { & $script:Step 6> $null } | Should -Not -Throw

            $runs.Count | Should -Be 1
            @($runs[0].Names | Sort-Object) | Should -Be @(@(
                'CLOUD_STAGING_ENDPOINT=endpoint-staging'
                'RESOURCE_KEY_BESPOKE_STAGING=resource-bespoke-staging'
                '_51DEGREES_RESOURCE_KEY_BASIC=resource-basic') | Sort-Object)
            $runs[0].Arguments | Should -BeLike '*-Dtest=HumanConfidenceLiveTest*'
            $runs[0].Arguments | Should -Not -BeLike '*humanConfidenceSource*'
        }

        # Each test needs its own key and the endpoint, so a missing one means
        # a test that would skip, and the job would still pass having proved
        # nothing.
        It 'fails naming <Name> when it is empty' -ForEach @(
            @{ Name = 'RESOURCE_KEY_BESPOKE_STAGING' }
            @{ Name = '_51DEGREES_RESOURCE_KEY_BASIC' }
            @{ Name = 'CLOUD_STAGING_ENDPOINT' }
        ) {
            $runs = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $runs.Add('run')
                $global:LASTEXITCODE = 0
            }
            Remove-Item "Env:$Name"

            { & $script:Step 6> $null } | Should -Throw "*$Name*"
            $runs | Should -BeNullOrEmpty
        }

        It 'names every missing name at once' {
            function mvn { $global:LASTEXITCODE = 0 }
            Remove-Item Env:RESOURCE_KEY_BESPOKE_STAGING, Env:CLOUD_STAGING_ENDPOINT

            { & $script:Step 6> $null } |
                Should -Throw '*RESOURCE_KEY_BESPOKE_STAGING, CLOUD_STAGING_ENDPOINT*'
        }

        It 'fails when a test was skipped' {
            function mvn {
                Write-Report -Tests 2 -Skipped 1
                $global:LASTEXITCODE = 0
            }

            { & $script:Step 6> $null } | Should -Throw '*skipped*'
        }

        It 'fails when the tests did not run' {
            function mvn {
                $global:LASTEXITCODE = 0
            }

            { & $script:Step 6> $null } | Should -Throw '*did not run*'
        }

        It 'fails when Maven fails' {
            function mvn {
                Write-Report -Tests 2 -Skipped 0
                $global:LASTEXITCODE = 1
            }

            { & $script:Step 6> $null } | Should -Throw
        }

        It 'takes the staging cloud from the variable of the same name' {
            $script:Lines | Where-Object {
                $_ -match '^\s*CLOUD_STAGING_ENDPOINT:\s*\$\{\{\s*vars\.CLOUD_STAGING_ENDPOINT\s*\}\}\s*$'
            } | Should -Not -BeNullOrEmpty
        }
    }
}
