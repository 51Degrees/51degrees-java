# Tests of the workflows in .github/workflows that set resource and license
# keys.

Describe 'the workflows' {

    # The 51Did license key unseals only results made with the resource key
    # of its own account. Filled from the Bespoke key, the licensed redeem
    # comes back unreadable, so the 51Did resource key is taken from the
    # secret of its own name.
    It 'fill _51DEGREES_RESOURCE_KEY_51DID from the secret of the same name' {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        $lines = @(Get-ChildItem $workflows -Filter *.yml |
            Select-String -Pattern '^\s*_51DEGREES_RESOURCE_KEY_51DID\s*:' |
            ForEach-Object { "$($_.Filename):$($_.LineNumber): $($_.Line.Trim())" })
        $lines | Should -Not -BeNullOrEmpty
        $lines | Where-Object {
            $_ -notmatch ':\s*\$\{\{\s*secrets\._51DEGREES_RESOURCE_KEY_51DID\s*\}\}\s*$'
        } | Should -BeNullOrEmpty
    }

    # cloud-51did.yml runs the 51Did tests once for each resource key. The
    # tests redeem with _51DEGREES_LICENSE_KEY_51DID, which unseals only
    # results made with _51DEGREES_RESOURCE_KEY_51DID. So each pass gets only
    # the license whose suffix matches its resource key, and the Bespoke pass
    # gets none. The step's script runs here with mvn replaced by a function
    # that records the keys each pass hands it.
    Context 'cloud-51did.yml' {

        BeforeAll {
            $workflow = Join-Path $PSScriptRoot '../.github/workflows/cloud-51did.yml'
            $lines = @(Get-Content $workflow)
            $start = $lines | Select-String -Pattern '^(\s*)run: \|\s*$' |
                Select-Object -First 1
            $indent = $start.Matches[0].Groups[1].Value.Length
            $body = foreach ($line in $lines[$start.LineNumber..($lines.Count - 1)]) {
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

        It 'redeems with the 51Did license only in the pass with the 51Did resource key' {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add([pscustomobject]@{
                    Resource = $env:_51DEGREES_RESOURCE_KEY
                    License  = $env:_51DEGREES_LICENSE_KEY_51DID
                })
                $global:LASTEXITCODE = 0
            }
            & $script:Step 6> $null

            @($passes.Resource | Sort-Object) |
                Should -Be @('resource-51did', 'resource-bespoke')
            ($passes | Where-Object Resource -eq 'resource-51did').License |
                Should -Be 'license-51did'
            ($passes | Where-Object Resource -eq 'resource-bespoke').License |
                Should -BeNullOrEmpty
        }

        # A license key with no resource key of its own suffix means a secret
        # is missing. The licensed redeem would then never run, and the job
        # would still pass, so the step fails naming the missing secret.
        It 'fails naming <Resource> when <License> has a value and it is empty' -ForEach @(
            @{ Resource = '_51DEGREES_RESOURCE_KEY_51DID'; License = '_51DEGREES_LICENSE_KEY_51DID' }
            @{ Resource = '_51DEGREES_RESOURCE_KEY_BESPOKE'; License = '_51DEGREES_LICENSE_KEY_BESPOKE' }
        ) {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add($env:_51DEGREES_RESOURCE_KEY)
                $global:LASTEXITCODE = 0
            }
            Remove-Item "Env:$Resource"

            { & $script:Step 6> $null } | Should -Throw "*$Resource*"
            $passes | Should -BeNullOrEmpty
        }

        # A deployment without 51Did has neither secret, and the runner then
        # sets both names empty. The Bespoke pass must still run, with no
        # 51Did license and no failure.
        It 'runs the Bespoke pass alone when both 51Did keys are empty' {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add([pscustomobject]@{
                    Resource = $env:_51DEGREES_RESOURCE_KEY
                    License  = $env:_51DEGREES_LICENSE_KEY_51DID
                })
                $global:LASTEXITCODE = 0
            }
            $env:_51DEGREES_RESOURCE_KEY_51DID = ''
            $env:_51DEGREES_LICENSE_KEY_51DID = ''

            { & $script:Step 6> $null } | Should -Not -Throw
            @($passes.Resource) | Should -Be @('resource-bespoke')
            $passes[0].License | Should -BeNullOrEmpty
        }
    }
}
