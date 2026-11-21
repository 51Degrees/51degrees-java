# Dot-sourced by the scripts that publish. Each package in packages.json has
# its own version, tagged '<package>/<version>'. A publish releases a package
# when a file under its folders changed since its last tag, and every package
# that depends on it, with the patch number moved. common-ci's
# steps/release-plan.ps1 works out the plan, and the functions here build it.
#
# Kept apart from packages.ps1, which every script loads, so that building and
# testing a pull request does not need the release plan.
. "$PSScriptRoot/packages.ps1"

# common-ci runs these scripts from its own root, with this repository checked
# out inside it. A checkout of common-ci beside this repository is found too,
# or one elsewhere can be named with COMMON_CI.
$releasePlan = @(
    $env:COMMON_CI,
    (Join-Path $PSScriptRoot '..' '..'),
    (Join-Path $PSScriptRoot '..' '..' 'common-ci')) |
    Where-Object { $_ } |
    ForEach-Object { Join-Path $_ 'steps' 'release-plan.ps1' } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1
if ($null -eq $releasePlan) {
    throw "common-ci's steps/release-plan.ps1 was not found. Set COMMON_CI " +
        "to a checkout of common-ci."
}
. $releasePlan
Remove-Variable releasePlan

# The examples' properties that name the version of a package they use, which
# the publish sets to the version it built.
$ExampleVersionProperties = @{
    'device-detection.version' = 'device-detection'
    'ip-intelligence.version' = 'ip-intelligence'
    'pipeline.jsonbuilder.version' = 'pipeline.jsonbuilder'
}

# The packages a publish builds. When nothing is to be released, every package
# is built at its released version, so that the tests have packages to install
# and the nightly publish passes without publishing anything.
function Get-BuildPlan([string]$RepoPath) {
    $plan = @(Get-ReleasePlan $RepoPath)
    if ($plan.Count -gt 0) {
        return $plan
    }
    $published = Get-PublishedPackages $RepoPath
    foreach ($name in (Sort-ByDependency $published @($published.Name))) {
        $package = $published | Where-Object { $_.Name -eq $name }
        $last = @(Get-ReleasedVersions $RepoPath $name)[-1]
        [pscustomobject]@{
            Name = $name
            Path = $package.Path
            Version = "$last"
            Previous = $last
            Reason = 'already released, built for the tests'
        }
    }
}

# The version of every published package in a build, which is its version in
# the plan, or its last release when the plan leaves it out.
function Get-BuildVersions([string]$RepoPath, $Plan = $null) {
    if ($null -eq $Plan) {
        $Plan = @(Get-BuildPlan $RepoPath)
    }
    $versions = @{}
    foreach ($package in Get-PublishedPackages $RepoPath) {
        $release = $Plan | Where-Object { $_.Name -eq $package.Name }
        if ($null -ne $release) {
            $versions[$package.Name] = $release.Version
            continue
        }
        $released = @(Get-ReleasedVersions $RepoPath $package.Name)
        if ($released.Count -eq 0) {
            throw "$($package.Name) has never been released and is not in the plan."
        }
        $versions[$package.Name] = "$($released[-1])"
    }
    return $versions
}

# Writes into every published pom.xml its own version, its parent's and those
# of the packages it depends on, so each module is built at its own version and
# its published pom requires the versions it was built with. The poms in the
# repository keep one development version for every module, which this
# replaces only in the checkout being built.
function Set-PomVersions([string]$RepoPath, [hashtable]$Versions) {
    $gaps = @(Get-PackagesJsonGaps $RepoPath)
    if ($gaps.Count -gt 0) {
        throw "ci/packages.json does not match the poms:`n$($gaps -join "`n")"
    }
    $packages = Get-PublishedPackages $RepoPath
    $names = @($packages.Name)
    foreach ($package in $packages) {
        $pom = Read-Pom (Join-Path $RepoPath (Get-PomPath $package))
        $project = $pom.Xml.DocumentElement
        $version = $project.SelectSingleNode('m:version', $pom.Ns)
        if ($null -eq $version) {
            # The module took its version from its parent. It gets an element
            # of its own after its artifactId, indented as the artifactId is.
            $artifactId = $project.SelectSingleNode('m:artifactId', $pom.Ns)
            $version = $pom.Xml.CreateElement('version', $PomNamespace)
            $null = $project.InsertAfter($version, $artifactId)
            $indent = $artifactId.PreviousSibling
            if ($null -ne $indent -and
                    $indent.NodeType -eq [Xml.XmlNodeType]::Whitespace) {
                $null = $project.InsertAfter($indent.Clone(), $artifactId)
            }
        }
        $version.InnerText = $Versions[$package.Name]
        foreach ($reference in Get-PomReferences $pom $names) {
            if ($null -ne $reference.Version) {
                $reference.Version.InnerText = $Versions[$reference.Name]
            }
        }
        Save-Pom $pom
    }
}

# The arguments that point the examples at the versions a publish built.
function Get-ExampleVersionArguments([string]$RepoPath) {
    $versions = Get-BuildVersions $RepoPath
    foreach ($property in $ExampleVersionProperties.Keys | Sort-Object) {
        "-D$property=$($versions[$ExampleVersionProperties[$property]])"
    }
}
