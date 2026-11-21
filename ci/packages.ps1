# Shared by the scripts in this folder, which dot-source it. Paths are
# relative to the repository root.

# The package trees in this repository. The root pom.xml builds them all in
# one reactor but is not their parent. Publish says whether the tree's
# packages go to Maven Central. The examples are built and tested but never
# published. Each published module is a package of its own in packages.json.
# The example modules inside the published trees are packages too, and give
# an Ignore glob over their whole folder, so a change to one releases nothing.
$Trees = @(
    @{ Path = 'pipeline'; Publish = $true }
    @{ Path = 'device-detection'; Publish = $true }
    @{ Path = 'ip-intelligence'; Publish = $true }
    @{ Path = 'location'; Publish = $true }
    @{ Path = 'examples/device-detection'; Publish = $false }
    @{ Path = 'examples/ip-intelligence'; Publish = $false }
)

# Modules that compile a native library during the build. Library matches the
# file names CMake gives the library on every platform, so the libraries built
# on each runner can be put back into the module they came from.
$NativePackages = @(
    @{ Path = 'device-detection/device-detection.hash.engine.on-premise'; Library = '*DeviceDetectionHashEngine-*' }
    @{ Path = 'ip-intelligence/ip-intelligence.engine.on-premise'; Library = '*IPIntelligenceOnPremiseEngine-*' }
)

# The strongly typed accessors generated from each product's data file
# metadata. DataType is the name the generator takes, Files are what it
# writes, and Destination is where they live in this repository.
$Accessors = @(
    @{
        DataType = 'HashV41'
        Files = 'DeviceDataBase.java', 'DeviceData.java'
        Destination = 'device-detection/device-detection.shared/src/main/java/fiftyone/devicedetection/shared'
    }
    @{
        DataType = 'IpIntelligence'
        Files = 'IPIntelligenceDataBase.java', 'IPIntelligenceData.java'
        Destination = 'ip-intelligence/ip-intelligence.shared/src/main/java/fiftyone/ipintelligence/shared'
    }
)

# Arguments every Maven call in these scripts passes.
$MavenOptions = '--batch-mode', '--no-transfer-progress'

$PomNamespace = 'http://maven.apache.org/POM/4.0.0'

# The packages in packages.json that are published, one per Maven module,
# each named by its artifactId.
function Get-PublishedPackages([string]$RepoPath) {
    return @(Get-Content -Raw (Join-Path $RepoPath 'ci/packages.json') |
        ConvertFrom-Json | Where-Object { $_.Publish -eq $true })
}

# A parent's Path is its pom.xml alone, because its folder also holds its
# modules, whose changes are not changes to the parent.
function Get-PomPath($Package) {
    if ($Package.Path.EndsWith('/pom.xml') -eq $true) {
        return $Package.Path
    }
    return "$($Package.Path)/pom.xml"
}

# The folder Maven builds a package in.
function Get-ModulePath($Package) {
    return (Get-PomPath $Package) -replace '/pom\.xml$', ''
}

# A pom.xml read with its whitespace kept, so that writing it back changes
# only what was set.
function Read-Pom([string]$Path) {
    # .NET resolves a relative path from the process's folder, which is not
    # the folder PowerShell is in after a Set-Location.
    $Path = (Resolve-Path -LiteralPath $Path).ProviderPath
    $xml = [Xml.XmlDocument]::new()
    $xml.PreserveWhitespace = $true
    $xml.Load($Path)
    $namespaces = [Xml.XmlNamespaceManager]::new($xml.NameTable)
    $namespaces.AddNamespace('m', $PomNamespace)
    return [pscustomobject]@{ Path = $Path; Xml = $xml; Ns = $namespaces }
}

