# Every 51Degrees environment variable name in this repository starts with an
# underscore, because a POSIX shell cannot set a name that starts with a digit,
# and each variable has one name. An old name is still read, as a deprecated
# fallback for when its new name is not set, by the files listed in
# DeprecatedFallbacks below and by nothing else. This reads every file git
# tracks, which leaves out build output and the submodules, and fails on any
# name that starts with the digit and on any old name, outside the files
# listed for that name.
#
# Dot-sourcing the script defines Find-EnvironmentNameProblem and
# Test-DeprecatedFallback without reading the repository, which is how its
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

# An old name read or set as an environment variable, or held in a constant to
# fall back to. A Java read takes the name as a string, so a quoted bare name
# is matched, along with the PowerShell and config forms the CI scripts and the
# web example use, and a name at the start of a line past any spaces and a
# list dash, which is how a workflow declares one as a key in an env: block
# and how a readme lists one.
#
# The env token is matched on its own, in any case and with a following
# backslash allowed, because PowerShell braces a name as well as prefixing
# it, reads one through the Env: drive, and because the pipeline config
# lookup and a workflow expression both brace one too. Wanting the dollar
# and the token adjacent misses every braced form.
#
# These keep their case. The cloud request engine's tests name the query string
# parameter resource_key, which differs from RESOURCE_KEY only by case, and a
# query string parameter is on the wire rather than in the environment, so
# matching in any case would fail on a name that is not an environment
# variable at all.
$script:FallbackPattern = [regex]::new(
    '(?:["'']|(?i:env)[:.]\\?|\$\{|getenv\(\s*["'']|^[\t\- ]*)' +
    '(?:RESOURCE_KEY|LICENSE_KEY|FOD_CLOUD_API_URL)(?![A-Za-z0-9_])|' +
    '\bLEGACY_[A-Z_]*ENV_VARS?\b')

# The files that read an old name as a deprecated fallback, or that test
# that read, each with the old names it may hold. The names are put together
# from parts so that this file does not match itself.
$oldEndpoint = 'FOD_CLOUD' + '_API_URL'
$oldResourceKey = 'RESOURCE' + '_KEY'
$oldLicenseKey = 'LICENSE' + '_KEY'
$oldDigitKey = $digitInitial + $oldResourceKey
$oldIpiPath = $digitInitial + 'IPI_PATH'
$oldDdPath = $digitInitial + 'DD_PATH'
$cloudEngine = 'pipeline/pipeline.cloudrequestengine/src'
$did = 'pipeline/pipeline.did/src'
$examples = 'pipeline/pipeline.developer-examples/pipeline.developer-examples'
$ipiShared = 'ip-intelligence/ip-intelligence.shared/src'
$ipiHelpers = 'fiftyone/ipintelligence/shared/testhelpers'
$ipiExamples = 'examples/ip-intelligence/shared/src'
$ipiExampleHelpers = 'fiftyone/ipintelligence/examples/shared'
$onPrem = 'examples/device-detection/web/getting-started.onprem/src'
$onPremPackage = 'fiftyone/devicedetection/examples/web'
$script:DeprecatedFallbacks = @{
    "$cloudEngine/main/java/fiftyone/pipeline/cloudrequestengine/flowelements/Constants.java" =
        @($oldEndpoint)
    "$cloudEngine/test/java/fiftyone/pipeline/cloudrequestengine/flowelements/CloudRequestEngineBuilderTests.java" =
        @($oldEndpoint)
    "$did/main/java/fiftyone/pipeline/did/DidClient.java" =
        @($oldEndpoint)
    "$did/test/java/fiftyone/pipeline/did/DidClientEnvironmentTests.java" =
        @($oldEndpoint)
    "$did/test/java/fiftyone/pipeline/did/LiveCloudKeys.java" =
        @($oldResourceKey, $oldLicenseKey)
    "$did/test/java/fiftyone/pipeline/did/LiveCloudKeysTests.java" =
        @($oldResourceKey, $oldLicenseKey)
    "$examples.cloud-engine/src/main/java/pipeline/developerexamples/cloudengine/Main.java" =
        @($oldEndpoint, $oldResourceKey)
    "$examples.cloud-engine/src/test/java/pipeline/developerexamples/cloudengine/EndpointTests.java" =
        @($oldEndpoint, $oldResourceKey)
    "$examples.fodid/src/main/java/pipeline/developerexamples/fodid/CreatorContextDemoServer.java" =
        @($oldResourceKey, $oldLicenseKey)
    "$examples.fodid/src/test/java/pipeline/developerexamples/fodid/DeprecatedKeyNameTests.java" =
        @($oldResourceKey, $oldLicenseKey)
    "$ipiShared/main/java/$ipiHelpers/FileUtils.java" = @($oldIpiPath)
    "$ipiShared/test/java/$ipiHelpers/FileUtilsTest.java" = @($oldIpiPath)
    "$ipiShared/main/java/$ipiHelpers/KeyUtils.java" = @($oldDigitKey)
    "$ipiShared/test/java/$ipiHelpers/KeyUtilsTest.java" = @($oldDigitKey)
    "$ipiExamples/main/java/$ipiExampleHelpers/DataFileHelper.java" =
        @($oldIpiPath)
    "$ipiExamples/test/java/$ipiExampleHelpers/DataFileHelperTest.java" =
        @($oldIpiPath)
    "$ipiExamples/main/java/$ipiExampleHelpers/KeyHelper.java" =
        @($oldDigitKey)
    "$ipiExamples/test/java/$ipiExampleHelpers/KeyHelperTest.java" =
        @($oldDigitKey)
    'ip-intelligence/ip-intelligence.translation/src/test/java/fiftyone/ipintelligence/translation/CountryTranslationIntegrationTest.java' =
        @($oldIpiPath)
    "$onPrem/main/webapp/WEB-INF/51Degrees-OnPrem.xml" = @($oldDdPath)
    "$onPrem/main/java/$onPremPackage/GettingStartedWebOnPrem.java" =
        @($oldDdPath)
    "$onPrem/test/java/$onPremPackage/OnPremDataFileOptionTest.java" =
        @($oldDdPath)
}

