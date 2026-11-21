# Tests for ci/run-fresh-clone-example.ps1.
#
# Each case runs the script in a new pwsh process, with -File as a person
# would type it at a shell, or with -Command as the workflow's pwsh step does.
# The process runs in a folder that holds an empty jar where the script looks
# for one, and a stand-in for java comes first on the PATH. The stand-in
# prints results, and also the line an example logs when it downloads its data
# file when it is given the example name Downloads.

BeforeDiscovery {
    # Each call the workflow makes, with the job it is in, so that a change to
    # the workflow's call is tested as well.
    $workflow = Join-Path $PSScriptRoot '../.github/workflows/examples-fresh-clone.yml'
    $lines = @(Get-Content -LiteralPath $workflow)
    $script:WorkflowCalls = @()
    for ($i = 0; $i -lt $lines.Count - 1; $i++) {
        if ($lines[$i].Trim() -ne 'run: >-' -or
            $lines[$i + 1] -notmatch 'run-fresh-clone-example\.ps1') {
            continue
        }
        $parts = @()
        for ($j = $i + 1; $j -lt $lines.Count -and $lines[$j].Trim() -ne ''; $j++) {
            $parts += $lines[$j].Trim()
        }
        $job = $null
        for ($k = $i; $k -ge 0 -and $null -eq $job; $k--) {
            if ($lines[$k] -match '^  ([A-Za-z0-9_-]+):\s*$') {
                $job = $Matches[1]
            }
        }
        $script:WorkflowCalls += @{ Job = $job; Command = $parts -join ' ' }
    }
}

BeforeAll {
    $script:Script = Join-Path $PSScriptRoot 'run-fresh-clone-example.ps1'
    $script:Pwsh = (Get-Process -Id $PID).Path

    $script:Root = Join-Path $TestDrive 'repository'
    $target = Join-Path $script:Root 'examples/device-detection/console/target'
    New-Item -ItemType Directory $target -Force | Out-Null
    New-Item -ItemType File (Join-Path $target 'example-jar-with-dependencies.jar') | Out-Null

    $script:Bin = Join-Path $TestDrive 'bin'
    New-Item -ItemType Directory $script:Bin | Out-Null
    $missing = "'device-detection/51Degrees-LiteV4.1.hash' is missing, so it is being downloaded by running the script"
    if ($IsWindows) {
        # The arguments are -cp, the jar and the example.
        [IO.File]::WriteAllText((Join-Path $script:Bin 'java.cmd'),
            "@echo off`r`nif ""%~3""==""Downloads"" echo $missing`r`necho Results:`r`nexit /b 0`r`n")
    } else {
        $java = Join-Path $script:Bin 'java'
        [IO.File]::WriteAllText($java,
            "#!/bin/sh`nif [ ""`$3"" = Downloads ]; then echo ""$missing""; fi`necho Results:`nexit 0`n")
        chmod +x $java
    }

    # Runs pwsh with the arguments in the repository folder, with the stand-in
    # for java first on the PATH, and returns the exit code and the output.
    function Invoke-Pwsh([string[]]$Arguments) {
        $path = $env:PATH
        Push-Location $script:Root
        try {
            $env:PATH = $script:Bin + [IO.Path]::PathSeparator + $path
            $output = & $script:Pwsh -NoProfile -NonInteractive @Arguments 2>&1 |
                ForEach-Object { "$_" }
            [pscustomobject]@{ Code = $LASTEXITCODE; Text = $output -join "`n" }
        } finally {
            $env:PATH = $path
            Pop-Location
        }
    }

    function Invoke-File([string]$Example, [string[]]$Flag) {
        Invoke-Pwsh (@('-File', $script:Script, '-Product', 'device-detection',
                '-Example', $Example) + $Flag)
    }
}

