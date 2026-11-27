# Tests for ci/run-fresh-clone-example.ps1.
#
# Each case runs the script in a new pwsh process, with -File as a person
# would type it at a shell, or with -Command as the workflow's pwsh step does.
# The process runs in a folder that holds an empty jar where the script looks
# for one, the data file's link and a copy of the data file to serve, and a
# stand-in for java comes first on the PATH. The stand-in prints results.
# Given the example name Downloads, it also prints the line an example logs
# when it downloads its data file, and asks the address the link names for
# the file, as the data repository's script does. The link names a port on
# this machine that nothing listens on, unless the script points it at the
# stand-in it starts for the file to serve.

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

    # The link, as the workflow names it, and the file to serve, as the
    # workflow names the copy the build fetched.
    $script:Link = 'cxx/data/device-detection/51Degrees-LiteV4.1.hash.link'
    $script:Asset = '51Degrees-LiteV4.1.hash'
    $script:LinkText = "http://127.0.0.1:9/api/v2/free/lite-hash-v41`n"
    $linkPath = Join-Path $script:Root $script:Link
    New-Item -ItemType Directory (Split-Path $linkPath) -Force | Out-Null
    [IO.File]::WriteAllText($linkPath, $script:LinkText)
    $assets = Join-Path $script:Root 'assets'
    New-Item -ItemType Directory $assets -Force | Out-Null
    $bytes = [byte[]]::new(100000)
    [Random]::new(1).NextBytes($bytes)
    [IO.File]::WriteAllBytes((Join-Path $assets $script:Asset), $bytes)

    $script:Bin = Join-Path $TestDrive 'bin'
    New-Item -ItemType Directory $script:Bin | Out-Null
    $missing = "'device-detection/51Degrees-LiteV4.1.hash' is missing, so it is being downloaded by running the script"
    if ($IsWindows) {
        # The arguments are -cp, the jar and the example.
        [IO.File]::WriteAllText((Join-Path $script:Bin 'java.cmd'), (@(
            '@echo off'
            'if not "%~3"=="Downloads" goto results'
            "echo $missing"
            'if not exist cxx\data\device-detection\51Degrees-LiteV4.1.hash.link goto results'
            'for /f "usebackq delims=" %%u in ("cxx\data\device-detection\51Degrees-LiteV4.1.hash.link") do curl -s -D headers.txt -o downloaded.gz "%%u"'
            ':results'
            'echo Results:'
            'exit /b 0'
            '') -join "`r`n"))
    } else {
        $java = Join-Path $script:Bin 'java'
        [IO.File]::WriteAllText($java, (@(
            '#!/bin/sh'
            'if [ "$3" = Downloads ]; then'
            "    echo ""$missing"""
            '    link=cxx/data/device-detection/51Degrees-LiteV4.1.hash.link'
            '    if [ -f "$link" ]; then'
            '        curl -s -D headers.txt -o downloaded.gz "$(head -n 1 "$link")"'
            '    fi'
            'fi'
            'echo Results:'
            'exit 0'
            '') -join "`n"))
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

Describe 'run-fresh-clone-example.ps1 given a file to serve' {

    BeforeEach {
        Remove-Item (Join-Path $script:Root 'downloaded.gz'), (Join-Path $script:Root 'headers.txt') `
            -ErrorAction SilentlyContinue
    }

    It 'serves the file, compressed with its MD5 hash, at the address the link names, and puts the link back' {
        $run = Invoke-File 'Downloads' @('-ExpectDownload', '-LinkFile', $script:Link,
            '-ServeFile', "assets/$script:Asset")
        $run.Text | Should -Not -Match '::error::'
        $run.Text | Should -Match 'Requests the stand-in answered: GET /api/v2/free/lite-hash-v41 HTTP/1\.1'
        $run.Code | Should -Be 0

        $downloaded = Join-Path $script:Root 'downloaded.gz'
        $received = [IO.File]::OpenRead($downloaded)
        try {
            $gzip = [IO.Compression.GZipStream]::new($received, [IO.Compression.CompressionMode]::Decompress)
            $unpacked = [IO.MemoryStream]::new()
            $gzip.CopyTo($unpacked)
            $gzip.Dispose()
        } finally {
            $received.Dispose()
        }
        [Convert]::ToBase64String($unpacked.ToArray()) |
            Should -Be ([Convert]::ToBase64String([IO.File]::ReadAllBytes(
                (Join-Path $script:Root "assets/$script:Asset"))))
        $header = Select-String -LiteralPath (Join-Path $script:Root 'headers.txt') `
            -Pattern '^Content-MD5:\s*(\S+)' -CaseSensitive
        $header.Matches[0].Groups[1].Value |
            Should -BeExactly (Get-FileHash -Algorithm MD5 -LiteralPath $downloaded).Hash.ToLowerInvariant()

        [IO.File]::ReadAllText((Join-Path $script:Root $script:Link)) | Should -BeExactly $script:LinkText
    }

    It 'fails when the example does not download from the stand-in, and puts the link back' {
        $run = Invoke-File 'FromCache' @('-ExpectDownload', '-LinkFile', $script:Link,
            '-ServeFile', "assets/$script:Asset")
        $run.Text | Should -Match '::error::The example did not download its data file from the stand-in\.'
        $run.Text | Should -Match 'Requests the stand-in answered: none'
        $run.Code | Should -Be 1
        [IO.File]::ReadAllText((Join-Path $script:Root $script:Link)) | Should -BeExactly $script:LinkText
    }

    It 'fails given <Name> alone' -ForEach @(
        @{ Name = '-LinkFile'; Flag = @('-LinkFile', 'cxx/data/device-detection/51Degrees-LiteV4.1.hash.link') }
        @{ Name = '-ServeFile'; Flag = @('-ServeFile', 'assets/51Degrees-LiteV4.1.hash') }
    ) {
        $run = Invoke-File 'Downloads' (@('-ExpectDownload') + $Flag)
        $run.Text | Should -Match '::error::-LinkFile and -ServeFile are given together or not at all\.'
        $run.Code | Should -Be 1
    }
}

Describe 'run-fresh-clone-example.ps1 called as the workflow calls it' {

    It 'finds both of the workflow''s calls' -ForEach @(
        @{ Found = $script:WorkflowCalls.Count }
    ) {
        $Found | Should -Be 2
    }

    # The download job expects the example to download its file, from the
    # stand-in the script starts, and the other jobs expect it to be there
    # already.
    # The command is wrapped as GitHub's pwsh shell wraps a step, which stops
    # on an error and exits with the last exit code.
    It 'passes the <Job> job''s call only when the example does what that job expects' -ForEach $script:WorkflowCalls {
        $quoted = $script:Script.Replace("'", "''")
        $call = $Command.Replace('./ci/run-fresh-clone-example.ps1', "& '$quoted'")
        $call = $call.Replace('${{ matrix.product }}', 'device-detection')
        $call = $call.Replace('${{ matrix.file }}', 'cxx/data/device-detection/51Degrees-LiteV4.1.hash')
        $call = $call.Replace('${{ matrix.asset }}', $script:Asset)
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
