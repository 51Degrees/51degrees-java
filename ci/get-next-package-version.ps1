param (
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$VariableName
)
$ErrorActionPreference = "Stop"

# The whole repository has one version, tagged as a plain version such as
# 4.5.127. Only the patch number moves here. The major and minor numbers
# change only when someone edits FirstVersion before the first release or
# pushes a tag by hand.

# The version used while the repository has no version tag. It is above every
# version the absorbed repositories published, so the first release from here
# is an ordinary upgrade for each package.
$FirstVersion = '4.5.127'

$tags = @(git -C $RepoName tag --list |
    Where-Object { $_ -match '^\d+\.\d+\.\d+$' } |
    Sort-Object { [version]$_ })
$headTags = @(git -C $RepoName tag --points-at HEAD |
    Where-Object { $_ -match '^\d+\.\d+\.\d+$' })

if ($headTags) {
    # Already released, so the publish sees the tag and does nothing.
    $next = $headTags | Sort-Object { [version]$_ } | Select-Object -Last 1
} elseif ($tags) {
    $highest = [version]$tags[-1]
    $next = "$($highest.Major).$($highest.Minor).$($highest.Build + 1)"
} else {
    $next = $FirstVersion
}

Set-Variable -Scope Global -Name $VariableName -Value $next
