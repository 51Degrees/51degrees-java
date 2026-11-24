# Pester 5 tests of the warnings the two native engines build with. Every C and
# C++ warning is an error on every compiler, at the level the cxx submodule
# builds its own tests with, and nothing in these files turns a warning off.

BeforeDiscovery {
    $Engines = @(
        @{
            Engine = 'Device Detection'
            Folder = 'device-detection/device-detection.hash.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Hash_Engine.cpp'
        }
        @{
            Engine = 'IP Intelligence'
            Folder = 'ip-intelligence/ip-intelligence.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Ipi_Engine.cpp'
        }
    )
}

BeforeAll {
    $Root = Join-Path $PSScriptRoot '..'

    # The lines of a file without comments. Comments start with # in CMake and
    # with rem or :: in a batch file.
    function Get-Code([string]$Path) {
        @(Get-Content (Join-Path $Root $Path) | ForEach-Object {
            if ($Path -like '*.bat') {
                if ($_ -notmatch '^\s*(rem\b|::)') { $_ }
            } else {
                $_ -replace '#.*$', ''
            }
        })
    }

    # The CMake lines before the file includes cxx, which apply to every target
    # cxx adds as well as to the wrapper.
    function Get-LinesBeforeCxx([string]$Path) {
        foreach ($line in Get-Code $Path) {
            if ($line -match '^\s*include\(.*cxx/CMakeLists\.txt') {
                break
            }
            $line
        }
    }

    # Switches that hide warnings from MSVC, GCC or Clang, or that turn off the
    # CRT and C++ library deprecation warnings.
    $Hiding = @(
        '(?<![\w-])[/-]wd\d+'
        '(?<![\w-])[/-]w(?![\w-])'
        '(?<![\w-])-Wno-'
        '_CRT_SECURE_NO_WARNINGS'
        '(?<!SWIG_NO)_CRT_SECURE_NO_DEPRECATE'
        '_SCL_SECURE_NO_WARNINGS'
        '(?<!SWIG_NO)_SCL_SECURE_NO_DEPRECATE'
        'pragma\s+warning\s*\(\s*disable'
        'diagnostic\s+ignored'
    )
}

Describe 'the <Engine> engine native build' -ForEach $Engines {
    BeforeAll {
        $CMake = "$Folder/CMakeLists.txt"
    }

    # PreBuild.bat is left out because its own /w argument names the Windows
    # SDK. It passes the compiler nothing.
    It 'turns no warning off in <_>' -ForEach @(
        'CMakeLists.txt', 'PreBuild.sh', 'hash_java.i') {
        $code = (Get-Code "$Folder/$_") -join "`n"
        foreach ($pattern in $Hiding) {
            $code | Should -Not -MatchExactly $pattern
        }
    }

    It 'needs a CMake that leaves the warning level out of the default flags' {
        $code = (Get-Code $CMake) -join "`n"
        $code -match 'cmake_minimum_required\(VERSION\s+(?<v>\d+\.\d+)[\s)]' |
            Should -BeTrue
        [version]$Matches['v'] | Should -BeGreaterOrEqual ([version]'3.24')
    }

    It 'warns at /W4 and -Wall on every target' {
        $before = (Get-LinesBeforeCxx $CMake) -join "`n"
        $before | Should -MatchExactly 'add_compile_options\([^)]*/W4'
        $before | Should -MatchExactly 'add_compile_options\([^)]*-Wall'
    }

    It 'makes every warning an error' {
        $before = (Get-LinesBeforeCxx $CMake) -join "`n"
        $before | Should -Match 'set\(\s*CMAKE_COMPILE_WARNING_AS_ERROR\s+ON\s*\)'
        (Get-Code $CMake) -join "`n" |
            Should -MatchExactly 'target_link_options\([^)]*/WX'
    }

    It 'keeps the warnings the SWIG wrapper would turn off' {
        $code = (Get-Code $CMake) -join "`n"
        foreach ($name in 'SWIG_MSC_UNSUPPRESS_4505',
            'SWIG_NO_CRT_SECURE_NO_DEPRECATE', 'SWIG_NO_SCL_SECURE_NO_DEPRECATE') {
            $code | Should -Match "target_compile_definitions\([^)]*\b$name\b"
        }
    }

    It 'stops PreBuild.bat at the first CMake command that fails' {
        $commands = @(Get-Code "$Folder/PreBuild.bat" |
            Where-Object { $_ -match '^\s*cmake\s' })
        $commands.Count | Should -BeGreaterThan 0
        foreach ($command in $commands) {
            $command | Should -Match '\|\|\s*exit\s+/b\s+1\s*$'
        }
    }

    It 'stops PreBuild.sh at the first command that fails' {
        (Get-Code "$Folder/PreBuild.sh") -join "`n" | Should -Match '(?m)^\s*set\s+-e'
    }
}
