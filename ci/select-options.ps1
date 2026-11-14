param(
    # Every entry of options.json.
    [Parameter(Mandatory)][object[]]$Options,
    # The files the pull request changes, less documentation.
    [string[]]$ChangedFiles,
    # Those of the changed files whose change touches only comments.
    [string[]]$CommentOnlyFiles
)
# Chooses the options.json entries a pull request is built on, for common-ci's
# steps/select-options.ps1. The branch itself and a release build every entry.
#
# The standard set is JDK 17 and JDK 8 on Linux, the main JDK and the oldest,
# without the three IP Intelligence tests that load the Enterprise data file,
# and with the Selenium contract on JDK 17 alone. A change in more than its
# comments to a native module or the cxx submodule adds Windows and Linux ARM,
# where the native library is built differently. A change to IP Intelligence
# or the cxx submodule runs the three tests, and a change to a pom.xml adds
# JDK 11 and 21. A change to a file outside the package trees, such as ci,
# counts for every entry. Only five macOS jobs run at once across the
# organisation, and the releases need them, so macOS is left to the branch and
# release builds, as is performance.

$Standard = @('Ubuntu_Java_17', 'Ubuntu_Java_8')
$Contract = 'Ubuntu_Java_17'
$NativeEntries = @('Windows_Java_17', 'Ubuntu_ARM_Java_17')
$Jdk = @('Ubuntu_Java_11', 'Ubuntu_Java_21')

$PackageTrees = @('pipeline/', 'device-detection/', 'ip-intelligence/',
    'location/', 'examples/')
$NativePaths = @('device-detection/device-detection.hash.engine.on-premise/',
    'ip-intelligence/ip-intelligence.engine.on-premise/')
$IpIntelligencePaths = @('ip-intelligence/', 'examples/ip-intelligence/')

function Test-Under([string]$Path, [string[]]$Prefixes) {
    return @($Prefixes | Where-Object { $Path.StartsWith($_) }).Count -gt 0
}

$code = @($ChangedFiles | Where-Object { $CommentOnlyFiles -notcontains $_ })
$cxx = @($code | Where-Object { $_ -eq 'cxx' }).Count -gt 0
$native = $cxx -or @($code | Where-Object { Test-Under $_ $NativePaths }).Count -gt 0
$ipi = $cxx -or
    @($code | Where-Object { Test-Under $_ $IpIntelligencePaths }).Count -gt 0
$poms = @($code | Where-Object { $_ -match '(^|/)pom\.xml$' }).Count -gt 0
$other = @($code | Where-Object {
    $_ -ne 'cxx' -and $_ -notmatch '(^|/)pom\.xml$' -and
        !(Test-Under $_ $PackageTrees)
}).Count -gt 0
Write-Host ("Native: $native. IP Intelligence: $ipi. pom.xml: $poms. " +
    "Outside the package trees: $other.")

foreach ($entry in $Options) {
    $name = $entry.Name
    if ($entry.Image -like 'macos*') {
        Write-Host "$name is left to the branch and release builds"
        continue
    }
    $why = if ($Standard -contains $name) { 'the standard set' }
        elseif ($other) { 'a change outside the package trees' }
        elseif ($native -and $NativeEntries -contains $name) { 'a native change' }
        elseif ($poms -and $Jdk -contains $name) { 'a pom.xml change' }
    if (!$why) {
        Write-Host "$name is not needed for this change"
        continue
    }
    $entry.Depth = ($ipi -or $other) ? 'deep' : 'standard'
    $entry.SeleniumContract = $name -eq $Contract
    $entry.RunPerformance = $false
    Write-Host "$name runs for $why, at depth $($entry.Depth)"
    $entry
}
