param (
    [Parameter(Mandatory)][string]$Product,
    [Parameter(Mandatory)][string]$Example,
    # Given where the clone holds only the data file's link, so the example
    # has to download the file. Left out, or given as -ExpectDownload:$false,
    # where the file is already in place. A switch, so that it works the same
    # with pwsh -File, which passes every other value as a string.
    [switch]$ExpectDownload,
    # Text the output must hold, such as the weighting the IP Intelligence
    # example prints with each value. Empty when there is none.
    [string]$Expect = ''
)

# Runs a product's console example jar from the repository root with no
# settings, as AGENTS.md's "Run the examples" does, and fails unless it ends
# with exit code 0, prints results and any expected text, and downloads its
# data file exactly when expected.

$jar = Get-ChildItem "examples/$Product/console/target" `
    -Filter '*-jar-with-dependencies.jar' | Select-Object -First 1
if ($null -eq $jar) {
    Write-Output '::error::The build made no jar with dependencies.'
    exit 1
}
$output = & java -cp $jar.FullName $Example 2>&1 | ForEach-Object { "$_" }
$code = $LASTEXITCODE
$output
$text = $output -join "`n"
$failed = $false
if ($code -ne 0) {
    Write-Output "::error::The example exited with $code."
    $failed = $true
}
$downloaded = $text -match 'is missing, so it is being downloaded'
if ($ExpectDownload -and -not $downloaded) {
    Write-Output '::error::The example did not download its free data file.'
    $failed = $true
}
if (-not $ExpectDownload -and $downloaded) {
    Write-Output '::error::The example downloaded its data file, which should have come from the cache.'
    $failed = $true
}
if ($text -notmatch 'Results:') {
    Write-Output '::error::The example printed no results.'
    $failed = $true
}
if ($Expect -and -not $text.Contains($Expect)) {
    Write-Output "::error::The example's output does not hold '$Expect'."
    $failed = $true
}
if ($failed) {
    exit 1
}
