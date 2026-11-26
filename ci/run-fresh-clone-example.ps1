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
    [string]$Expect = '',
    # The data repository's link for the data file, which the example
    # follows to download it, and a copy of the data file that the build has
    # already fetched. Given together, a stand-in on this machine serves the
    # copy, and the link names the stand-in while the example runs, so the
    # example downloads nothing from the Distributor. The link is put back
    # as it was afterwards.
    [string]$LinkFile = '',
    [string]$ServeFile = ''
)

# Runs a product's console example jar from the repository root with no
# settings, as AGENTS.md's "Run the examples" does, and fails unless it ends
# with exit code 0, prints results and any expected text, and downloads its
# data file exactly when expected.

# Starts a stand-in for the address a data file's link names. It answers
# every request with the file, compressed as the Distributor sends a free
# data file, with the MD5 hash of the compressed file in lower case
# hexadecimal, and records the request line of each request.
function Start-StandIn([string]$File) {
    $compressed = [IO.Path]::GetTempFileName()
    $source = [IO.File]::OpenRead($File)
    try {
        $target = [IO.File]::Create($compressed)
        try {
            $gzip = [IO.Compression.GZipStream]::new($target,
                [IO.Compression.CompressionLevel]::Fastest)
            try {
                $source.CopyTo($gzip)
            } finally {
                $gzip.Dispose()
            }
        } finally {
            $target.Dispose()
        }
    } finally {
        $source.Dispose()
    }
    $md5 = (Get-FileHash -Algorithm MD5 -LiteralPath $compressed).Hash.ToLowerInvariant()
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $requests = [Collections.Concurrent.ConcurrentQueue[string]]::new()
    $job = Start-ThreadJob -ArgumentList $listener, $compressed, $md5, $requests -ScriptBlock {
        param($listener, $compressed, $md5, $requests)
        while ($true) {
            try {
                $client = $listener.AcceptTcpClient()
            } catch {
                # Stopping the listener ends the wait for a request.
                return
            }
            try {
                $stream = $client.GetStream()
                $reader = [IO.StreamReader]::new($stream, [Text.Encoding]::ASCII,
                    $false, 1024, $true)
                $requests.Enqueue([string]$reader.ReadLine())
                # The headers end with an empty line.
                while ($reader.ReadLine()) { }
                $length = ([IO.FileInfo]::new($compressed)).Length
                $head = [Text.Encoding]::ASCII.GetBytes(
                    "HTTP/1.1 200 OK`r`n" +
                    "Content-Type: application/octet-stream`r`n" +
                    "Content-Length: $length`r`n" +
                    "Content-MD5: $md5`r`n" +
                    "Connection: close`r`n`r`n")
                $stream.Write($head, 0, $head.Length)
                $body = [IO.File]::OpenRead($compressed)
                try {
                    $body.CopyTo($stream)
                } finally {
                    $body.Dispose()
                }
            } catch {
                # A client that goes before the answer is written ends only
                # this answer.
            } finally {
                $client.Dispose()
            }
        }
    }
    $port = ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    [pscustomobject]@{
        Address = "http://127.0.0.1:$port"
        Listener = $listener
        Job = $job
        Requests = $requests
        Compressed = $compressed
    }
}

function Stop-StandIn($StandIn) {
    $StandIn.Listener.Stop()
    $StandIn.Job | Wait-Job -Timeout 60 | Out-Null
    $StandIn.Job | Remove-Job -Force
    Remove-Item -LiteralPath $StandIn.Compressed -Force -ErrorAction SilentlyContinue
}

if ([bool]$LinkFile -ne [bool]$ServeFile) {
    Write-Output '::error::-LinkFile and -ServeFile are given together or not at all.'
    exit 1
}
$jar = Get-ChildItem "examples/$Product/console/target" `
    -Filter '*-jar-with-dependencies.jar' | Select-Object -First 1
if ($null -eq $jar) {
    Write-Output '::error::The build made no jar with dependencies.'
    exit 1
}
$standIn = $null
$link = $null
$original = $null
if ($ServeFile) {
    if (-not (Test-Path -LiteralPath $ServeFile -PathType Leaf)) {
        Write-Output "::error::There is no file to serve at '$ServeFile'."
        exit 1
    }
    if (-not (Test-Path -LiteralPath $LinkFile -PathType Leaf)) {
        Write-Output "::error::There is no link at '$LinkFile'."
        exit 1
    }
    $link = Convert-Path -LiteralPath $LinkFile
    $original = [IO.File]::ReadAllBytes($link)
    $standIn = Start-StandIn (Convert-Path -LiteralPath $ServeFile)
}
try {
    if ($standIn) {
        # The stand-in answers at the path the link names, so the example
        # sees the same address but for the host.
        $address = @([Text.Encoding]::UTF8.GetString($original).Trim() -split "`n")[0].Trim()
        $path = ([Uri]$address).AbsolutePath
        [IO.File]::WriteAllText($link, "$($standIn.Address)$path`n")
    }
    $output = & java -cp $jar.FullName $Example 2>&1 | ForEach-Object { "$_" }
    $code = $LASTEXITCODE
} finally {
    if ($standIn) {
        [IO.File]::WriteAllBytes($link, $original)
        Stop-StandIn $standIn
    }
}
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
if ($standIn) {
    $asked = @($standIn.Requests.ToArray())
    Write-Output "Requests the stand-in answered: $(if ($asked.Count) { $asked -join '; ' } else { 'none' })"
    if ($ExpectDownload -and $asked.Count -eq 0) {
        Write-Output '::error::The example did not download its data file from the stand-in.'
        $failed = $true
    }
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
