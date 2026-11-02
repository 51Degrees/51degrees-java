param (
    [string]$IpIntelligenceUrl
)
$ErrorActionPreference = "Stop"

$ipIntelligenceData = "$PSScriptRoot/../ip-intelligence-data"
$deviceDetectionData = "$PSScriptRoot/../device-detection-data"

./steps/fetch-assets.ps1 -IpIntelligenceUrl $IpIntelligenceUrl -Assets '51Degrees-EnterpriseIpiV41.ipi', '51Degrees-LiteIpiV41.ipi', '51Degrees-LiteV4.1.hash'
New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/51Degrees-EnterpriseIpiV41.ipi" -Path "$ipIntelligenceData/51Degrees-EnterpriseIpiV41.ipi"
# The asset is fetched under the blob name, but DataFileHelper and the data
# repo scripts use the unpacked name 51Degrees-LiteV41.ipi, so link it as that.
New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/51Degrees-LiteIpiV41.ipi" -Path "$ipIntelligenceData/51Degrees-LiteV41.ipi"
# The mixed examples need a device detection data file. Fetch the free Lite
# hash and link it where the mixed example tests look for it, otherwise those
# tests skip.
New-Item -ItemType Directory -Force -Path $deviceDetectionData | Out-Null
New-Item -ItemType SymbolicLink -Force -Target "$PWD/assets/51Degrees-LiteV4.1.hash" -Path "$deviceDetectionData/51Degrees-LiteV4.1.hash"

Write-Host "Assets hashes:"
Get-FileHash -Algorithm MD5 -Path assets/*
