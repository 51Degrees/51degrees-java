# Pester 5 tests of the warnings the two native engines build with. Every C and
# C++ warning is on and is an error on every compiler, every SWIG warning is an
# error, and nothing in these files, in the wrappers SWIG made or in the cxx
# sources the engines compile turns a warning off.

BeforeDiscovery {
    $Engines = @(
        @{
            Engine = 'Device Detection'
            Folder = 'device-detection/device-detection.hash.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Hash_Engine.cpp'
            Files = @('CMakeLists.txt', 'PreBuild.sh', 'hash_java.i')
        }
        @{
            Engine = 'IP Intelligence'
            Folder = 'ip-intelligence/ip-intelligence.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Ipi_Engine.cpp'
            Files = @('CMakeLists.txt', 'PreBuild.sh', 'hash_java.i', 'RegenSwig.ps1')
        }
    )

    # The cxx sources and CMake files the engines include. Its tests and
    # examples are not built here.
    $Top = Join-Path $PSScriptRoot '..'
    $CxxFiles = @(
        Get-ChildItem -File -Recurse (Join-Path $Top 'cxx/src') |
            Where-Object { $_.Extension -in '.c', '.h', '.cpp', '.hpp', '.i' -or
                $_.Name -eq 'CMakeLists.txt' } |
            ForEach-Object {
                [IO.Path]::GetRelativePath($Top, $_.FullName).Replace('\', '/')
            }
        'cxx/CMakeLists.txt'
    )
}

BeforeAll {
    $Root = Join-Path $PSScriptRoot '..'

    # The lines of a file without comments. Comments start with # in CMake,
    # PowerShell and shell scripts, and with rem or :: in a batch file. Other
    # files, such as SWIG and C files, are taken whole, as # starts a
    # preprocessor line there.
    function Get-Code([string]$Path) {
        @(Get-Content (Join-Path $Root $Path) | ForEach-Object {
            if ($Path -like '*.bat') {
                if ($_ -notmatch '^\s*(rem\b|::)') { $_ }
            } elseif ($Path -match '(CMakeLists\.txt|\.cmake|\.ps1|\.sh)$') {
                $_ -replace '#.*$', ''
            } else {
                $_
            }
        })
    }

    # Each line of a file that turns a warning off, as path:line: text.
    function Find-Hiding([string]$Path) {
        $number = 0
        foreach ($line in Get-Code $Path) {
            $number++
            foreach ($pattern in $Hiding) {
                if ($line -cmatch $pattern) {
                    "${Path}:${number}: $($line.Trim())"
                    break
                }
            }
        }
    }

    # The lines of a SWIG wrapper that turn a warning off outside the guards
    # SWIG puts them in, which the build keeps closed. The definitions
    # SWIG_MSC_UNSUPPRESS_4505, SWIG_NO_CRT_SECURE_NO_DEPRECATE and
    # SWIG_NO_SCL_SECURE_NO_DEPRECATE close the first three, and no release is
    # built with the Intel compiler.
    function Find-UnguardedHiding([string]$Path) {
        $guards = @(
            '^#\s*ifndef\s+SWIG_MSC_UNSUPPRESS_4505\b'
            '^#\s*if\b.*!defined\(SWIG_NO_CRT_SECURE_NO_DEPRECATE\)'
            '^#\s*if\b.*!defined\(SWIG_NO_SCL_SECURE_NO_DEPRECATE\)'
            '^#\s*ifdef\s+__INTEL_COMPILER\b'
        )
        $wrapperHiding = $Hiding + @(
            'pragma\s+warning\s+disable'
            '#\s*define\s+_(CRT|SCL)_SECURE_NO_(DEPRECATE|WARNINGS)\b'
        )
        # Whether each open #if block is a guard, innermost last.
        $open = [System.Collections.Generic.List[bool]]::new()
        $number = 0
        foreach ($line in Get-Content (Join-Path $Root $Path)) {
            $number++
            $text = $line.Trim()
            if ($text -match '^#\s*if') {
                $guarded = $false
                foreach ($guard in $guards) {
                    if ($text -match $guard) { $guarded = $true }
                }
                $open.Add($guarded)
                continue
            }
            if ($text -match '^#\s*(else|elif)\b' -and $open.Count -gt 0) {
                $open[$open.Count - 1] = $false
                continue
            }
            if ($text -match '^#\s*endif\b' -and $open.Count -gt 0) {
                $open.RemoveAt($open.Count - 1)
                continue
            }
            if ($open -contains $true) {
                continue
            }
            foreach ($pattern in $wrapperHiding) {
                if ($text -cmatch $pattern) {
                    "${Path}:${number}: $text"
                    break
                }
            }
        }
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
    It 'turns no warning off in <_>' -ForEach $Files {
        @(Find-Hiding "$Folder/$_") | Should -BeNullOrEmpty
    }

    It 'turns no warning off in <Wrapper> outside the guards the build keeps closed' {
        @(Find-UnguardedHiding "$Folder/$Wrapper") | Should -BeNullOrEmpty
    }

    It 'needs a CMake that leaves the warning level out of the default flags' {
        $code = (Get-Code $CMake) -join "`n"
        $code -match 'cmake_minimum_required\(VERSION\s+(?<v>\d+\.\d+)[\s)]' |
            Should -BeTrue
        [version]$Matches['v'] | Should -BeGreaterOrEqual ([version]'3.24')
    }

    It 'warns at /W4 and -Wall -Wextra on every target' {
        $before = (Get-LinesBeforeCxx $CMake) -join "`n"
        $before | Should -MatchExactly 'add_compile_options\([^)]*/W4'
        $before | Should -MatchExactly 'add_compile_options\([^)]*-Wall'
        $before | Should -MatchExactly 'add_compile_options\([^)]*-Wextra'
    }

    It 'warns at /W4 and -Wall -Wextra on the wrapper' {
        $code = (Get-Code $CMake) -join "`n"
        foreach ($option in '/W4', '-Wall', '-Wextra') {
            $code | Should -MatchExactly (
                'target_compile_options\(\S+-java\s+PRIVATE[^)]*"' +
                [regex]::Escape($option) + '"')
        }
    }

    It 'runs SWIG with every warning on, as an error that stops the build' {
        $calls = @(Get-Code $CMake | Where-Object { $_ -match '\$\{SWIG_EXECUTABLE\}' })
        $calls.Count | Should -BeGreaterThan 0
        foreach ($call in $calls) {
            $call | Should -MatchExactly '\$\{SWIG_EXECUTABLE\}\s+-Wall\s+-Werror\s'
            $call | Should -MatchExactly 'COMMAND_ERROR_IS_FATAL\s+ANY'
        }
        # The scripts that run SWIG by hand pass the same options.
        foreach ($script in $Files | Where-Object { $_ -like '*.ps1' }) {
            (Get-Code "$Folder/$script") -join "`n" |
                Should -MatchExactly '&\s*\$swigExec[\s`]+-Wall\s+-Werror\s'
        }
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

Describe 'the cxx sources the native engines compile' {
    It 'turns no warning off in <_>' -ForEach $CxxFiles {
        @(Find-Hiding $_) | Should -BeNullOrEmpty
    }
}
