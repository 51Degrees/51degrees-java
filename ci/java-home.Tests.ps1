# Pester 5 tests of the helpers in packages.ps1 that make every Maven call
# that compiles run on JDK 17 while the tests run on the entry's own JDK, and
# a check that each script that compiles uses them.

BeforeAll {
    . (Join-Path $PSScriptRoot 'packages.ps1')

    # The runner variables are set by name here, as the runner sets them.
    function Set-RunnerJdk([string]$Name, [string]$Value) {
        [Environment]::SetEnvironmentVariable($Name, $Value)
    }
    $Arch = if ([Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture -eq
        [Runtime.InteropServices.Architecture]::Arm64) { 'arm64' } else { 'X64' }
}

Describe 'Get-CompileJavaHome' {
    BeforeEach {
        Set-RunnerJdk 'JAVA_HOME_17_X64' '/jdk/17-x64'
        Set-RunnerJdk 'JAVA_HOME_17_arm64' '/jdk/17-arm64'
    }
    AfterEach {
        Set-RunnerJdk 'JAVA_HOME_17_X64' $null
        Set-RunnerJdk 'JAVA_HOME_17_arm64' $null
    }

    It 'gives JDK 17 for an entry on <Entry>' -ForEach @(
        @{ Entry = 'JAVA_HOME_8_X64'; Expected = '/jdk/17-x64' }
        @{ Entry = 'JAVA_HOME_11_X64'; Expected = '/jdk/17-x64' }
        @{ Entry = 'JAVA_HOME_17_X64'; Expected = '/jdk/17-x64' }
        @{ Entry = 'JAVA_HOME_21_X64'; Expected = '/jdk/17-x64' }
        @{ Entry = 'JAVA_HOME_11_arm64'; Expected = '/jdk/17-arm64' }
        @{ Entry = 'JAVA_HOME_21_arm64'; Expected = '/jdk/17-arm64' }
    ) {
        Get-CompileJavaHome $Entry | Should -Be $Expected
    }

    It 'gives JDK 17 of the runner architecture with no entry' {
        Get-CompileJavaHome | Should -Be "/jdk/17-$($Arch.ToLower())"
    }

    It 'fails when the runner has no JDK 17' {
        Set-RunnerJdk 'JAVA_HOME_17_X64' $null
        { Get-CompileJavaHome 'JAVA_HOME_8_X64' } |
            Should -Throw '*JAVA_HOME_17_X64 is not set*'
    }

    It 'fails on a name that is not a runner JDK variable' {
        { Get-CompileJavaHome 'JAVA_HOME' } |
            Should -Throw "*'JAVA_HOME' is not a runner JDK variable*"
    }
}

Describe 'Invoke-WithJavaHome' {
    It 'runs the block on the JDK given and puts JAVA_HOME and PATH back' {
        $javaHome, $path = $env:JAVA_HOME, $env:PATH
        $seen = Invoke-WithJavaHome '/jdk/17' {
            $env:JAVA_HOME
            ($env:PATH -split [IO.Path]::PathSeparator)[0]
        }
        $seen | Should -Be @('/jdk/17', (Join-Path '/jdk/17' 'bin'))
        $env:JAVA_HOME | Should -Be $javaHome
        $env:PATH | Should -Be $path
    }

    It 'puts JAVA_HOME and PATH back when the block fails' {
        $javaHome, $path = $env:JAVA_HOME, $env:PATH
        { Invoke-WithJavaHome '/jdk/17' { throw 'failed' } } | Should -Throw 'failed'
        $env:JAVA_HOME | Should -Be $javaHome
        $env:PATH | Should -Be $path
    }
}

Describe 'Get-TestJvmArgument' {
    It "gives Surefire the java of the entry's JDK" {
        $javaHome = $env:JAVA_HOME
        try {
            $env:JAVA_HOME = '/jdk/8'
            $java = Join-Path '/jdk/8' 'bin' "java$(if ($IsWindows) { '.exe' })"
            Get-TestJvmArgument | Should -Be "-Djvm=$java"
        } finally {
            $env:JAVA_HOME = $javaHome
        }
    }
}

# Every Maven call in a script that compiles is inside a block that runs it on
# the compile JDK, so a new call that compiles on the entry's JDK fails here.
Describe 'Scripts that compile' {
    It '<Name> runs every Maven call on the compile JDK' -ForEach @(
        @{ Name = 'build-project.ps1' }
        @{ Name = 'build-package-requirements.ps1' }
        @{ Name = 'build-package.ps1' }
        @{ Name = 'run-integration-tests.ps1' }
        @{ Name = 'run-performance-tests.ps1' }
    ) {
        $path = Join-Path $PSScriptRoot $Name
        $ast = [Management.Automation.Language.Parser]::ParseFile(
            $path, [ref]$null, [ref]$null)
        $calls = $ast.FindAll({
            param($node)
            $node -is [Management.Automation.Language.CommandAst] -and
                $node.GetCommandName() -in 'mvn', './java/build-package.ps1'
        }, $true)
        $calls.Count | Should -BeGreaterThan 0
        foreach ($call in $calls) {
            $inside = $false
            for ($node = $call.Parent; $node; $node = $node.Parent) {
                if ($node -is [Management.Automation.Language.CommandAst] -and
                    $node.GetCommandName() -eq 'Invoke-WithJavaHome') {
                    $inside = $true
                    break
                }
            }
            $inside | Should -BeTrue -Because "line $($call.Extent.StartLineNumber) of $Name runs Maven"
        }
    }

    It '<Name> takes the entry JDK variable' -ForEach @(
        @{ Name = 'build-project.ps1' }
        @{ Name = 'build-package-requirements.ps1' }
        @{ Name = 'run-integration-tests.ps1' }
        @{ Name = 'run-performance-tests.ps1' }
    ) {
        (Get-Command (Join-Path $PSScriptRoot $Name)).Parameters.ContainsKey(
            'JavaSDKEnvVar') | Should -BeTrue
    }
}