function Save-Pom($Pom) {
    $settings = [Xml.XmlWriterSettings]::new()
    $settings.Encoding = [Text.UTF8Encoding]::new($false)
    $settings.NewLineHandling = [Xml.NewLineHandling]::None
    $writer = [Xml.XmlWriter]::Create($Pom.Path, $settings)
    try {
        $Pom.Xml.Save($writer)
    } finally {
        $writer.Dispose()
    }
}

function Get-PomText($Node, [string]$Name, $Ns) {
    $child = $Node.SelectSingleNode("m:$Name", $Ns)
    if ($null -eq $child) {
        return $null
    }
    return $child.InnerText.Trim()
}

# The other packages a pom.xml refers to, being its parent and its
# dependencies on them in any scope, wherever they are declared. Version is the element that
# holds the version, or $null where a parent pom manages it.
function Get-PomReferences($Pom, [string[]]$Names) {
    $self = Get-PomText $Pom.Xml.DocumentElement 'artifactId' $Pom.Ns
    $parent = $Pom.Xml.SelectSingleNode('/m:project/m:parent', $Pom.Ns)
    if ($null -ne $parent) {
        $name = Get-PomText $parent 'artifactId' $Pom.Ns
        if ($Names -contains $name) {
            [pscustomobject]@{
                Name = $name
                Version = $parent.SelectSingleNode('m:version', $Pom.Ns)
            }
        }
    }
    foreach ($dependency in $Pom.Xml.SelectNodes('//m:dependency', $Pom.Ns)) {
        $name = Get-PomText $dependency 'artifactId' $Pom.Ns
        $group = Get-PomText $dependency 'groupId' $Pom.Ns
        if ($Names -contains $name -and $name -ne $self -and
                $group -in 'com.51degrees', '${project.groupId}') {
            [pscustomobject]@{
                Name = $name
                Version = $dependency.SelectSingleNode('m:version', $Pom.Ns)
            }
        }
    }
}

# The modules of the published trees, as folders, read from each pom's
# modules.
function Get-PublishedModules([string]$RepoPath) {
    $folders = [Collections.Generic.Queue[string]]::new()
    foreach ($tree in $Trees | Where-Object { $_.Publish -eq $true }) {
        $folders.Enqueue($tree.Path)
    }
    while ($folders.Count -gt 0) {
        $folder = $folders.Dequeue()
        $folder
        $pom = Read-Pom (Join-Path $RepoPath $folder 'pom.xml')
        foreach ($module in $pom.Xml.SelectNodes('/m:project/m:modules/m:module', $pom.Ns)) {
            $folders.Enqueue("$folder/$($module.InnerText.Trim())")
        }
    }
}

# What packages.json leaves out, as one line each. Every published module must
# be a package, named by its artifactId, and each package's DependsOn must name
# every other package its pom.xml refers to. A dependency it leaves out would
# not be released when the package it depends on is, so its users would not
# get the fix.
function Get-PackagesJsonGaps([string]$RepoPath) {
    $packages = Get-PublishedPackages $RepoPath
    $names = @($packages.Name)
    $folders = @($packages | ForEach-Object { Get-ModulePath $_ })
    foreach ($folder in Get-PublishedModules $RepoPath) {
        if ($folders -notcontains $folder) {
            "$folder/pom.xml is a published module with no entry in ci/packages.json"
        }
    }
    foreach ($package in $packages) {
        $pom = Read-Pom (Join-Path $RepoPath (Get-PomPath $package))
        $artifactId = Get-PomText $pom.Xml.DocumentElement 'artifactId' $pom.Ns
        if ($artifactId -ne $package.Name) {
            "$(Get-PomPath $package) is $artifactId, but ci/packages.json names it $($package.Name)"
        }
        $missing = @(Get-PomReferences $pom $names |
            Where-Object { $package.DependsOn -notcontains $_.Name } |
            ForEach-Object Name | Select-Object -Unique)
        foreach ($name in $missing) {
            "$(Get-PomPath $package) refers to $name, which the DependsOn of $($package.Name) in ci/packages.json does not name"
        }
    }
}
