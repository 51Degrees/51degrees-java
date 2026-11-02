param(
    [Parameter(Mandatory)]$RepoName,
    [string]$Name
)

$RepoPath = [IO.Path]::Combine($pwd, $RepoName)
$PathToBinaries = "$RepoPath/ip-intelligence.engine.on-premise/target/classes"

./java/build-package-requirements.ps1 @PSBoundParameters

$Files = Get-ChildItem -Path $PathToBinaries/* -Include "*.dll", "*.so", "*.dylib"

# Create a directory for binary files from which they will be uploaded
# as artifacts.
$PackageFolder = "package-files"
New-Item -path $PackageFolder  -ItemType Directory -Force 

# Copy binary files over 
foreach($file in $Files){
    Copy-Item -Path $file -Destination "$PackageFolder/$($file.Name)"
}

exit $LASTEXITCODE
