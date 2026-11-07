param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys,
    [string]$Name
)
$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

. "$PSScriptRoot/release.ps1"

# Builds the packages this publish releases, each at its own version from the
# release plan (see release.ps1). $Version is common-ci's label for the run and
# is not used.
$plan = @(Get-BuildPlan $RepoName)
Write-Host "Building $($plan.Count) packages:"
$plan | Format-Table Name, Previous, Version, Reason -AutoSize | Out-String -Width 200 |
    Write-Host

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

# Every published module gets its own version, and the versions of its parent
# and of the packages it depends on, including those this publish does not
# release, which stay at their last release.
Set-PomVersions $RepoName (Get-BuildVersions $RepoName $plan)

# Only the modules in the plan are deployed, so the one publishing bundle holds
# only them. A package they depend on that is not released is taken from Maven
# Central at its last release, the version their poms now require.
$modules = @($plan | ForEach-Object { Get-ModulePath $_ })

# common-ci deploys from the root to a local staging folder with signing, and
# copies the result and the publishing bundle into the package folder. The
# versions are already set, so it is given no folder to set them in.
./java/build-package.ps1 `
    -RepoName $RepoName `
    -Name $Name `
    -Version $Version `
    -VersionPaths @() `
    -ExtraArgs "-DskipNativeBuild=true", "-pl", ($modules -join ',') `
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
