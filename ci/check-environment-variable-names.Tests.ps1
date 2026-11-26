# Tests for ci/check-environment-variable-names.ps1.
#
# Each case is one line of text, the path of the file it is in, and what the
# check must make of it. The names are made up, apart from the names of the
# platform and of the tools the build uses, which the check lets through.

BeforeAll {
    $script:Check = Join-Path $PSScriptRoot 'check-environment-variable-names.ps1'
    . $script:Check

    function Get-Problem([string]$Line, [string]$Path = 'src/Any.java') {
        @(Find-EnvironmentNameProblem -Line $Line -Path $Path)
    }

    # A repository of the given files, each a path and its lines, with every
    # file tracked.
    function New-SampleRepository([hashtable]$Files) {
        $repo = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid())
        New-Item -ItemType Directory $repo | Out-Null
        git -C $repo init -q
        foreach ($relative in $Files.Keys) {
            $file = Join-Path $repo $relative
            New-Item -ItemType Directory (Split-Path $file) -Force | Out-Null
            Set-Content -LiteralPath $file -Value $Files[$relative]
        }
        git -C $repo add -A
        $repo
    }
}

Describe 'Test-AllowedName' {

    It 'allows the 51Degrees name <Name>' -ForEach @(
        @{ Name = '_51DEGREES_SAMPLE' }
        @{ Name = '_51DEGREES_SAMPLE_PATH_2' }
    ) {
        Test-AllowedName $Name | Should -BeTrue
    }

    It 'allows the CI name <Name>' -ForEach @(
        @{ Name = 'CLOUD_STAGING_ENDPOINT' }
        @{ Name = 'RESOURCE_KEY_BESPOKE_STAGING' }
        @{ Name = 'LIVE_CLOUD_TESTS' }
    ) {
        Test-AllowedName $Name | Should -BeTrue
    }

    It 'allows the platform name <Name>' -ForEach @(
        @{ Name = 'PATH' }
        @{ Name = 'JAVA_HOME' }
        @{ Name = 'GITHUB_ACTIONS' }
        @{ Name = 'GITHUB_TOKEN' }
        @{ Name = 'CHROME_BIN' }
        @{ Name = 'MVNW_REPOURL' }
        @{ Name = 'os.name' }
        @{ Name = 'java.io.tmpdir' }
        @{ Name = 'user.dir' }
        @{ Name = 'logback.configurationFile' }
        @{ Name = 'maven.multiModuleProjectDirectory' }
    ) {
        Test-AllowedName $Name | Should -BeTrue
    }

    It 'refuses <Name>' -ForEach @(
        @{ Name = 'SAMPLE_NAME' }
        @{ Name = 'cloud_staging_endpoint' }
        @{ Name = 'RESOURCE_KEY_BESPOKE' }
        @{ Name = 'SampleName' }
        @{ Name = 'sample.name' }
        @{ Name = '_51DEGREES_' }
        @{ Name = '_51degrees_sample' }
        @{ Name = '_51DEGREES_Sample' }
        @{ Name = 'X_51DEGREES_SAMPLE' }
        @{ Name = 'path' }
        @{ Name = 'javax.sample' }
        # The shared browser tests give an example its port, address and
        # language under _51DEGREES_ names.
        @{ Name = 'PORT' }
        @{ Name = 'EXAMPLE_URL' }
        @{ Name = 'EXAMPLE_LANG' }
    ) {
        Test-AllowedName $Name | Should -BeFalse
    }
}

