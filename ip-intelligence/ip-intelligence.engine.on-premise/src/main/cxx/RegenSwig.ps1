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

    # SWIG converts a derived class pointer to its base class through a jlong
    # read as a pointer of another type, with no typemap to change it. Each
    # conversion is replaced with the functions hash_java.i defines, as
    # CMakeLists.txt does, and the run stops if one is left.
    $Wrapper = Join-Path $Temp "Java_Ipi_Engine.cpp"
    $Code = [IO.File]::ReadAllText($Wrapper)
    $Code = [regex]::Replace($Code,
        '\*\(([^\n]+) \*\*\)&baseptr = \*\(([^\n]+) \*\*\)&jarg1;',
        'baseptr = fiftyoneToJlong< $1 * >(fiftyoneFromJlong< $2 * >(jarg1));')
    if ($Code.Contains('&baseptr')) {
        Write-Error "A base class conversion in the SWIG wrapper was not replaced, so no file was changed." -ErrorAction Continue
        exit 1
    }
    [IO.File]::WriteAllText($Wrapper, $Code)

    # SWIG's own std_vector.i puts a this-escape annotation on the vector
    # constructors that take an array or an Iterable. hash_java.i makes the
    # vector proxies final, so javac has no escape to report, and the
    # annotation is removed from final classes only, as CMakeLists.txt does.
    # The run stops if any proxy still turns a javac warning off.
    foreach ($Proxy in Get-ChildItem (Join-Path $TempJava "*.java")) {
        $Java = [IO.File]::ReadAllText($Proxy.FullName)
        if ($Java.Contains("`npublic final class ")) {
            $Java = [regex]::Replace($Java,
                '  @SuppressWarnings\("this-escape"\)\n(  public [A-Za-z0-9_]+\()', '$1')
            [IO.File]::WriteAllText($Proxy.FullName, $Java)
        }
        if ($Java.Contains("@SuppressWarnings")) {
            Write-Error "$($Proxy.Name) turns a warning off, so no file was changed." -ErrorAction Continue
            exit 1
        }
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
