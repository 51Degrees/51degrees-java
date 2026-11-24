[CmdletBinding()]
param(
    [string]$swigExec = "swig"
)

# SWIG writes into a new folder, and the committed wrapper and Java proxies
# are replaced only when it succeeds, so a failed run leaves them as they
# were.
$ErrorActionPreference = "Stop"
$JavaOut = "../java/fiftyone/ipintelligence/engine/onpremise/interop/swig"
$Temp = Join-Path ([IO.Path]::GetTempPath()) ([IO.Path]::GetRandomFileName())
$TempJava = Join-Path $Temp "java"
New-Item -ItemType Directory -Path $TempJava | Out-Null

try {
    Write-Debug "Invoking SWIG executable..."

    # Every SWIG warning is on and is an error, so a warning stops the run.
    & $swigExec `
        -Wall -Werror `
        -c++ -java `
        "-I../../../../.." `
        -package fiftyone.ipintelligence.engine.onpremise.interop.swig `
        -module IpIntelligenceOnPremiseEngineModule `
        -outdir $TempJava `
        -o (Join-Path $Temp "Java_Ipi_Engine.cpp") `
        hash_java.i
    $SwigExit = $LASTEXITCODE

    Write-Debug "LASTEXITCODE = $SwigExit"

    if ($SwigExit -ne 0) {
        Write-Error "SWIG failed with exit code $SwigExit, so no file was changed." -ErrorAction Continue
        exit $SwigExit
    }

    Write-Debug "Replacing the old files..."

    Remove-Item "$JavaOut/*.java"
    Move-Item (Join-Path $TempJava "*.java") $JavaOut
    Move-Item -Force (Join-Path $Temp "Java_Ipi_Engine.cpp") "Java_Ipi_Engine.cpp"
    exit 0
}
finally {
    Remove-Item -Recurse -Force $Temp
}
