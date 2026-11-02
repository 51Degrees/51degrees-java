# Shared by the scripts in this folder, which dot-source it. Paths are
# relative to the repository root.

# The package trees in this repository. The root pom.xml builds them all in
# one reactor but is not their parent, so a version change has to be made in
# each of them. Publish says whether the tree's packages go to Maven Central;
# the examples are built and tested but never published.
$Packages = @(
    @{ Path = 'pipeline'; Publish = $true }
    @{ Path = 'device-detection'; Publish = $true }
    @{ Path = 'ip-intelligence'; Publish = $true }
    @{ Path = 'location'; Publish = $true }
    @{ Path = 'examples/device-detection'; Publish = $false }
    @{ Path = 'examples/ip-intelligence'; Publish = $false }
)

# Modules that compile a native library during the build. Library matches the
# file names CMake gives the library on every platform, so the libraries built
# on each runner can be put back into the module they came from.
$NativePackages = @(
    @{ Path = 'device-detection/device-detection.hash.engine.on-premise'; Library = '*DeviceDetectionHashEngine-*' }
    @{ Path = 'ip-intelligence/ip-intelligence.engine.on-premise'; Library = '*IPIntelligenceOnPremiseEngine-*' }
)

# The strongly typed accessors generated from each product's data file
# metadata. DataType is the name the generator takes, Files are what it
# writes, and Destination is where they live in this repository.
$Accessors = @(
    @{
        DataType = 'HashV41'
        Files = 'DeviceDataBase.java', 'DeviceData.java'
        Destination = 'device-detection/device-detection.shared/src/main/java/fiftyone/devicedetection/shared'
    }
    @{
        DataType = 'IpIntelligence'
        Files = 'IPIntelligenceDataBase.java', 'IPIntelligenceData.java'
        Destination = 'ip-intelligence/ip-intelligence.shared/src/main/java/fiftyone/ipintelligence/shared'
    }
)

# Arguments every Maven call in these scripts passes.
$MavenOptions = '--batch-mode', '--no-transfer-progress'
