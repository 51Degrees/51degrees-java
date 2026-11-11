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
        @{ Line = '${FOD_CLOUD_API_URL}' }
        @{ Line = 'Constants.LEGACY_ENV_VARS' }
        @{ Line = '          LICENSE_KEY: ${{ secrets.DEVICE_DETECTION_KEY }}' }
        @{ Line = '- RESOURCE_KEY names the resource key' }
        @{ Line = 'RESOURCE_KEY=my-key mvn test' }
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
        @{ Line = 'System.getenv("SUPER_RESOURCE_KEY")' }
        @{ Line = 'Set the key with !!YOUR_RESOURCE_KEY!! in the readme.' }
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
}
