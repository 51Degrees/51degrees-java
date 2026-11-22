# Tests of the workflows in .github/workflows that set resource and license
# keys.

Describe 'the workflows' {

    # Each name holds one thing, so a workflow never fills one name from the
    # secret or variable of another. The 51Did license key, for one, unseals
    # only results made with the resource key of its own account.
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
                [regex]::Escape($_.Name) + '\s*\}\}\s*$')
        } | ForEach-Object Text | Should -BeNullOrEmpty
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

        # A license key with no resource key of its own account means a
        # secret is missing. The licensed tests would then never run, and
        # the job would still pass, so the step fails naming the missing
        # secret.
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

        # A deployment without 51Did has neither secret, and the runner then
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
}