Describe 'Find-EnvironmentNameProblem' {

    # A name a POSIX shell cannot set is found wherever it is written and in
    # any case, because Windows reads an environment variable name in any
    # case.
    It 'finds the digit-initial name in <Line>' -ForEach @(
        @{ Line = 'System.getenv("51DEGREES_SAMPLE")' }
        @{ Line = 'public static final String SAMPLE = "51DEGREES_SAMPLE";' }
        @{ Line = '<DataFile>${51DEGREES_SAMPLE:-sample.dat}</DataFile>' }
        @{ Line = "'51DEGREES_SAMPLE' = (Resolve-Path `$file).Path" }
        @{ Line = 'Set 51degrees_sample to the file.' }
    ) {
        (Get-Problem $Line).Kind | Should -Contain 'digit'
    }

    It 'finds the name read in <Line>' -ForEach @(
        @{ Line = 'System.getenv("SAMPLE_NAME")' }
        @{ Line = 'String value = getenv( "SampleName" );' }
        @{ Line = 'System.getProperty("sample.name")' }
        @{ Line = 'System.getProperty("SampleName", "default")' }
        @{ Line = 'System.setProperty("SampleName", file.getPath());' }
        @{ Line = 'System.clearProperty("SampleName");' }
        @{ Line = '$env:SAMPLE_NAME = $Keys.Sample' }
        @{ Line = '${env:SAMPLE_NAME} = "value"' }
        @{ Line = '${Env:SAMPLE_NAME}' }
        @{ Line = '$ENV:SAMPLE_NAME = "value"' }
        @{ Line = 'Remove-Item Env:\SAMPLE_NAME' }
        @{ Line = 'Remove-Item Env:SAMPLE_NAME' }
        @{ Line = '[Environment]::GetEnvironmentVariable("SAMPLE_NAME")' }
        @{ Line = "[Environment]::SetEnvironmentVariable('SAMPLE_NAME', 'value')" }
        @{ Line = '$env:_51degrees_sample = "sample.dat"' }
        @{ Line = 'System.getenv("_51DEGREES_Sample")' }
    ) {
        $problems = Get-Problem $Line
        $problems.Kind | Should -Be @('name')
    }

    It 'gives the name read in <Line> as <Name>' -ForEach @(
        @{ Line = 'System.getenv("SAMPLE_NAME")'; Name = 'SAMPLE_NAME' }
        @{ Line = 'System.getProperty("sample.name")'; Name = 'sample.name' }
        @{ Line = 'Remove-Item Env:\SAMPLE_NAME'; Name = 'SAMPLE_NAME' }
        @{ Line = '${env:SAMPLE_NAME} = "value"'; Name = 'SAMPLE_NAME' }
    ) {
        (Get-Problem $Line).Value | Should -Be @($Name)
    }

    It 'leaves <Line> alone' -ForEach @(
        @{ Line = 'System.getenv("_51DEGREES_SAMPLE")' }
        @{ Line = 'System.getProperty("_51DEGREES_SAMPLE")' }
        @{ Line = 'System.setProperty("_51DEGREES_SAMPLE", file.getPath());' }
        @{ Line = '$env:_51DEGREES_SAMPLE = "sample.dat"' }
        @{ Line = '${env:_51DEGREES_SAMPLE} = $Keys.Sample' }
        @{ Line = 'Remove-Item Env:_51DEGREES_SAMPLE' }
        @{ Line = 'System.getenv("PATH")' }
        @{ Line = '"true".equals(System.getenv("GITHUB_ACTIONS"))' }
        @{ Line = 'System.getProperty("os.name", "")' }
        @{ Line = 'System.getProperty("java.io.tmpdir")' }
        @{ Line = '$env:JAVA_HOME = $home' }
        @{ Line = 'if ($env:CI) { return }' }
        # A name held in a variable or constant is read where it is defined.
        @{ Line = 'System.getenv(name)' }
        @{ Line = 'System.getProperty(SAMPLE_CONSTANT)' }
        # Not reads of the environment or of a system property.
        @{ Line = 'assertEquals("sample_name=" + key, query);' }
        @{ Line = 'json.getProperty("SampleName")' }
        @{ Line = 'data.get("SampleName").toString()' }
        @{ Line = '                TestSample = $TestSample' }
        @{ Line = 'Set the key with !!YOUR_SAMPLE_NAME!! in the readme.' }
        @{ Line = '<version>${sample.version}</version>' }
    ) {
        Get-Problem $Line | Should -BeNullOrEmpty
    }

    # An options file names a variable or system property as a placeholder,
    # which the options lookup reads.
    Context 'in an options file' {

        It 'finds the placeholder in <Line>' -ForEach @(
            @{ Line = '<DataFile>${SampleName}</DataFile>' }
            @{ Line = '<DataFile>${SampleName:-sample.dat}</DataFile>' }
            @{ Line = '<EndPoint>${env:SAMPLE_NAME}</EndPoint>' }
            @{ Line = '<EndPoint>${sys:sample.name:-https://example.com}</EndPoint>' }
            @{ Line = '<EndPoint>${envsys:SAMPLE_NAME}</EndPoint>' }
            @{ Line = '<A>${_51DEGREES_SAMPLE:-${SampleName:-sample.dat}}</A>' }
        ) {
            (Get-Problem $Line 'src/main/resources/sample.xml').Kind |
                Should -Be @('name')
        }

        It 'leaves <Line> alone' -ForEach @(
            @{ Line = '<DataFile>${_51DEGREES_SAMPLE}</DataFile>' }
            @{ Line = '<DataFile>${_51DEGREES_SAMPLE:-sample.dat}</DataFile>' }
            @{ Line = '<EndPoint>${envsys:_51DEGREES_SAMPLE}</EndPoint>' }
            @{ Line = '<File>${filepath:sample.dat}</File>' }
            @{ Line = '<Home>${sys:user.home}</Home>' }
        ) {
            Get-Problem $Line 'src/main/resources/sample.xml' |
                Should -BeNullOrEmpty
        }

        # A Maven project file and a page template use the same form for
        # their own properties and fields.
        It 'leaves a placeholder in <Path> alone' -ForEach @(
            @{ Path = 'pom.xml' }
            @{ Path = 'sample/pom.xml' }
            @{ Path = 'src/main/webapp/WEB-INF/html/index.html' }
            @{ Path = 'src/Any.java' }
        ) {
            Get-Problem '<a>${SampleName}</a>' $Path | Should -BeNullOrEmpty
        }
    }

    Context 'in a Maven project file' {

        It 'finds the environment variable in <Line>' -ForEach @(
            @{ Line = '<sample>${env.SAMPLE_NAME}</sample>' }
        ) {
            (Get-Problem $Line 'sample/pom.xml').Value |
                Should -Be @('SAMPLE_NAME')
        }

        It 'leaves <Line> alone' -ForEach @(
            @{ Line = '<sample>${env.JAVA_HOME}</sample>' }
            @{ Line = '<sample>${env._51DEGREES_SAMPLE}</sample>' }
            @{ Line = '<sample>${project.version}</sample>' }
        ) {
            Get-Problem $Line 'sample/pom.xml' | Should -BeNullOrEmpty
        }
    }

    Context 'in a workflow' {

        It 'finds the name in <Line>' -ForEach @(
            @{ Line = '          url: ${{ env.SAMPLE_NAME }}' }
            @{ Line = '          key: ${{ secrets.SAMPLE_NAME }}' }
            @{ Line = '          address: ${{ vars.SAMPLE_NAME }}' }
            @{ Line = '        if: secrets.SAMPLE_NAME != ''''' }
        ) {
            (Get-Problem $Line '.github/workflows/sample.yml').Value |
                Should -Be @('SAMPLE_NAME')
        }

        It 'leaves <Line> alone' -ForEach @(
            @{ Line = '          key: ${{ secrets._51DEGREES_SAMPLE }}' }
            @{ Line = '          address: ${{ vars._51DEGREES_SAMPLE }}' }
            @{ Line = '          token: ${{ secrets.GITHUB_TOKEN }}' }
            @{ Line = '    secrets: inherit' }
            @{ Line = '          ref: ${{ inputs.sample-ref }}' }
            @{ Line = '          os: ${{ matrix.os }}' }
        ) {
            Get-Problem $Line '.github/workflows/sample.yml' |
                Should -BeNullOrEmpty
        }
    }
}

Describe 'Find-BlockNameProblem' {

    It 'finds a name a workflow sets in an env block' {
        $lines = @(
            'jobs:'
            '  sample:'
            '    env:'
            '      _51DEGREES_SAMPLE: ${{ secrets._51DEGREES_SAMPLE }}'
            '      SAMPLE_NAME: value'
            '    steps:'
            '      - name: Sample'
            '        env:'
            '          # A comment.'
            '          OtherSample: value'
            '          JAVA_HOME: /sample'
            '        run: |'
            '          SampleKey: not in an env block'
        )
        $problems = @(Find-BlockNameProblem -Lines $lines `
            -Path '.github/workflows/sample.yml')
        $problems.Value | Should -Be @('SAMPLE_NAME', 'OtherSample')
        $problems.LineNumber | Should -Be @(5, 10)
    }

    It 'finds a system property a Maven project file sets for the tests' {
        $lines = @(
            '<configuration>'
            '    <systemPropertyVariables>'
            '        <!-- A comment. -->'
            '        <logback.configurationFile>${basedir}/logback.xml</logback.configurationFile>'
            '        <sample.name>${basedir}</sample.name>'
            '        <_51DEGREES_SAMPLE>${basedir}</_51DEGREES_SAMPLE>'
            '    </systemPropertyVariables>'
            '    <environmentVariables>'
            '        <SAMPLE_NAME>value</SAMPLE_NAME>'
            '    </environmentVariables>'
            '    <sample.other>value</sample.other>'
            '</configuration>'
        )
        $problems = @(Find-BlockNameProblem -Lines $lines -Path 'sample/pom.xml')
        $problems.Value | Should -Be @('sample.name', 'SAMPLE_NAME')
        $problems.LineNumber | Should -Be @(5, 9)
    }

    It 'reads no block in <Path>' -ForEach @(
        @{ Path = 'ci/sample.yml' }
        @{ Path = 'src/main/resources/sample.xml' }
    ) {
        $lines = @('env:', '  SAMPLE_NAME: value', '<systemPropertyVariables>',
            '<sample.name>value</sample.name>', '</systemPropertyVariables>')
        Find-BlockNameProblem -Lines $lines -Path $Path | Should -BeNullOrEmpty
    }
}

Describe 'the check over a repository' {

    AfterEach {
        if ($script:Repo) {
            Remove-Item -Recurse -Force $script:Repo
            $script:Repo = $null
        }
    }

    # git quotes a path holding a non-ASCII character unless told not to,
    # and a quoted path is not found on disk.
    It 'reads a file whose name is not ASCII' {
        $name = 'Donn' + [char]0xE9 + 'es.java'
        $script:Repo = New-SampleRepository @{
            $name = 'System.getenv("51DEGREES_SAMPLE");'
        }
        { & $script:Check -RepoRoot $script:Repo } |
            Should -Throw '1 names start with a digit*'
    }

    It 'fails on a name that is not a 51Degrees or platform name' {
        $script:Repo = New-SampleRepository @{
            'src/Sample.java' = 'String key = System.getenv("SAMPLE_NAME");'
            'src/main/resources/sample.xml' = '<A>${SampleName}</A>'
            'sample/pom.xml' = @(
                '<systemPropertyVariables>'
                '<sample.name>value</sample.name>'
                '</systemPropertyVariables>')
        }
        { & $script:Check -RepoRoot $script:Repo } |
            Should -Throw '*and 3 names are read or set that are not*'
    }

    It 'passes when every name is a 51Degrees or platform name' {
        $script:Repo = New-SampleRepository @{
            'src/Sample.java' = @(
                'String key = System.getenv("_51DEGREES_SAMPLE");'
                'String os = System.getProperty("os.name");')
            'src/main/resources/sample.xml' = '<A>${_51DEGREES_SAMPLE:-a}</A>'
            'ci/sample.ps1' = '$env:_51DEGREES_SAMPLE = $env:JAVA_HOME'
        }
        { & $script:Check -RepoRoot $script:Repo } | Should -Not -Throw
    }
}
