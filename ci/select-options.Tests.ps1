# Pester 5 tests of select-options.ps1, which chooses the options.json
# entries a pull request is built on from the files it changes.

BeforeAll {
    $Script = Join-Path $PSScriptRoot 'select-options.ps1'
    $Options = Get-Content -Raw (Join-Path $PSScriptRoot 'options.json')

    $Package = 'pipeline/pipeline.core/src/main/java/fiftyone/A.java'
    $NativeFile = 'device-detection/device-detection.hash.engine.on-premise/' +
        'src/main/java/fiftyone/A.java'

    # The entries chosen for the changed files, in options.json order.
    function Select-Entries(
        [string[]]$Changed,
        [string[]]$CommentOnly = @()) {
        $entries = @(ConvertFrom-Json $Options -AsHashtable)
        @(& $Script -Options $entries -ChangedFiles $Changed `
            -CommentOnlyFiles $CommentOnly 6>$null)
    }

    function Select-Names([string[]]$Changed, [string[]]$CommentOnly = @()) {
        (Select-Entries $Changed $CommentOnly | ForEach-Object { $_.Name }) -join ','
    }
}

Describe 'select-options.ps1' {
    It 'builds the standard set for a change to a package' {
        Select-Names $Package | Should -Be 'Ubuntu_Java_17,Ubuntu_Java_8'
    }

    It 'adds Windows and Linux ARM for a change to a native module' {
        Select-Names $NativeFile |
            Should -Be 'Ubuntu_Java_17,Ubuntu_Java_8,Ubuntu_ARM_Java_17,Windows_Java_17'
    }

    It 'leaves a native module change to its comments to the standard set' {
        Select-Names $NativeFile $NativeFile | Should -Be 'Ubuntu_Java_17,Ubuntu_Java_8'
    }

    It 'adds Windows and Linux ARM at depth deep for the cxx submodule' {
        $entries = Select-Entries 'cxx'
        ($entries | ForEach-Object { $_.Name }) -join ',' |
            Should -Be 'Ubuntu_Java_17,Ubuntu_Java_8,Ubuntu_ARM_Java_17,Windows_Java_17'
        $entries | ForEach-Object { $_.Depth | Should -Be 'deep' }
    }

    It 'adds JDK 11 and 21 for a pom.xml change' {
        Select-Names 'pipeline/pipeline.core/pom.xml' |
            Should -Be 'Ubuntu_Java_21,Ubuntu_Java_17,Ubuntu_Java_11,Ubuntu_Java_8'
    }

    It 'builds every entry but macOS, at depth deep, for a change outside the package trees' {
        $entries = Select-Entries 'ci/run-unit-tests.ps1'
        ($entries | ForEach-Object { $_.Name }) -join ',' | Should -Be (
            'Ubuntu_Java_21,Ubuntu_Java_17,Ubuntu_Java_11,Ubuntu_Java_8,' +
            'Ubuntu_ARM_Java_17,Windows_Java_21,Windows_Java_17,' +
            'Windows_Java_11,Windows_Java_8')
        $entries | ForEach-Object { $_.Depth | Should -Be 'deep' }
    }

    It 'runs the Selenium contract on Ubuntu JDK 17 alone, and no performance tests' {
        $entries = Select-Entries 'ci/run-unit-tests.ps1'
        ($entries | Where-Object { $_.SeleniumContract } |
            ForEach-Object { $_.Name }) -join ',' | Should -Be 'Ubuntu_Java_17'
        $entries | Where-Object { $_.RunPerformance } | Should -BeNullOrEmpty
    }
}
