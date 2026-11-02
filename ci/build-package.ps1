param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys,
    [string]$Name
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/packages.ps1"

# Put the native libraries built on every platform into the engine they came
# from, so each engine's jar carries all of its own and none of the other's.
foreach ($package in $NativePackages) {
    $binaries = New-Item -ItemType Directory -Path "$RepoName/$($package.Path)/target/classes" -Force
    Get-ChildItem -Path package-files -Recurse -Include "*.dll", "*.so", "*.dylib" |
        Where-Object { $_.Name -like $package.Library } |
        ForEach-Object {
            Write-Host "Adding '$($_.Name)' to '$($package.Path)'"
            Copy-Item -Path $_ -Destination $binaries
        }
}

# The root pom.xml is not the parent of the package trees, so setting the
# version at the root does not reach them. Set it in each tree, the examples
# included, so the whole repository carries the version being released.
foreach ($package in $Packages) {
    Write-Host "Setting the version of '$($package.Path)' to '$Version'"
    Push-Location "$RepoName/$($package.Path)"
    try {
        mvn versions:set @MavenOptions "-DnewVersion=$Version" -DgenerateBackupPoms=false
    } finally {
        Pop-Location
    }
}

# The examples are never published. They are left out of the deploy so that
# the last project in the reactor is one the publishing plugin is configured
# for. Maven 3 does not exclude a folder's modules with the folder, so each of
# them is named.
$repoPath = (Resolve-Path $RepoName).Path
$examples = foreach ($package in $Packages | Where-Object { -not $_.Publish }) {
    Get-ChildItem -Path "$RepoName/$($package.Path)" -Recurse -Filter pom.xml |
        Where-Object { $_.FullName -notmatch '[\\/](target|node_modules)[\\/]' } |
        ForEach-Object { '!' + [IO.Path]::GetRelativePath($repoPath, $_.DirectoryName).Replace('\', '/') }
}

# common-ci sets the version of the root pom.xml, deploys from the root to a
# local staging folder with signing, and copies the result and the publishing
# bundle into the package folder.
./java/build-package.ps1 `
    -RepoName $RepoName `
    -Name $Name `
    -Version $Version `
    -ExtraArgs "-DskipNativeBuild=true", "-pl", ($examples -join ',') `
    -JavaGpgKeyPassphrase $Keys['JavaGpgKeyPassphrase'] `
    -JavaPGP $Keys['JavaPGP'] `
    -CodeSigningKeyVaultName: $Keys['CodeSigningKeyVaultName'] `
    -CodeSigningKeyVaultUrl $Keys['CodeSigningKeyVaultUrl'] `
    -CodeSigningKeyVaultClientId $Keys['CodeSigningKeyVaultClientId'] `
    -CodeSigningKeyVaultTenantId $Keys['CodeSigningKeyVaultTenantId'] `
    -CodeSigningKeyVaultClientSecret $Keys['CodeSigningKeyVaultClientSecret'] `
    -CodeSigningKeyVaultCertificateName $Keys['CodeSigningKeyVaultCertificateName'] `
    -CodeSigningKeyVaultCertificateData $Keys['CodeSigningKeyVaultCertificateData']

exit $LASTEXITCODE
