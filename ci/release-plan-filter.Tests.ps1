# Pester 5 tests of release-plan-filter.ps1. Each test makes a repository
# whose pom.xml is tagged as released, changes it in a second commit, and asks
# the filter whether the change counts.

BeforeAll {
    $Filter = Join-Path $PSScriptRoot 'release-plan-filter.ps1'

    $Released = @'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.51degrees</groupId>
    <artifactId>pipeline.core</artifactId>
    <version>4.5.130</version>
    <name>51Degrees :: Pipeline :: Core</name>
    <description>Pipeline core</description>
    <url>https://example.com</url>
    <licenses>
        <license><name>EUPL-1.2</name></license>
    </licenses>
    <developers>
        <developer><name>51Degrees</name></developer>
    </developers>
    <scm>
        <url>https://github.com/51Degrees/pipeline-java</url>
    </scm>
    <issueManagement>
        <url>https://github.com/51Degrees/pipeline-java/issues</url>
    </issueManagement>
    <repositories>
        <repository>
            <id>central</id>
            <url>https://repo1.maven.org/maven2</url>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20240303</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
            </plugin>
        </plugins>
    </build>
</project>
'@

    # A repository holding $Released at the tag 'pkg/4.5.130', then the given
    # files in a second commit. Returns the repository's folder.
    function New-ChangedRepo([hashtable]$Files, [string[]]$Remove = @()) {
        $repo = Join-Path $TestDrive ([Guid]::NewGuid().ToString('N'))
        $null = New-Item -ItemType Directory -Path (Join-Path $repo 'pkg')
        Set-Content -LiteralPath (Join-Path $repo 'pkg/pom.xml') -Value $Released -NoNewline
        Set-Content -LiteralPath (Join-Path $repo 'pkg/Core.java') -Value 'class Core {}' -NoNewline
        git -C $repo init -q
        git -C $repo config user.email 'test@example.com'
        git -C $repo config user.name 'Test'
        git -C $repo config core.autocrlf false
        git -C $repo add -A
        git -C $repo commit -q -m 'released'
        git -C $repo tag 'pkg/4.5.130'
        foreach ($path in $Files.Keys) {
            $full = Join-Path $repo $path
            $null = New-Item -ItemType Directory -Force -Path (Split-Path $full)
            Set-Content -LiteralPath $full -Value $Files[$path] -NoNewline
        }
        foreach ($path in $Remove) {
            git -C $repo rm -q $path
        }
        git -C $repo add -A
        git -C $repo commit -q --allow-empty -m 'changed'
        return $repo
    }

    # The released pom with one piece of text replaced, which must be there.
    function Edit-Released([string]$Old, [string]$New) {
        if ($Released.Contains($Old) -eq $false) {
            throw "The released pom has no '$Old'."
        }
        return $Released.Replace($Old, $New)
    }

    function Invoke-Filter([string]$Repo, [string[]]$Files) {
        return @(& $Filter -RepoPath $Repo -Package 'pkg' -From 'pkg/4.5.130' -Files $Files)
    }
}

Describe 'release-plan-filter.ps1' {
    It 'passes a file that is not a pom.xml through' {
        $repo = New-ChangedRepo @{ 'pkg/Core.java' = 'class Core { int a; }' }
        Invoke-Filter $repo @('pkg/Core.java') | Should -Be @('pkg/Core.java')
    }

    It 'leaves out a pom.xml whose <metadata> changed' -ForEach @(
        @{ Metadata = 'name'; Old = 'Pipeline :: Core</name>'; New = 'Pipeline :: Core API</name>' }
        @{ Metadata = 'description'; Old = 'Pipeline core</description>'; New = 'The Pipeline core</description>' }
        @{ Metadata = 'url'; Old = '<url>https://example.com</url>'; New = '<url>https://example.com/documentation</url>' }
        @{ Metadata = 'licenses'; Old = '<name>EUPL-1.2</name>'; New = '<name>EUPL 1.2</name>' }
        @{ Metadata = 'developers'; Old = '<developer><name>51Degrees</name></developer>'; New = '<developer><name>51Degrees Mobile Experts</name></developer>' }
        @{ Metadata = 'scm'; Old = 'pipeline-java</url>'; New = '51degrees-java</url>' }
        @{ Metadata = 'issueManagement'; Old = 'pipeline-java/issues'; New = '51degrees-java/issues' }
    ) {
        $repo = New-ChangedRepo @{ 'pkg/pom.xml' = (Edit-Released $Old $New) }
        Invoke-Filter $repo @('pkg/pom.xml') | Should -BeNullOrEmpty
    }

    It 'leaves out a pom.xml whose only change is a comment or its layout' {
        $pom = Edit-Released '<modelVersion>4.0.0</modelVersion>' `
            "<!-- The model version. -->`n        <modelVersion>4.0.0</modelVersion>`n"
        $repo = New-ChangedRepo @{ 'pkg/pom.xml' = $pom }
        Invoke-Filter $repo @('pkg/pom.xml') | Should -BeNullOrEmpty
    }

    It 'keeps a pom.xml whose <change> changed' -ForEach @(
        @{ Change = 'version'; Old = '<version>4.5.130</version>'; New = '<version>4.5.131</version>' }
        @{ Change = 'dependency version'; Old = '<version>20240303</version>'; New = '<version>20250107</version>' }
        @{ Change = 'dependencies'; Old = '</dependencies>'; New = '<dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId></dependency></dependencies>' }
        @{ Change = 'plugin'; Old = '<version>3.3.0</version>'; New = '<version>3.4.1</version>' }
        @{ Change = 'build'; Old = '<build>'; New = '<build><finalName>core</finalName>' }
        @{ Change = 'repository url'; Old = 'https://repo1.maven.org/maven2'; New = 'https://repo.maven.apache.org/maven2' }
    ) {
        $repo = New-ChangedRepo @{ 'pkg/pom.xml' = (Edit-Released $Old $New) }
        Invoke-Filter $repo @('pkg/pom.xml') | Should -Be @('pkg/pom.xml')
    }

    It 'keeps a pom.xml whose metadata and dependencies both changed' {
        $pom = (Edit-Released 'Pipeline core</description>' 'The Pipeline core</description>').
            Replace('<version>20240303</version>', '<version>20250107</version>')
        $repo = New-ChangedRepo @{ 'pkg/pom.xml' = $pom }
        Invoke-Filter $repo @('pkg/pom.xml') | Should -Be @('pkg/pom.xml')
    }

    It 'keeps a pom.xml that is new since the release' {
        $repo = New-ChangedRepo @{ 'pkg/sub/pom.xml' = $Released }
        Invoke-Filter $repo @('pkg/sub/pom.xml') | Should -Be @('pkg/sub/pom.xml')
    }

    It 'keeps a pom.xml that was removed since the release' {
        $repo = New-ChangedRepo @{} -Remove @('pkg/pom.xml')
        Invoke-Filter $repo @('pkg/pom.xml') | Should -Be @('pkg/pom.xml')
    }

    It 'judges each file on its own' {
        $repo = New-ChangedRepo @{
            'pkg/pom.xml' = (Edit-Released 'Pipeline core</description>' 'The Pipeline core</description>')
            'pkg/Core.java' = 'class Core { int a; }'
        }
        Invoke-Filter $repo @('pkg/pom.xml', 'pkg/Core.java') | Should -Be @('pkg/Core.java')
    }

    It 'gives nothing back for no files' {
        $repo = New-ChangedRepo @{}
        Invoke-Filter $repo @() | Should -BeNullOrEmpty
    }
}