Describe 'run-fresh-clone-example.ps1 called with pwsh -File' {

    It 'passes when the example downloads, given <Flag>' -ForEach @(
        @{ Flag = @('-ExpectDownload') }
        @{ Flag = @('-ExpectDownload:$true') }
    ) {
        $run = Invoke-File 'Downloads' $Flag
        $run.Text | Should -Match 'Results:'
        $run.Text | Should -Not -Match '::error::'
        $run.Code | Should -Be 0
    }

    It 'fails when the example does not download, given <Flag>' -ForEach @(
        @{ Flag = @('-ExpectDownload') }
        @{ Flag = @('-ExpectDownload:$true') }
    ) {
        $run = Invoke-File 'FromCache' $Flag
        $run.Text | Should -Match '::error::The example did not download its free data file\.'
        $run.Code | Should -Be 1
    }

    It 'passes when the example does not download, given <Name>' -ForEach @(
        @{ Name = 'no flag'; Flag = @() }
        @{ Name = '-ExpectDownload:$false'; Flag = @('-ExpectDownload:$false') }
    ) {
        $run = Invoke-File 'FromCache' $Flag
        $run.Text | Should -Match 'Results:'
        $run.Text | Should -Not -Match '::error::'
        $run.Code | Should -Be 0
    }

    It 'fails when the example downloads, given <Name>' -ForEach @(
        @{ Name = 'no flag'; Flag = @() }
        @{ Name = '-ExpectDownload:$false'; Flag = @('-ExpectDownload:$false') }
    ) {
        $run = Invoke-File 'Downloads' $Flag
        $run.Text | Should -Match '::error::The example downloaded its data file'
        $run.Code | Should -Be 1
    }
}

Describe 'run-fresh-clone-example.ps1 given text the output must hold' {

    It 'passes when the output holds the text' {
        $run = Invoke-File 'FromCache' @('-Expect', 'Results:')
        $run.Text | Should -Not -Match '::error::'
        $run.Code | Should -Be 0
    }

    It 'fails when the output does not hold the text' {
        $run = Invoke-File 'FromCache' @('-Expect', '(weighting ')
        $run.Text | Should -Match "::error::The example's output does not hold '\(weighting '\."
        $run.Code | Should -Be 1
    }
}

Describe 'run-fresh-clone-example.ps1 called as the workflow calls it' {

    It 'finds both of the workflow''s calls' -ForEach @(
        @{ Found = $script:WorkflowCalls.Count }
    ) {
        $Found | Should -Be 2
    }

    # The download job expects the example to download its file, and the
    # other jobs expect it to be there already.
    # The command is wrapped as GitHub's pwsh shell wraps a step, which stops
    # on an error and exits with the last exit code.
    It 'passes the <Job> job''s call only when the example does what that job expects' -ForEach $script:WorkflowCalls {
        $quoted = $script:Script.Replace("'", "''")
        $call = $Command.Replace('./ci/run-fresh-clone-example.ps1', "& '$quoted'")
        $call = $call.Replace('${{ matrix.product }}', 'device-detection')
        # The stand-in prints no weighting, so the expected text is the one
        # the device-detection entries give, which is none.
        $call = $call.Replace('${{ matrix.expect }}', '')
        $call = "`$ErrorActionPreference = 'stop'`n$call`n" +
            "if ((Test-Path -LiteralPath variable:\LASTEXITCODE)) { exit `$LASTEXITCODE }"
        $expected, $other = if ($Job -eq 'download') {
            'Downloads', 'FromCache'
        } else {
            'FromCache', 'Downloads'
        }

        $run = Invoke-Pwsh @('-Command', $call.Replace('${{ matrix.example }}', $expected))
        $run.Text | Should -Match 'Results:'
        $run.Text | Should -Not -Match '::error::'
        $run.Code | Should -Be 0

        $run = Invoke-Pwsh @('-Command', $call.Replace('${{ matrix.example }}', $other))
        $run.Text | Should -Match '::error::'
        $run.Code | Should -Be 1
    }
}
