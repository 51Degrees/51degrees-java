param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

. "$PSScriptRoot/packages.ps1"

./java/install-package.ps1 @PSBoundParameters

# Temporary, on the dry run branch only. Lets common-ci's Package job run
# ci/publish-package.ps1 off main, which the dry run keeps from uploading or
# pushing anything.
$global:PublishAnyBranch = $true

if ($IsMacOS) {
    # Native libraries extracted from a downloaded jar can be refused by
    # Gatekeeper, which shows only as an engine that will not load. This shows
    # what the jars carry and clears the quarantine attributes, so a failure on
    # macOS can be told apart from a missing or wrong library.
    Write-Host "=== macOS native library check ==="
    Write-Host "JAVA_HOME: $env:JAVA_HOME"
    java -version 2>&1 | ForEach-Object { Write-Host "  $_" }
    Write-Host "os.arch property (what LibLoader sees):"
    java -XshowSettings:properties 2>&1 | Select-String "os.arch" | ForEach-Object { Write-Host "  $_" }
    Write-Host "Machine hardware (uname -m): $(uname -m)"

    $mvnLocalRepo = mvn help:evaluate -Dexpression="settings.localRepository" -q -DforceStdout
    Write-Host "Maven local repo: $mvnLocalRepo"

    $jarFiles = foreach ($package in $NativePackages) {
        $artifactId = Split-Path -Leaf $package.Path
        $jars = Get-ChildItem -Recurse "$mvnLocalRepo/com/51degrees/$artifactId" -Filter "*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch "sources|javadoc" }
        if (-not $jars) {
            Write-Host "WARNING: No $artifactId jar found in $mvnLocalRepo"
        }
        foreach ($jar in $jars) {
            Write-Host "Found $($jar.FullName), $($jar.Length) bytes, with native libraries:"
            $nativeLibs = jar -tf $jar.FullName 2>&1 | Where-Object { $_ -match "\.(dylib|so|dll)$" }
            if ($nativeLibs) {
                $nativeLibs | ForEach-Object { Write-Host "  $_" }
            } else {
                Write-Host "  WARNING: No native libraries found in the jar!"
            }
            $jar
        }
    }

    Write-Host "Disabling Gatekeeper for CI"
    try {
        sudo spctl --master-disable 2>&1
        Write-Host "Gatekeeper disabled"
    } catch {
        Write-Host "Note: Could not disable Gatekeeper: $_"
    }

    Write-Host "Removing quarantine attributes from the jars"
    Get-ChildItem -Recurse "$mvnLocalRepo/com/51degrees" -Filter "*.jar" -ErrorAction SilentlyContinue | ForEach-Object {
        xattr -d com.apple.quarantine $_.FullName 2>&1 | Out-Null
    }

    Write-Host "Extracting the native libraries to show their architecture"
    $tempDir = [System.IO.Path]::GetTempPath()
    Push-Location $tempDir
    try {
        foreach ($jar in $jarFiles) {
            jar -xf $jar.FullName 2>&1 | Out-Null
        }
        foreach ($lib in Get-ChildItem -Recurse -Filter "*.dylib" -ErrorAction SilentlyContinue) {
            Write-Host "Extracted: $($lib.FullName)"
            xattr -d com.apple.quarantine $lib.FullName 2>&1 | Out-Null
            chmod +x $lib.FullName 2>&1 | Out-Null
            file $lib.FullName
            lipo -info $lib.FullName 2>&1
        }
    } finally {
        Pop-Location
    }
    Write-Host "=== End of macOS native library check ==="
}
