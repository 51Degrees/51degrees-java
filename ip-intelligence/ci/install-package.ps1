param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/install-package.ps1 @PSBoundParameters

if ($IsMacOS) {
    Write-Host "=== macOS Native Library Debug ==="

    # Check Java architecture - this is what LibLoader.getArch() will see
    Write-Host ""
    Write-Host "=== Java Architecture Debug ==="
    Write-Host "JAVA_HOME: $env:JAVA_HOME"
    Write-Host "Java version:"
    java -version 2>&1 | ForEach-Object { Write-Host "  $_" }
    Write-Host "os.arch property (what LibLoader sees):"
    java -XshowSettings:properties 2>&1 | Select-String "os.arch" | ForEach-Object { Write-Host "  $_" }
    Write-Host "Machine hardware (uname -m):"
    Write-Host "  $(uname -m)"
    Write-Host ""

    # Get maven local repo path
    $mvnLocalRepo = mvn help:evaluate -Dexpression="settings.localRepository" -q -DforceStdout
    Write-Host "Maven local repo: $mvnLocalRepo"

    # Find the on-premise JAR
    Write-Host "Searching for ip-intelligence.engine.on-premise JAR..."
    $jarFiles = Get-ChildItem -Recurse "$mvnLocalRepo/com/51degrees/ip-intelligence.engine.on-premise" -Filter "*.jar" -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -notmatch "sources|javadoc" }

    if ($jarFiles) {
        foreach ($jar in $jarFiles) {
            Write-Host "Found JAR: $($jar.FullName)"
            Write-Host "JAR size: $($jar.Length) bytes"

            # List native libraries in JAR
            Write-Host "Native libraries in JAR:"
            $nativeLibs = jar -tf $jar.FullName 2>&1 | Where-Object { $_ -match "\.(dylib|so|dll)$" }
            if ($nativeLibs) {
                $nativeLibs | ForEach-Object { Write-Host "  $_" }
            } else {
                Write-Host "  WARNING: No native libraries found in JAR!"
            }
        }
    } else {
        Write-Host "WARNING: No ip-intelligence.engine.on-premise JAR found in $mvnLocalRepo"
        Write-Host "Directory contents:"
        Get-ChildItem -Recurse "$mvnLocalRepo/com/51degrees" -ErrorAction SilentlyContinue | Select-Object -First 20 | ForEach-Object { Write-Host "  $($_.FullName)" }
    }

    # Disable Gatekeeper to allow unsigned native libraries to load
    Write-Host ""
    Write-Host "=== Disabling macOS Gatekeeper for CI ==="
    try {
        # This disables Gatekeeper assessment (requires sudo on CI runners)
        sudo spctl --master-disable 2>&1
        Write-Host "Gatekeeper disabled successfully"
    } catch {
        Write-Host "Note: Could not disable Gatekeeper: $_"
    }

    # Also remove quarantine from the JAR files themselves
    Write-Host "Removing quarantine attributes from JAR files..."
    Get-ChildItem -Recurse "$mvnLocalRepo/com/51degrees" -Filter "*.jar" -ErrorAction SilentlyContinue | ForEach-Object {
        xattr -d com.apple.quarantine $_.FullName 2>&1 | Out-Null
    }
    Write-Host "Quarantine attributes removed"

    # Pre-extract native library and remove quarantine
    Write-Host ""
    Write-Host "=== Pre-extracting native library ==="
    $tempDir = [System.IO.Path]::GetTempPath()
    Write-Host "Temp directory: $tempDir"

    if ($jarFiles) {
        $jar = $jarFiles | Select-Object -First 1
        Push-Location $tempDir
        try {
            # Extract dylib files from JAR
            jar -xf $jar.FullName 2>&1 | Out-Null
            $extractedLibs = Get-ChildItem -Recurse -Filter "*.dylib" -ErrorAction SilentlyContinue
            if ($extractedLibs) {
                foreach ($lib in $extractedLibs) {
                    Write-Host "Extracted: $($lib.FullName)"
                    # Remove quarantine
                    xattr -d com.apple.quarantine $lib.FullName 2>&1 | Out-Null
                    # Make executable
                    chmod +x $lib.FullName 2>&1 | Out-Null
                    # Show file info
                    file $lib.FullName
                    # Show architecture
                    lipo -info $lib.FullName 2>&1
                }

                # Check symbols in ARM64 library
                $arm64Lib = $extractedLibs | Where-Object { $_.Name -match "aarch64" } | Select-Object -First 1
                if ($arm64Lib) {
                    Write-Host ""
                    Write-Host "=== Checking JNI symbols in ARM64 library ==="
                    Write-Host "Looking for ConfigIpiSwig symbol:"
                    nm -g $arm64Lib.FullName 2>&1 | Select-String "ConfigIpiSwig" | Select-Object -First 5
                    Write-Host ""
                    Write-Host "Total exported symbols:"
                    $symbolCount = (nm -g $arm64Lib.FullName 2>&1 | Measure-Object -Line).Lines
                    Write-Host "  $symbolCount symbols"
                    Write-Host ""
                    Write-Host "Sample JNI symbols (first 10):"
                    nm -g $arm64Lib.FullName 2>&1 | Select-String "Java_" | Select-Object -First 10
                }
            } else {
                Write-Host "No .dylib files extracted"
            }
        } finally {
            Pop-Location
        }
    }

    Write-Host "=== End macOS Native Library Debug ==="
}

