# Every case change in the main code names its locale. Java's toLowerCase()
# and toUpperCase() with no argument use the default locale, and under a
# Turkish or Azeri default a capital I lowers to a dotless i, so a name that
# holds one no longer matches its lower case form. This reads every Java file
# git tracks under a src/main folder, which leaves out the tests, build output
# and the submodules, and fails on any call that names no locale. Pass
# Locale.ROOT.
#
# Dot-sourcing the script defines Find-DefaultLocaleCaseChange and
# Test-MainSource without reading the repository, which is how its tests use
# them.

[CmdletBinding()]
param(
    # The root of the checkout to read. The default is the repository this
    # script lives in.
    [string]$RepoRoot = (Join-Path $PSScriptRoot '..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:DefaultLocalePattern = [regex]::new('\.to(?:Lower|Upper)Case\(\s*\)')

function Find-DefaultLocaleCaseChange {
    # One "<path>:<line>: <text>" for each line of the text with a case
    # change that names no locale.
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][AllowEmptyString()][string]$Text
    )
    $lines = $Text -split "`r?`n"
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($script:DefaultLocalePattern.IsMatch($lines[$i])) {
            "${Path}:$($i + 1): $($lines[$i].Trim())"
        }
    }
}

function Test-MainSource {
    # Whether a path as git gives it is a Java file under a src/main folder.
    param([Parameter(Mandatory)][string]$Path)
    $Path.EndsWith('.java') -and "/$Path".Contains('/src/main/')
}

if ($MyInvocation.InvocationName -eq '.') {
    return
}

$root = (Resolve-Path $RepoRoot).Path
$tracked = & git -C $root -c core.quotePath=false ls-files
if ($LASTEXITCODE -ne 0) {
    throw "git ls-files failed in $root"
}
$files = @($tracked | Where-Object { Test-MainSource $_ })
$found = @(foreach ($file in $files) {
    $text = [string](Get-Content -Raw -LiteralPath (Join-Path $root $file))
    Find-DefaultLocaleCaseChange -Path $file -Text $text
})
if ($found.Count -gt 0) {
    $found | ForEach-Object { Write-Output $_ }
    Write-Output ("$($found.Count) case changes name no locale. Pass " +
        'Locale.ROOT, because the default locale can lower a capital I ' +
        'to a dotless i.')
    exit 1
}
Write-Output "Every case change in $($files.Count) main source files names its locale."
