param (
    [string]$DeviceDetection,
    [string]$DeviceDetectionUrl,
    [string]$IpIntelligence,
    [string]$IpIntelligenceUrl
)
$ErrorActionPreference = "Stop"

# Tests and examples find data files by searching below the repository root,
# so every asset is linked into the data folders of the cxx submodule.
$deviceDetectionData = "$PSScriptRoot/../cxx/device-detection-data"
$ipIntelligenceData = "$PSScriptRoot/../cxx/ip-intelligence-data"

$deviceDetectionAssets = 'TAC-HashV41.hash', '51Degrees-LiteV4.1.hash', '20000 Evidence Records.yml', '20000 User Agents.csv'
./steps/fetch-assets.ps1 -DeviceDetection:$DeviceDetection -DeviceDetectionUrl:$DeviceDetectionUrl -Assets $deviceDetectionAssets
foreach ($asset in $deviceDetectionAssets) {
    New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/$asset" -Path "$deviceDetectionData/$asset"
}

./steps/fetch-assets.ps1 -IpIntelligence:$IpIntelligence -IpIntelligenceUrl:$IpIntelligenceUrl -Assets '51Degrees-EnterpriseIpiV41.ipi', '51Degrees-LiteIpiV41.ipi'
New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/51Degrees-EnterpriseIpiV41.ipi" -Path "$ipIntelligenceData/51Degrees-EnterpriseIpiV41.ipi"
# The asset is fetched under the blob name, but the tests, DataFileHelper and
# the data repository scripts use the unpacked name 51Degrees-LiteV41.ipi, so
# link it as that.
New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/51Degrees-LiteIpiV41.ipi" -Path "$ipIntelligenceData/51Degrees-LiteV41.ipi"

Write-Host "Assets hashes:"
Get-FileHash -Algorithm MD5 -Path assets/*

# Generate the evidence files used by the IP Intelligence tests and examples.
Push-Location $ipIntelligenceData
try {
    ./evidence-gen.ps1 -v4 10000 -v6 10000
    ./evidence-gen.ps1 -v4 10000 -v6 10000 -csv -path "evidence.csv"
} finally {
    Pop-Location
}
