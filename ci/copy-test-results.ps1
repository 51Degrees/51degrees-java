# Copies the Surefire reports of every module below Path into Destination.
#
# common-ci's Java scripts copy the reports of the one Maven run they make. The
# integration and performance tests here make several runs, each in a tree of
# its own and each reporting into the same folder, so those two scripts copy
# the reports themselves. The unit tests are one run, which common-ci makes.
param (
    [Parameter(Mandatory)][string]$Path,
    [Parameter(Mandatory)][string]$Destination,
    [string]$Filter = '*',
    [string]$Exclude
)
$ErrorActionPreference = "Stop"

$destinationDir = New-Item -ItemType Directory -Force -Path $Destination

Get-ChildItem -Path $Path -Recurse -Directory -Filter 'surefire-reports' |
    Where-Object { $_.Parent.Name -eq 'target' -and $_.FullName -notmatch '[\\/]cxx[\\/]' } |
    ForEach-Object {
        Get-ChildItem -Path $_.FullName -File -Filter $Filter |
            Where-Object { -not $Exclude -or $_.Name -notlike $Exclude } |
            Copy-Item -Destination $destinationDir
    }
