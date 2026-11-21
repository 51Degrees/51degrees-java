# Tests for ci/check-environment-variable-names.ps1.
#
# Each case is one line of text and what the check must make of it. The
# digit-initial name is matched in any case, because Windows reads an
# environment variable name in any case, so a lower case old name is still
# the old name there. The old names keep their case, for the reason the check
# gives.

BeforeAll {
    $script:Check = Join-Path $PSScriptRoot 'check-environment-variable-names.ps1'
    . $script:Check

    function Get-Kind([string]$Line) {
        @(Find-EnvironmentNameProblem $Line | ForEach-Object { $_.Kind })
    }
}

Describe 'Find-EnvironmentNameProblem' {

    It 'finds the digit-initial name <Line>' -ForEach @(
        @{ Line = 'System.getenv("51DEGREES_IPI_PATH")' }
        @{ Line = 'public static final String IPI = "51DEGREES_IPI_PATH";' }
        @{ Line = '<DataFile>${51DEGREES_DD_PATH:-lite.hash}</DataFile>' }
        @{ Line = "'51DEGREES_DD_PATH' = (Resolve-Path `$file).Path" }
        @{ Line = 'System.getenv("51degrees_ipi_path")' }
        @{ Line = '$env:51Degrees_Resource_Key = "key"' }
    ) {
        Get-Kind $Line | Should -Contain 'digit'
    }

    It 'finds the old name <Line>' -ForEach @(
        @{ Line = 'System.getenv("RESOURCE_KEY")' }
        @{ Line = 'env("_51DEGREES_RESOURCE_KEY", "RESOURCE_KEY");' }
        @{ Line = 'env("_51DEGREES_LICENSE_KEY", "LICENSE_KEY");' }
        @{ Line = 'public static final String FOD = "FOD_CLOUD_API_URL";' }
        @{ Line = '$env:LICENSE_KEY = $Keys.TestResourceKey' }
        @{ Line = '${env:LICENSE_KEY} = $Keys.TestResourceKey' }
        @{ Line = '${Env:RESOURCE_KEY}' }
        @{ Line = '$ENV:RESOURCE_KEY = "key"' }
        @{ Line = 'Remove-Item Env:\LICENSE_KEY' }
        @{ Line = '<Endpoint>${env:FOD_CLOUD_API_URL}</Endpoint>' }
        @{ Line = '          url: ${{ env.FOD_CLOUD_API_URL }}' }
        @{ Line = '${FOD_CLOUD_API_URL}' }
        @{ Line = 'Constants.LEGACY_ENV_VARS' }
        @{ Line = '          LICENSE_KEY: ${{ secrets.DEVICE_DETECTION_KEY }}' }
        @{ Line = '- RESOURCE_KEY names the resource key' }
        @{ Line = 'RESOURCE_KEY=my-key mvn test' }
    ) {
        Get-Kind $Line | Should -Contain 'fallback'
    }

    # The names this repository's tests, examples and CI scripts used for
    # keys and the cloud address before each had a _51DEGREES_ name, read as
    # a Java string, a system property passed with -D, a PowerShell
    # parameter or variable, a placeholder in a configuration file, or a key
    # in a hashtable.
    It 'finds the old key name <Line>' -ForEach @(
        @{ Line = '.setResourceKey(TestKeyUtils.getNamedKey("TestResourceKey"))' }
        @{ Line = 'public static final String RESOURCE_KEY_ENV_VAR = "SuperResourceKey";' }
        @{ Line = 'UPDATE_EXAMPLE_LICENSE_KEY_NAME = "LicenseKey";' }
        @{ Line = '        "TestLicenseKey",' }
        @{ Line = '        "_51DEGREES_LICENSE_KEY_CLOUDV5BESPOKE",' }
        @{ Line = 'CLOUD_END_POINT_NAME = "TestCloudEndPoint";' }
        @{ Line = '<EndPoint>${TestCloudEndpoint:-https://cloud.51degrees.com/api/v4}</EndPoint>' }
        @{ Line = '<ResourceKey>${SuperResourceKey}</ResourceKey>' }
        @{ Line = '<AcceptChNoneKey>${AcceptChNoneKey}</AcceptChNoneKey>' }
        @{ Line = '    "-DTestResourceKey=$TestResourceKey"' }
        @{ Line = '    "-DLicenseKey=$DeviceDetection"' }
        @{ Line = '    [string]$TestResourceKey,' }
        @{ Line = '                TestResourceKey = $TestResourceKey' }
        @{ Line = 'mvn clean test -DTestResourceKey=[Resource Key]' }
    ) {
        Get-Kind $Line | Should -Contain 'fallback'
    }

    # The cloud request engine's tests name the query string parameter
    # resource_key, which is on the wire and not in the environment, so the
    # old names keep their case.
    It 'leaves <Line> alone' -ForEach @(
        @{ Line = 'assertEquals("resource_key=" + key, query);' }
        @{ Line = '          DeviceDetection: ${{ secrets.DEVICE_DETECTION_KEY }}' }
        @{ Line = '  PAID_RESOURCE_KEY: ${{ secrets.X }}' }
        @{ Line = 'System.getenv("_51DEGREES_RESOURCE_KEY")' }
        @{ Line = 'System.getenv("_51DEGREES_CLOUD_ENDPOINT")' }
        @{ Line = '$env:_51DEGREES_DD_PATH = "file.hash"' }
        @{ Line = '$env:_51degrees_dd_path = "file.hash"' }
        @{ Line = '${env:_51DEGREES_LICENSE_KEY} = $Keys.TestResourceKey' }
        @{ Line = 'assertEquals("${env:user.home}", substitutor.replace(line));' }
        @{ Line = '  PAID_RESOURCE_KEY: ${{ env.PAID_RESOURCE_KEY }}' }
        @{ Line = 'System.getenv("SUPER_RESOURCE_KEY")' }
        @{ Line = 'Set the key with !!YOUR_RESOURCE_KEY!! in the readme.' }
        @{ Line = ' * https://distributor.51degrees.com/api/v2/download?LicenseKeys=<your_license_key>' }
        @{ Line = '                    .setDataUpdateLicenseKey(licenseKey)' }
        @{ Line = ' * method for a configuration option named ''LicenseKey''. If the' }
        @{ Line = '        "_51DEGREES_LICENSE_KEY_BESPOKE",' }
        @{ Line = '        resourceKey = KeyHelper.getOrSetTestResourceKey(resourceKey);' }
        @{ Line = '<ResourceKey>${_51DEGREES_RESOURCE_KEY_SUPER}</ResourceKey>' }
    ) {
        Get-Kind $Line | Should -BeNullOrEmpty
    }
}

