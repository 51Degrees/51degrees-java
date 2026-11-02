# Makes sure WMIC is installed on Windows, so that a forked test JVM can tell
# whether Maven is still alive by looking at the Maven process.
#
# Maven Surefire runs the tests in a forked JVM. The fork has to notice when
# Maven has died so that it does not run on forever. On Windows it does that by
# asking WMIC for the start time of the Maven process. Recent Windows images do
# not include WMIC. Without it Surefire writes "Cannot use PPID ... Going to
# use NOOP events" to its dump file and falls back to expecting a message from
# Maven at least every 30 seconds. That limit is fixed in Surefire and cannot
# be configured.
#
# IPIntelligenceTests copies and loads the Enterprise data file, which is
# several GB, ten times over. On Windows that keeps the machine busy for about
# twenty minutes, long enough for those messages to be delayed. The fork then
# ends itself with exit code 1 and Maven reports "The forked VM terminated
# without properly saying goodbye" for a test that had not failed.
#
# Installing WMIC is a precaution, so a failure to install it is reported as a
# warning and never fails the job. The tests then run as they did before.

# A failing installer must reach the check below rather than throw here, as
# the caller turns native command failures into errors.
$PSNativeCommandUseErrorActionPreference = $false

# Surefire runs this exact path, so WMIC being somewhere else on the PATH
# would not help.
$WmicPath = "$env:SystemRoot\System32\Wbem\wmic.exe"

if ($IsWindows -eq $false) {
    # Surefire only uses WMIC on Windows. Elsewhere it uses ps, which is
    # always there.
    return
}

if ((Test-Path -Path $WmicPath) -eq $true) {
    Write-Host "WMIC found at '$WmicPath', nothing to install."
    return
}

Write-Host "WMIC not found at '$WmicPath', installing it."
$installed = $false
try {
    dism.exe /Online /Add-Capability /CapabilityName:WMIC~~~~ /NoRestart |
        Out-Host
    $installed = $LASTEXITCODE -eq 0
}
catch {
    Write-Host "Installing WMIC threw: $($_.Exception.Message)"
}
finally {
    # The common-ci step runner fails the step if a non zero exit code is
    # left behind, and a failure here has been dealt with.
    $global:LASTEXITCODE = 0
}

if ($installed -eq $true -and (Test-Path -Path $WmicPath) -eq $true) {
    Write-Host "WMIC installed at '$WmicPath'."
}
else {
    Write-Host ("$($env:CI -eq 'true' ? '::warning::' : '')" +
        "WMIC could not be installed at '$WmicPath'. Maven Surefire will " +
        "fall back to its 30 second ping, which long running tests can miss.")
}
