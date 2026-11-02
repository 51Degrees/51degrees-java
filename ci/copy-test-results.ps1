# Copies the Surefire reports of every module below Path into Destination.
#
# common-ci's Java scripts only look one folder down for modules, which finds
# the modules of a single package tree but not those of this repository, where
# each tree is a folder of its own and the examples are a folder further down.
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
