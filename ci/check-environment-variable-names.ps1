# Every name 51Degrees defines for an environment variable or a system
# property in this repository starts _51DEGREES_, with the underscore because
# a POSIX shell cannot set a name that starts with a digit. This reads every
# file git tracks, which leaves out build output and the submodules, and fails
# on:
#
# - any name that starts with the digit, wherever it is written, and
# - any name read or set that neither starts _51DEGREES_ nor is one the
#   platform or a build tool defines, which are listed below.
#
# A name counts as read or set when it is a string given to getenv or to
# System.getProperty, setProperty or clearProperty, a name on PowerShell's
# Env: drive, a placeholder in an options file, a secret, variable or env
# entry of a workflow, or an environment variable or system property a Maven
# project file reads or gives the tests.
#
# Dot-sourcing the script defines Test-AllowedName, Find-EnvironmentNameProblem
# and Find-BlockNameProblem without reading the repository, which is how its
# tests use them.

[CmdletBinding()]
param(
    # The root of the checkout to read. The default is the repository this
    # script lives in.
    [string]$RepoRoot = (Join-Path $PSScriptRoot '..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# The start of a name a shell cannot set, split so this file does not match
# itself.
$digitInitial = '5' + '1DEGREES_'

# A name is not matched when a name character comes before it, so a name with
# a leading underscore is left alone. It is matched in any case, because
# Windows reads an environment variable name in any case, so a lower case
# name that starts with the digit is still one a shell cannot set.
$script:DigitInitialPattern = [regex]::new(
    "(?<![A-Za-z0-9_])$digitInitial[A-Za-z0-9_]*",
    [Text.RegularExpressions.RegexOptions]::IgnoreCase)

# A name 51Degrees defines. It keeps its case, because a POSIX shell reads a
# name in its exact case.
$script:OwnNamePattern = [regex]::new('^_51DEGREES_[A-Z0-9][A-Z0-9_]*$')

# The names the platform and the build tools define, which keep the name
# their owner gave them.
$script:PlatformNames = @(
    # The operating system and the shell.
    'PATH', 'USER', 'SystemRoot'
    # The JDK and Maven, with its wrapper.
    'JAVA_HOME', 'MAVEN_USER_HOME', 'MVNW_USERNAME', 'MVNW_PASSWORD',
    'MVNW_VERBOSE', 'MVNW_REPOURL', 'basedir',
    'maven.multiModuleProjectDirectory', 'logback.configurationFile'
    # GraalVM, which runs the JavaScript the tests of the JavaScript builder
    # check.
    'polyglot.engine.WarnInterpreterOnly'
    # GitHub Actions and its runners.
    'CI', 'GITHUB_ACTIONS', 'GITHUB_ENV', 'GITHUB_TOKEN', 'CHROME_BIN',
    'CHROMEWEBDRIVER'
    # The shared browser test suite reads these three to learn where the
    # example under test listens. They are 51Degrees names, and stay here
    # only until the suite reads them with the prefix.
    'PORT', 'EXAMPLE_URL', 'EXAMPLE_LANG'
)

# The system properties the JDK defines start with one of these.
$script:PlatformPropertyPattern = [regex]::new('^(?:java|os|user|sun|file|line)\.[A-Za-z0-9_.]+$')

# Whether a name read or set is one 51Degrees defines under its prefix, or
# one the platform or a build tool defines.
function Test-AllowedName([string]$Name) {
    return $script:OwnNamePattern.IsMatch($Name) -or
        $script:PlatformNames -ccontains $Name -or
        $script:PlatformPropertyPattern.IsMatch($Name)
}

$nameCharacters = '[A-Za-z0-9_.]+'

# The forms a name is read or set in, in any file. Each captures the name.
#
# A Java read takes the name as a string. A system property is matched only
# on System, because other classes have a getProperty of their own.
#
# PowerShell reads a name from its Env: drive, with or without a dollar, a
# brace or a backslash, and in any case.
$script:ReadPatterns = @(
    [regex]::new("\bgetenv\(\s*`"(?<name>$nameCharacters)`"")
    [regex]::new("\bSystem\.(?:get|set|clear)Property\(\s*`"(?<name>$nameCharacters)`"")
    [regex]::new("(?<![A-Za-z0-9_])(?i:env):\\?(?<name>$nameCharacters)")
    [regex]::new("\b[GS]etEnvironmentVariable\(\s*[`"'](?<name>$nameCharacters)[`"']")
)

# A placeholder in an options file, which the options lookup reads as an
# environment variable and then as a system property. A prefix chooses one of
# the two. A placeholder with another prefix, such as a file path, names no
# variable. The Env: form above already finds the env prefix.
$script:PlaceholderPattern = [regex]::new(
    "\$\{(?:(?:sys|envsys):)?(?<name>$nameCharacters)(?=\}|:-)")

# An environment variable a Maven project file reads.
$script:MavenEnvironmentPattern = [regex]::new(
    "\$\{env\.(?<name>$nameCharacters)\}")

# A secret, a variable or an env entry a workflow reads.
$script:WorkflowPattern = [regex]::new(
    "\b(?:secrets|vars|env)\.(?<name>$nameCharacters)")

function Test-OptionsFile([string]$Path) {
    return $Path -like '*.xml' -and (Split-Path $Path -Leaf) -ne 'pom.xml'
}

function Test-MavenProjectFile([string]$Path) {
    return (Split-Path $Path -Leaf) -eq 'pom.xml'
}

function Test-Workflow([string]$Path) {
    return $Path -like '.github/*.yml' -or $Path -like '.github/*.yaml'
}

# The problems on one line of the file at this path, relative to the root
# with forward slashes. Each has its kind, 'digit' or 'name', and its value,
# which is the text matched for a digit and the name read or set otherwise.
function Find-EnvironmentNameProblem([string]$Line, [string]$Path = '') {
    foreach ($match in $script:DigitInitialPattern.Matches($Line)) {
        [pscustomobject]@{ Kind = 'digit'; Value = $match.Value }
    }
    $patterns = @($script:ReadPatterns)
    if (Test-OptionsFile $Path) {
        $patterns += $script:PlaceholderPattern
    }
    if (Test-MavenProjectFile $Path) {
        $patterns += $script:MavenEnvironmentPattern
    }
    if (Test-Workflow $Path) {
        $patterns += $script:WorkflowPattern
    }
    $names = foreach ($pattern in $patterns) {
        foreach ($match in $pattern.Matches($Line)) {
            $match.Groups['name'].Value.TrimEnd('.')
        }
    }
    foreach ($name in @($names | Select-Object -Unique)) {
        # A name that starts with the digit is already reported.
        if ($script:DigitInitialPattern.IsMatch($name)) {
            continue
        }
        if (-not (Test-AllowedName $name)) {
            [pscustomobject]@{ Kind = 'name'; Value = $name }
        }
    }
}

# The names set in a block of the file at this path that are not allowed,
# each with its line number. A workflow sets environment variables as the
# keys of an env block, and a Maven project file gives the tests system
# properties and environment variables as the elements of a block.
function Find-BlockNameProblem([string[]]$Lines, [string]$Path = '') {
    $workflow = Test-Workflow $Path
    $maven = Test-MavenProjectFile $Path
    if (-not $workflow -and -not $maven) {
        return
    }
    $inBlock = $false
    $blockIndent = -1
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        $name = $null
        if ($workflow) {
            if (-not $line.Trim() -or $line -match '^\s*#') {
                continue
            }
            $indent = $line.Length - $line.TrimStart().Length
            if ($inBlock -and $indent -le $blockIndent) {
                $inBlock = $false
            }
            if ($inBlock) {
                if ($line -match "^\s*(?<name>$nameCharacters)\s*:") {
                    $name = $Matches['name']
                }
            } elseif ($line -match '^\s*env\s*:\s*$') {
                $inBlock = $true
                $blockIndent = $indent
            }
        } else {
            if ($line -match '<(?:systemPropertyVariables|environmentVariables)>') {
                $inBlock = $true
            } elseif ($line -match '</(?:systemPropertyVariables|environmentVariables)>') {
                $inBlock = $false
            } elseif ($inBlock -and $line -match "^\s*<(?<name>$nameCharacters)[\s/>]") {
                $name = $Matches['name']
            }
        }
        if ($null -ne $name -and -not (Test-AllowedName $name)) {
            [pscustomobject]@{ Kind = 'name'; Value = $name; LineNumber = $i + 1 }
        }
    }
}

if ($MyInvocation.InvocationName -eq '.') {
    return
}

$root = (Resolve-Path $RepoRoot).Path
# Paths are listed as they are, so a path holding a non-ASCII character is not
# quoted and is still found. git writes them as UTF-8, which is not what a
# Windows console reads by default.
$consoleEncoding = [Console]::OutputEncoding
[Console]::OutputEncoding = [Text.Encoding]::UTF8
try {
    $tracked = & git -C $root -c core.quotePath=false ls-files
} finally {
    [Console]::OutputEncoding = $consoleEncoding
}
if ($LASTEXITCODE -ne 0) {
    throw "git ls-files failed in $root"
}

# The tests of this script name every form it fails on, on purpose.
$ownTests = 'ci/check-environment-variable-names.Tests.ps1'

$digits = @()
$names = @()
$read = 0
$skipped = @()
foreach ($relative in $tracked) {
    if ($relative -eq $ownTests) {
        continue
    }
    $path = Join-Path $root $relative
    # A submodule is tracked as a directory, and its files belong to another
    # repository.
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $skipped += $relative
        continue
    }
    $read++
    $lines = @([IO.File]::ReadLines($path))
    $lineNumber = 0
    foreach ($line in $lines) {
        $lineNumber++
        foreach ($problem in Find-EnvironmentNameProblem -Line $line -Path $relative) {
            $text = "${relative}:${lineNumber}: $($problem.Value)"
            if ($problem.Kind -eq 'digit') {
                $digits += $text
            } else {
                $names += $text
            }
        }
    }
    foreach ($problem in Find-BlockNameProblem -Lines $lines -Path $relative) {
        $names += "${relative}:$($problem.LineNumber): $($problem.Value)"
    }
}

Write-Output ("$read paths read, $($skipped.Count) skipped as not files: " +
    "$($skipped -join ', '), and $ownTests left out")

if ($digits.Count -gt 0 -or $names.Count -gt 0) {
    Write-Output $digits
    Write-Output $names
    throw ("$($digits.Count) names start with a digit, which a POSIX shell " +
        'cannot set, so each must start with an underscore, and ' +
        "$($names.Count) names are read or set that are not _51DEGREES_ " +
        'names or names the platform or a build tool defines.')
}

Write-Output ('Every name read or set is a _51DEGREES_ name or one the ' +
    'platform or a build tool defines.')
