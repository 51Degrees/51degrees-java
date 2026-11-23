# Pester 5 tests of the options the two native engines give the C and C++
# code. Each engine's CMakeLists.txt includes the CMakeLists.txt of the cxx
# submodule, after setting the options that leave the other product out. An
# option cxx does not declare is ignored without a message, so the names must
# be the ones cxx reads.

BeforeDiscovery {
    $Root = Join-Path $PSScriptRoot '..'
    $Engines = @(
        @{
            Engine = 'Device Detection'
            Path = 'device-detection/device-detection.hash.engine.on-premise/src/main/cxx/CMakeLists.txt'
            LeavesOut = '_51DEGREES_BUILD_IP_INTELLIGENCE'
        }
        @{
            Engine = 'IP Intelligence'
            Path = 'ip-intelligence/ip-intelligence.engine.on-premise/src/main/cxx/CMakeLists.txt'
            LeavesOut = '_51DEGREES_BUILD_DEVICE_DETECTION'
        }
    )
    # The pull request checks do not check the submodules out, so the tests
    # that read cxx run only where it is there, which includes every build.
    $NoCxx = -not (Test-Path (Join-Path $Root 'cxx/CMakeLists.txt'))
}

BeforeAll {
    $Root = Join-Path $PSScriptRoot '..'

    # The lines of the file before it includes cxx, without comments. Only
    # these can change what cxx builds.
    function Get-LinesBeforeCxx([string]$Path) {
        $lines = Get-Content (Join-Path $Root $Path)
        $before = foreach ($line in $lines) {
            if ($line -match '^\s*include\(.*cxx/CMakeLists\.txt') {
                break
            }
            $line -replace '#.*$', ''
        }
        $include = @($lines | Where-Object {
            $_ -match '^\s*include\(.*cxx/CMakeLists\.txt' })
        $include.Count | Should -Be 1 -Because "$Path includes cxx once"
        @($before)
    }

    # The cache options the file sets before it includes cxx.
    function Get-OptionsSet([string]$Path) {
        @(Get-LinesBeforeCxx $Path | ForEach-Object {
            if ($_ -match '^\s*set\(\s*(?<name>[A-Za-z0-9_]+)\s.*\bCACHE\s+BOOL\b') {
                $Matches['name']
            }
        })
    }

    # The names the file tests in an if before it includes cxx, leaving out
    # CMake's own words and anything written as ${...} or in quotes.
    function Get-NamesTested([string]$Path) {
        $words = 'NOT', 'AND', 'OR', 'DEFINED', 'MATCHES', 'STREQUAL',
            'MSVC', 'UNIX', 'APPLE', 'WIN32', 'ARCH',
            'CMAKE_GENERATOR_PLATFORM'
        @(Get-LinesBeforeCxx $Path | ForEach-Object {
            if ($_ -match '^\s*(?:else)?if\s*\((?<test>.*)\)\s*$') {
                $test = $Matches['test'] -replace '\$\{[^}]*\}', ''
                $test = $test -replace '"[^"]*"', ''
                [regex]::Matches($test, '(?<![\^.*\w])[A-Za-z0-9_]+(?![\w.*])') |
                    ForEach-Object { $_.Value } |
                    Where-Object { $words -cnotcontains $_ }
            }
        } | Select-Object -Unique)
    }

    # The options cxx declares.
    function Get-CxxOptions {
        @(Get-Content (Join-Path $Root 'cxx/CMakeLists.txt') | ForEach-Object {
            if ($_ -match '^\s*option\(\s*(?<name>[A-Za-z0-9_]+)\s') {
                $Matches['name']
            }
        })
    }
}

Describe 'the <Engine> engine CMakeLists.txt' -ForEach $Engines {
    It 'leaves the other product out under the name <LeavesOut>' {
        Get-OptionsSet $Path | Should -Contain $LeavesOut
    }

    It 'sets no option before cxx but the other product and the tests' {
        Get-OptionsSet $Path | Sort-Object |
            Should -Be (@($LeavesOut, 'BUILD_TESTING') | Sort-Object)
    }

    It 'sets only options cxx declares' -Skip:$NoCxx {
        $declared = @(Get-CxxOptions) + 'BUILD_TESTING'
        $declared.Count | Should -BeGreaterThan 1
        foreach ($name in Get-OptionsSet $Path) {
            $declared | Should -Contain $name
        }
    }

    It 'tests only options cxx declares' -Skip:$NoCxx {
        $declared = @(Get-CxxOptions)
        foreach ($name in Get-NamesTested $Path) {
            $declared | Should -Contain $name
        }
    }

    It 'tests no option under a name without the prefix' {
        foreach ($name in Get-NamesTested $Path) {
            $name | Should -Match '^_51DEGREES_'
        }
    }
}
