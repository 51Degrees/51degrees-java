# Called by common-ci's steps/release-plan.ps1 with the files under a
# package's folders that changed since its last tag, once its Ignore globs are
# applied. Writes out the files whose change releases the package.
#
# A pom.xml counts only when something other than the project's descriptive
# metadata changed, so a new link, description or developer does not release
# the package, while a version, dependency, plugin or build change does.
# Comments and layout do not count either. Every other file counts.
param(
    [Parameter(Mandatory)][string]$RepoPath,
    # The package the files belong to. Not needed here, because a pom.xml is
    # judged the same way whichever package it belongs to.
    $Package,
    # The revision the package was last released from, normally its tag.
    [Parameter(Mandatory)][string]$From,
    [string[]]$Files = @()
)
$ErrorActionPreference = 'Stop'
# 'git show' fails for a file that is not in a revision, which is an answer
# here rather than an error.
$PSNativeCommandUseErrorActionPreference = $false

# The children of <project> that describe the project rather than build it.
$MetadataElements = @(
    'name', 'description', 'url', 'licenses', 'developers', 'scm',
    'issueManagement')

# The pom.xml at a revision with its comments, its layout and its metadata
# taken out, or $null when the revision has no such file or it is not XML.
function Get-PomBuildText([string]$Revision, [string]$File) {
    # git's message for a missing file is not an error of this script.
    $ErrorActionPreference = 'Continue'
    $text = & git -C $RepoPath show "${Revision}:$File" 2>$null
    $found = $LASTEXITCODE -eq 0
    $global:LASTEXITCODE = 0
    if ($found -eq $false) {
        return $null
    }
    $xml = [Xml.XmlDocument]::new()
    $xml.PreserveWhitespace = $false
    try {
        $xml.LoadXml(($text -join "`n"))
    } catch {
        return $null
    }
    foreach ($comment in @($xml.SelectNodes('//comment()'))) {
        $null = $comment.ParentNode.RemoveChild($comment)
    }
    $project = $xml.DocumentElement
    foreach ($child in @($project.ChildNodes)) {
        if ($child.NodeType -eq [Xml.XmlNodeType]::Element -and
                $MetadataElements -contains $child.LocalName) {
            $null = $project.RemoveChild($child)
        }
    }
    return $project.OuterXml
}

foreach ($file in $Files) {
    if (($file -replace '\\', '/') -notmatch '(^|/)pom\.xml$') {
        $file
        continue
    }
    $before = Get-PomBuildText $From $file
    $after = Get-PomBuildText 'HEAD' $file
    # A pom.xml that is new, removed or unreadable on either side counts.
    if ($null -eq $before -or $null -eq $after -or $before -ne $after) {
        $file
    }
}
