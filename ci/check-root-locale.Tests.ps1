# Tests for ci/check-root-locale.ps1.

BeforeAll {
    $script:Check = Join-Path $PSScriptRoot 'check-root-locale.ps1'
    . $script:Check
}

Describe 'Find-DefaultLocaleCaseChange' {

    It 'finds <Line>' -ForEach @(
        @{ Line = 'String key = property.getName().toLowerCase();' }
        @{ Line = 'if (name.toUpperCase().equals(other)) {' }
        @{ Line = 'key.toLowerCase( ).contains("x")' }
    ) {
        Find-DefaultLocaleCaseChange -Path 'A.java' -Text $Line |
            Should -Be "A.java:1: $Line"
    }

    It 'leaves <Line> alone' -ForEach @(
        @{ Line = 'String key = property.getName().toLowerCase(Locale.ROOT);' }
        @{ Line = 'name.toUpperCase(Locale.ENGLISH)' }
        @{ Line = 'a.equalsIgnoreCase(b)' }
        @{ Line = 'Character.toLowerCase(c)' }
    ) {
        Find-DefaultLocaleCaseChange -Path 'A.java' -Text $Line |
            Should -BeNullOrEmpty
    }

    It 'gives each line number' {
        $text = "class A {`n  String a = b.toLowerCase();`r`n  String c = d;`n" +
            "  String e = f.toUpperCase();`n}"
        Find-DefaultLocaleCaseChange -Path 'p/A.java' -Text $text |
            Should -Be @(
                'p/A.java:2: String a = b.toLowerCase();'
                'p/A.java:4: String e = f.toUpperCase();')
    }

    It 'finds nothing in an empty file' {
        Find-DefaultLocaleCaseChange -Path 'A.java' -Text '' |
            Should -BeNullOrEmpty
    }
}

Describe 'Test-MainSource' {

    It 'reads <Path>' -ForEach @(
        @{ Path = 'pipeline/pipeline.core/src/main/java/a/B.java' }
        @{ Path = 'src/main/java/B.java' }
        @{ Path = 'examples/device-detection/shared/src/main/java/a/B.java' }
    ) {
        Test-MainSource $Path | Should -BeTrue
    }

    It 'leaves out <Path>' -ForEach @(
        @{ Path = 'pipeline/pipeline.core/src/test/java/a/B.java' }
        @{ Path = 'pipeline/pipeline.core/src/main/resources/a.json' }
        @{ Path = 'ci/check-root-locale.ps1' }
        @{ Path = 'docs/src/main.java.md' }
    ) {
        Test-MainSource $Path | Should -BeFalse
    }
}

Describe 'The check' {

    BeforeAll {
        $script:Repo = Join-Path ([IO.Path]::GetTempPath()) ([guid]::NewGuid())
        New-Item -ItemType Directory -Path $script:Repo | Out-Null
        & git -C $script:Repo init -q
        $main = Join-Path $script:Repo 'm/src/main/java'
        $test = Join-Path $script:Repo 'm/src/test/java'
        New-Item -ItemType Directory -Path $main, $test | Out-Null
        Set-Content -LiteralPath (Join-Path $test 'T.java') `
            -Value 'class T { String a = b.toLowerCase(); }'
        Set-Content -LiteralPath (Join-Path $main 'M.java') `
            -Value 'class M { String a = b.toLowerCase(Locale.ROOT); }'
        & git -C $script:Repo add -A
    }

    AfterAll {
        Remove-Item -LiteralPath $script:Repo -Recurse -Force
    }

    It 'passes when every main case change names its locale' {
        $output = & pwsh -NoProfile -File $script:Check -RepoRoot $script:Repo
        $LASTEXITCODE | Should -Be 0
        ($output -join "`n") | Should -Match 'Every case change in 1 main'
    }

    It 'fails on a main case change with no locale, naming the line' {
        Set-Content -LiteralPath (Join-Path $script:Repo 'm/src/main/java/N.java') `
            -Value "class N {`n  String a = b.toUpperCase();`n}"
        & git -C $script:Repo add -A
        $output = & pwsh -NoProfile -File $script:Check -RepoRoot $script:Repo
        $LASTEXITCODE | Should -Be 1
        $output | Should -Contain 'm/src/main/java/N.java:2: String a = b.toUpperCase();'
    }
}