Describe 'the check over a repository' {

    # git quotes a path holding a non-ASCII character unless told not to,
    # and a quoted path is not found on disk.
    It 'reads a file whose name is not ASCII' {
        $repo = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid())
        New-Item -ItemType Directory $repo | Out-Null
        try {
            git -C $repo init -q
            $name = 'Donn' + [char]0xE9 + 'es.java'
            Set-Content -LiteralPath (Join-Path $repo $name) `
                -Value 'System.getenv("51DEGREES_DD_PATH");'
            git -C $repo add -A
            { & $script:Check -RepoRoot $repo } |
                Should -Throw '1 names start with a digit*'
        } finally {
            Remove-Item -Recurse -Force $repo
        }
    }

    It 'passes an old name in a file that keeps a deprecated constant holding it' {
        $repo = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid())
        New-Item -ItemType Directory $repo | Out-Null
        try {
            git -C $repo init -q
            $file = Join-Path $repo 'pipeline/pipeline.did/src/main/java/fiftyone/pipeline/did/DidClient.java'
            New-Item -ItemType Directory (Split-Path $file) -Force | Out-Null
            Set-Content -LiteralPath $file `
                -Value 'String ENDPOINT_VARIABLE = "FOD_CLOUD_API_URL";'
            git -C $repo add -A
            { & $script:Check -RepoRoot $repo } | Should -Not -Throw
        } finally {
            Remove-Item -Recurse -Force $repo
        }
    }

    It 'fails on the same old name in a file that is not named for it' {
        $repo = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid())
        New-Item -ItemType Directory $repo | Out-Null
        try {
            git -C $repo init -q
            Set-Content -LiteralPath (Join-Path $repo 'Other.java') `
                -Value 'String ENDPOINT_VARIABLE = "FOD_CLOUD_API_URL";'
            git -C $repo add -A
            { & $script:Check -RepoRoot $repo } |
                Should -Throw '*and 1 old names are read*'
        } finally {
            Remove-Item -Recurse -Force $repo
        }
    }
}

Describe 'Test-DeprecatedFallback' {

    # An old name is allowed only in a file that keeps a deprecated constant
    # holding it or tests that constant, and only the names listed for that
    # file.
    It 'allows <Value> in <Path>' -ForEach @(
        @{ Path = 'pipeline/pipeline.did/src/main/java/fiftyone/pipeline/did/DidClient.java'; Value = '"FOD_CLOUD_API_URL' }
        @{ Path = 'ip-intelligence/ip-intelligence.shared/src/main/java/fiftyone/ipintelligence/shared/testhelpers/FileUtils.java'; Value = '51DEGREES_IPI_PATH' }
        @{ Path = 'examples/ip-intelligence/shared/src/main/java/fiftyone/ipintelligence/examples/shared/KeyHelper.java'; Value = '51DEGREES_RESOURCE_KEY' }
    ) {
        Test-DeprecatedFallback $Path $Value | Should -BeTrue
    }

    It 'refuses <Value> in <Path>' -ForEach @(
        @{ Path = 'pipeline/pipeline.core/src/main/java/Any.java'; Value = '"FOD_CLOUD_API_URL' }
        @{ Path = 'ci/run-unit-tests.ps1'; Value = 'env:RESOURCE_KEY' }
        @{ Path = 'pipeline/pipeline.did/src/main/java/fiftyone/pipeline/did/DidClient.java'; Value = '"RESOURCE_KEY' }
        @{ Path = 'pipeline/pipeline.did/src/main/java/fiftyone/pipeline/did/DidClient.java'; Value = 'LEGACY_ENV_VARS' }
        @{ Path = 'pipeline/pipeline.did/src/test/java/fiftyone/pipeline/did/LiveCloudKeys.java'; Value = '"FOD_CLOUD_API_URL' }
        @{ Path = 'ip-intelligence/ip-intelligence.shared/src/main/java/fiftyone/ipintelligence/shared/testhelpers/FileUtils.java'; Value = '51DEGREES_DD_PATH' }
        @{ Path = 'ip-intelligence/ip-intelligence.shared/src/main/java/fiftyone/ipintelligence/shared/testhelpers/FileUtils.java'; Value = '51degrees_ipi_path' }
        @{ Path = 'examples/device-detection/web/getting-started.onprem/src/main/webapp/WEB-INF/51Degrees-OnPrem.xml'; Value = '51DEGREES_DD_PATH' }
        @{ Path = 'pipeline/pipeline.did/src/test/java/fiftyone/pipeline/did/LiveCloudKeys.java'; Value = '"RESOURCE_KEY' }
    ) {
        Test-DeprecatedFallback $Path $Value | Should -BeFalse
    }
}

Describe 'the workflows' {

    # The 51Did license key unseals only results made with the resource key
    # of its own account. Filled from the Bespoke key, the licensed redeem
    # comes back unreadable, so the 51Did resource key is taken from the
    # secret of its own name.
    It 'fill _51DEGREES_RESOURCE_KEY_51DID from the secret of the same name' {
        $workflows = Join-Path $PSScriptRoot '../.github/workflows'
        $lines = @(Get-ChildItem $workflows -Filter *.yml |
            Select-String -Pattern '^\s*_51DEGREES_RESOURCE_KEY_51DID\s*:' |
            ForEach-Object { "$($_.Filename):$($_.LineNumber): $($_.Line.Trim())" })
        $lines | Should -Not -BeNullOrEmpty
        $lines | Where-Object {
            $_ -notmatch ':\s*\$\{\{\s*secrets\._51DEGREES_RESOURCE_KEY_51DID\s*\}\}\s*$'
        } | Should -BeNullOrEmpty
    }

    # cloud-51did.yml runs the 51Did tests once for each resource key. The
    # tests redeem with _51DEGREES_LICENSE_KEY_51DID, which unseals only
    # results made with _51DEGREES_RESOURCE_KEY_51DID. So each pass gets only
    # the license whose suffix matches its resource key, and the Bespoke pass
    # gets none. The step's script runs here with mvn replaced by a function
    # that records the keys each pass hands it.
    Context 'cloud-51did.yml' {

        BeforeAll {
            $workflow = Join-Path $PSScriptRoot '../.github/workflows/cloud-51did.yml'
            $lines = @(Get-Content $workflow)
            $start = $lines | Select-String -Pattern '^(\s*)run: \|\s*$' |
                Select-Object -First 1
            $indent = $start.Matches[0].Groups[1].Value.Length
            $body = foreach ($line in $lines[$start.LineNumber..($lines.Count - 1)]) {
                if ($line.Trim() -and $line -notmatch "^\s{$($indent + 1),}") { break }
                if ($line.Length -gt $indent + 2) { $line.Substring($indent + 2) } else { '' }
            }
            $script:Step = [scriptblock]::Create($body -join "`n")
        }

        BeforeEach {
            $script:Saved = @(Get-ChildItem env: |
                Where-Object { $_.Name -like '_51DEGREES_*' })
            $script:Saved | ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $env:_51DEGREES_RESOURCE_KEY_51DID = 'resource-51did'
            $env:_51DEGREES_RESOURCE_KEY_BESPOKE = 'resource-bespoke'
            $env:_51DEGREES_LICENSE_KEY_51DID = 'license-51did'
            $env:_51DEGREES_LICENSE_KEY_BESPOKE = 'license-bespoke'
        }

        AfterEach {
            Get-ChildItem env: | Where-Object { $_.Name -like '_51DEGREES_*' } |
                ForEach-Object { Remove-Item "Env:$($_.Name)" }
            $script:Saved | ForEach-Object { Set-Item "Env:$($_.Name)" $_.Value }
        }

        It 'redeems with the 51Did license only in the pass with the 51Did resource key' {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add([pscustomobject]@{
                    Resource = $env:_51DEGREES_RESOURCE_KEY
                    License  = $env:_51DEGREES_LICENSE_KEY_51DID
                })
                $global:LASTEXITCODE = 0
            }
            & $script:Step 6> $null

            @($passes.Resource | Sort-Object) |
                Should -Be @('resource-51did', 'resource-bespoke')
            ($passes | Where-Object Resource -eq 'resource-51did').License |
                Should -Be 'license-51did'
            ($passes | Where-Object Resource -eq 'resource-bespoke').License |
                Should -BeNullOrEmpty
        }

        # A license key with no resource key of its own suffix means a secret
        # is missing. The licensed redeem would then never run, and the job
        # would still pass, so the step fails naming the missing secret.
        It 'fails naming <Resource> when <License> has a value and it is empty' -ForEach @(
            @{ Resource = '_51DEGREES_RESOURCE_KEY_51DID'; License = '_51DEGREES_LICENSE_KEY_51DID' }
            @{ Resource = '_51DEGREES_RESOURCE_KEY_BESPOKE'; License = '_51DEGREES_LICENSE_KEY_BESPOKE' }
        ) {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add($env:_51DEGREES_RESOURCE_KEY)
                $global:LASTEXITCODE = 0
            }
            Remove-Item "Env:$Resource"

            { & $script:Step 6> $null } | Should -Throw "*$Resource*"
            $passes | Should -BeNullOrEmpty
        }

        # A deployment without 51Did has neither secret, and the runner then
        # sets both names empty. The Bespoke pass must still run, with no
        # 51Did license and no failure.
        It 'runs the Bespoke pass alone when both 51Did keys are empty' {
            $passes = [System.Collections.Generic.List[object]]::new()
            function mvn {
                $passes.Add([pscustomobject]@{
                    Resource = $env:_51DEGREES_RESOURCE_KEY
                    License  = $env:_51DEGREES_LICENSE_KEY_51DID
                })
                $global:LASTEXITCODE = 0
            }
            $env:_51DEGREES_RESOURCE_KEY_51DID = ''
            $env:_51DEGREES_LICENSE_KEY_51DID = ''

            { & $script:Step 6> $null } | Should -Not -Throw
            @($passes.Resource) | Should -Be @('resource-bespoke')
            $passes[0].License | Should -BeNullOrEmpty
        }
    }
}