# Whether the text matched in the file at this path, relative to the root
# with forward slashes, is an old name that file is listed for. The name is
# the run of name characters the match ends with, compared in its exact
# case.
function Test-DeprecatedFallback([string]$Path, [string]$Value) {
    $allowed = $script:DeprecatedFallbacks[$Path]
    if ($null -eq $allowed) {
        return $false
    }
    $name = [regex]::Match($Value, '[A-Za-z0-9_]+$').Value
    return $allowed -ccontains $name
}

# The problems on one line, each as its kind, 'digit' or 'fallback', and the
# text matched.
function Find-EnvironmentNameProblem([string]$Line) {
    foreach ($match in $script:DigitInitialPattern.Matches($Line)) {
        [pscustomobject]@{ Kind = 'digit'; Value = $match.Value }
    }
    foreach ($match in $script:FallbackPattern.Matches($Line)) {
        [pscustomobject]@{ Kind = 'fallback'; Value = $match.Value }
    }
}

if ($MyInvocation.InvocationName -eq '.') {
    return
}

$root = (Resolve-Path $RepoRoot).Path
# Paths are listed as they are, so a path holding a non-ASCII character is not
# quoted and is still found.
$tracked = & git -C $root -c core.quotePath=false ls-files
if ($LASTEXITCODE -ne 0) {
    throw "git ls-files failed in $root"
}

# The tests of this script name every form it fails on, on purpose.
$ownTests = 'ci/check-environment-variable-names.Tests.ps1'

$found = @()
$fallbacks = @()
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
    $lineNumber = 0
    foreach ($line in [IO.File]::ReadLines($path)) {
        $lineNumber++
        foreach ($problem in Find-EnvironmentNameProblem $line) {
            if (Test-DeprecatedFallback $relative $problem.Value) {
                continue
            }
            $text = "${relative}:${lineNumber}: $($problem.Value)"
            if ($problem.Kind -eq 'digit') {
                $found += $text
            } else {
                $fallbacks += $text
            }
        }
    }
}

Write-Output ("$read paths read, $($skipped.Count) skipped as not files: " +
    "$($skipped -join ', '), and $ownTests left out")

if ($found.Count -gt 0 -or $fallbacks.Count -gt 0) {
    Write-Output $found
    Write-Output $fallbacks
    throw ("$($found.Count) names start with a digit, which a POSIX shell " +
        'cannot set, so each must start with an underscore, and ' +
        "$($fallbacks.Count) old names are read, set or kept, where each " +
        'variable has one name.')
}

Write-Output 'No tracked file names a variable starting with a digit or an old name.'
