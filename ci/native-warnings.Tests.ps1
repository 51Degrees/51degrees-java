# Pester 5 tests of the warnings the two native engines build with. Every C and
# C++ warning is on, on every compiler, and every SWIG warning is an error.
# Compiler and linker warnings are errors when _51DEGREES_WARNINGS_AS_ERRORS is
# true, as it is in the builds 51Degrees runs. Nothing in these files, in the
# wrappers SWIG made or in the cxx sources the engines compile turns a warning
# off.

BeforeDiscovery {
    $Engines = @(
        @{
            Engine = 'Device Detection'
            Folder = 'device-detection/device-detection.hash.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Hash_Engine.cpp'
            Proxies = 'device-detection/device-detection.hash.engine.on-premise/src/main/java/fiftyone/devicedetection/hash/engine/onpremise/interop/swig'
            Files = @('CMakeLists.txt', 'PreBuild.sh', 'hash_java.i')
        }
        @{
            Engine = 'IP Intelligence'
            Folder = 'ip-intelligence/ip-intelligence.engine.on-premise/src/main/cxx'
            Wrapper = 'Java_Ipi_Engine.cpp'
            Proxies = 'ip-intelligence/ip-intelligence.engine.on-premise/src/main/java/fiftyone/ipintelligence/engine/onpremise/interop/swig'
            Files = @('CMakeLists.txt', 'PreBuild.sh', 'hash_java.i', 'RegenSwig.ps1')
        }
    )

    # The cxx sources and CMake files the engines include. Its tests and
    # examples are not built here. Without the cxx submodule there is nothing
    # to list, and one test says so.
    $Top = Join-Path $PSScriptRoot '..'
    $NoCxx = -not (Test-Path (Join-Path $Top 'cxx/src'))
    $CxxFiles = if ($NoCxx) { @() } else { @(
        Get-ChildItem -File -Recurse (Join-Path $Top 'cxx/src') |
            Where-Object { $_.Extension -in '.c', '.h', '.cpp', '.hpp', '.i' -or
                $_.Name -eq 'CMakeLists.txt' } |
            ForEach-Object {
                [IO.Path]::GetRelativePath($Top, $_.FullName).Replace('\', '/')
            }
        'cxx/CMakeLists.txt'
    ) }
}

BeforeAll {
    $Root = Join-Path $PSScriptRoot '..'
    $NoCxx = -not (Test-Path (Join-Path $Root 'cxx/src'))

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
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -MatchExactly 'target_compile_options\(\$\{TARGET\}\s+BEFORE\s+PRIVATE\s+/W4\)'
        $code | Should -MatchExactly 'target_compile_options\(\$\{TARGET\}\s+BEFORE\s+PRIVATE\s+-Wall\s+-Wextra\b'
    }

    It 'uses no directory-wide command, which CMake 4.5 warns about' {
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -Not -Match (
            '(?m)^\s*(add_compile_options|add_compile_definitions|add_definitions|' +
            'add_link_options|include_directories|link_directories|link_libraries)\s*\(')
    }

    It 'warns at /W4 and -Wall -Wextra on the wrapper' {
        $code = (Get-Code $CMake) -join "`n"
        foreach ($option in '/W4', '-Wall', '-Wextra') {
            $code | Should -MatchExactly (
                'target_compile_options\(\S+-java\s+PRIVATE[^)]*"' +
                [regex]::Escape($option) + '"')
        }
    }

    # The wrapper copies each pointer to and from its jlong with memcpy, so
    # it builds under the strict aliasing rules, and gcc's warning about them
    # stays on.
    It 'builds the wrapper with strict aliasing' {
        (Get-Code $CMake) -join "`n" | Should -Not -Match 'no-strict-aliasing'
    }

    It 'reads no jlong in <Wrapper> as a pointer of another type' {
        $punned = @(Select-String -Path (Join-Path $Root "$Folder/$Wrapper") `
            -Pattern '\*\s*\)\s*&\s*(jarg\d+|jresult|baseptr)\b' |
            ForEach-Object { "$($_.LineNumber): $($_.Line.Trim())" })
        $punned | Should -BeNullOrEmpty
    }

    It 'replaces every base class conversion SWIG writes and stops if one is left' {
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -MatchExactly 'string\(REGEX REPLACE\s+"[^"]*&baseptr'
        $code | Should -MatchExactly 'string\(FIND\s+"\$\{WRAPPER\}"\s+"&baseptr"'
        foreach ($script in $Files | Where-Object { $_ -like '*.ps1' }) {
            $text = (Get-Code "$Folder/$script") -join "`n"
            $text | Should -MatchExactly '\[regex\]::Replace\(\$Code,'
            $text | Should -MatchExactly '\$Code\.Contains\(''&baseptr''\)'
        }
    }

    # SWIG's own std_vector.i puts a this-escape annotation on the vector
    # constructors that take an array or an Iterable. The vector proxies are
    # final, so javac reports no escape, and the annotation is removed. No
    # annotation that turns a javac warning off is allowed in the proxies.
    It 'turns no warning off in the Java proxies SWIG made' {
        $files = @(Get-ChildItem (Join-Path $Root $Proxies) -Filter *.java)
        $files.Count | Should -BeGreaterThan 0
        $left = foreach ($file in $files) {
            if ((Get-Content -Raw $file.FullName).Contains('@SuppressWarnings')) { $file.Name }
        }
        @($left) | Should -BeNullOrEmpty
    }

    It 'makes every vector proxy final' {
        $files = @(Get-ChildItem (Join-Path $Root $Proxies) -Filter *.java)
        $open = foreach ($file in $files) {
            $java = Get-Content -Raw $file.FullName
            if ($java -match '\bextends java\.util\.AbstractList<' -and
                $java -notmatch '\npublic final class ') { $file.Name }
        }
        @($open) | Should -BeNullOrEmpty
        foreach ($interface in $Files | Where-Object { $_ -like '*.i' }) {
            (Get-Code "$Folder/$interface") -join "`n" |
                Should -MatchExactly '%typemap\(javaclassmodifiers\) std::vector "public final class"'
        }
    }

    It 'stops the SWIG run if a proxy turns any warning off' {
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -MatchExactly 'if \("\$\{JAVA\}" MATCHES "@SuppressWarnings"\)\s+message\(FATAL_ERROR'
        foreach ($script in $Files | Where-Object { $_ -like '*.ps1' }) {
            $text = (Get-Code "$Folder/$script") -join "`n"
            $text | Should -MatchExactly '\$Java\.Contains\("@SuppressWarnings"\)'
        }
    }

    It 'stops when a SWIG rebuild is asked for and SWIG is missing' {
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -Not -MatchExactly 'message\(WARNING\s+"SWIG'
        $code | Should -MatchExactly 'NOT SWIG_FOUND\)\s+message\(FATAL_ERROR'
        $code | Should -MatchExactly 'NOT SWIG_java_FOUND\)\s+message\(FATAL_ERROR'
    }

    It 'runs SWIG with every warning on, as an error that stops the build' {
        $calls = @(Get-Code $CMake | Where-Object { $_ -match '\$\{SWIG_EXECUTABLE\}' })
        $calls.Count | Should -BeGreaterThan 0
        foreach ($call in $calls) {
            $call | Should -MatchExactly '\$\{SWIG_EXECUTABLE\}\s+-Wall\s+-Werror\s'
            $call | Should -MatchExactly 'COMMAND_ERROR_IS_FATAL\s+ANY'
            # A failed run removes its output files, so SWIG writes into the
            # build folder and the committed files are replaced after it.
            $call | Should -MatchExactly '-outdir\s+\$\{SWIGOUT\}/'
            $call | Should -MatchExactly '\s-o\s+\$\{SWIGOUT\}/'
        }
        # The scripts that run SWIG by hand pass the same options, and write
        # into a new folder too.
        foreach ($script in $Files | Where-Object { $_ -like '*.ps1' }) {
            $code = (Get-Code "$Folder/$script") -join "`n"
            $code | Should -MatchExactly '&\s*\$swigExec[\s`]+-Wall\s+-Werror\s'
            $code | Should -MatchExactly '-outdir\s+\$TempJava'
            $code | Should -MatchExactly '-o\s+\(Join-Path\s+\$Temp\s'
        }
    }

    It 'takes warnings as errors from _51DEGREES_WARNINGS_AS_ERRORS, in any case' {
        $before = (Get-LinesBeforeCxx $CMake) -join "`n"
        $before -cmatch (
            'string\(TOLOWER\s+"\$ENV\{_51DEGREES_WARNINGS_AS_ERRORS\}"\s+(?<v>\w+)\)') |
            Should -BeTrue
        $before -cmatch (
            'string\(COMPARE\s+EQUAL\s+"\$\{' + $Matches['v'] +
            '\}"\s+"true"\s+(?<d>\w+)\)') | Should -BeTrue
        $before | Should -MatchExactly (
            'option\(_51DEGREES_WARNINGS_AS_ERRORS\s[^)]*\$\{' + $Matches['d'] + '\}\)')
    }

    It 'makes no warning an error unless _51DEGREES_WARNINGS_AS_ERRORS is on' {
        $code = (Get-Code $CMake) -join "`n"
        $code | Should -Not -Match 'CMAKE_COMPILE_WARNING_AS_ERROR\s+ON'
        $code | Should -Not -MatchExactly 'target_compile_options\([^)]*"(-Werror|/WX)"'
        $code | Should -MatchExactly (
            'PROPERTY\s+COMPILE_WARNING_AS_ERROR\s+\$\{_51DEGREES_WARNINGS_AS_ERRORS\}')
        # cxx adds -Werror and /WX to its own libraries, which are taken off.
        $code | Should -MatchExactly 'list\(REMOVE_ITEM\s+OPTIONS\s+"-Werror"\s+"/WX"\)'
        # The function runs over every folder of targets, from this one.
        $code | Should -MatchExactly (
            '(?m)^fiftyone_follow_warnings_switch\(\$\{CMAKE_CURRENT_SOURCE_DIR\}\)')
    }

    It 'makes linker and librarian warnings errors when warnings are errors' {
        $code = (Get-Code $CMake) -join "`n"
        $switched = 'if\s*\(_51DEGREES_WARNINGS_AS_ERRORS\)[\s\S]*'
        $code | Should -MatchExactly ($switched + 'STATIC_LIBRARY_OPTIONS\s+"/WX"')
        $code | Should -MatchExactly ($switched + 'target_link_options\([^)]*"/WX"')
        $code | Should -MatchExactly ($switched + '"LINKER:-fatal_warnings"')
        $code | Should -MatchExactly ($switched + '"LINKER:--fatal-warnings"')
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
    It 'has the cxx submodule to check' {
        $NoCxx | Should -BeFalse -Because (
            'cxx/src is missing, so run git submodule update --init cxx')
    }

    It 'turns no warning off in <_>' -ForEach $CxxFiles {
        @(Find-Hiding $_) | Should -BeNullOrEmpty
    }
}
