[CmdletBinding()]
param(
    [string]$swigExec = "swig"
)

Write-Debug "Removing old files..."

Remove-Item ../java/fiftyone/ipintelligence/engine/onpremise/interop/swig/*.java
if (Test-Path -Path Java_Ipi_Engine.cpp -PathType Leaf) {
    Remove-Item Java_Ipi_Engine.cpp
}

Write-Debug "Invoking SWIG executable..."

& $swigExec `
    -c++ -java `
    -package fiftyone.ipintelligence.engine.onpremise.interop.swig `
    -module IpIntelligenceOnPremiseEngineModule `
    -outdir ../java/fiftyone/ipintelligence/engine/onpremise/interop/swig `
    -o Java_Ipi_Engine.cpp `
    hash_java.i

Write-Debug "LASTEXITCODE = $LASTEXITCODE"

exit $LASTEXITCODE
