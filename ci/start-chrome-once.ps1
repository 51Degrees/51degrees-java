# Starts Chrome and its driver once, waits for them to finish and prints how
# long each took. The first start of Chrome on a fresh runner takes many times
# longer than the starts after it, so the Selenium contract tests run this
# first and that cost falls outside the limits of their tests. A start that
# takes longer than the limit is stopped and reported as a warning, and the
# tests then run as they would have without it.
param(
    # The longest each first start is given.
    [int]$LimitSeconds = 300
)
$ErrorActionPreference = "Stop"

# The browser the contract tests find when nothing names one, the first of
# these names on the path, then the place Chrome installs itself.
function Find-Chrome {
    foreach ($name in 'google-chrome', 'google-chrome-stable', 'chromium-browser', 'chromium', 'chrome.exe') {
        $command = Get-Command $name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) {
            return $command.Source
        }
    }
    $installed = if ($IsMacOS) {
        '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
    } elseif ($IsWindows) {
        Join-Path ([Environment]::GetFolderPath('ProgramFiles')) 'Google\Chrome\Application\chrome.exe'
    }
    if ($installed -and (Test-Path $installed)) {
        return $installed
    }
    return $null
}

# Runs a program, stopping it after the limit, and gives the seconds it took
# or -1 when it was stopped.
function Measure-Start {
    param([string]$FilePath, [string[]]$Arguments)
    $output = [System.IO.Path]::GetTempFileName()
    $errors = [System.IO.Path]::GetTempFileName()
    try {
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        $process = Start-Process -FilePath $FilePath -ArgumentList $Arguments -PassThru -NoNewWindow `
            -RedirectStandardOutput $output -RedirectStandardError $errors
        if (-not $process.WaitForExit($LimitSeconds * 1000)) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            return -1
        }
        return [math]::Round($watch.Elapsed.TotalSeconds, 1)
    } finally {
        Remove-Item $output, $errors -Force -ErrorAction SilentlyContinue
    }
}

$driver = Get-Command chromedriver -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if ($driver) {
    $seconds = Measure-Start -FilePath $driver.Source -Arguments '--version'
    Write-Host "Started the Chrome driver once in $seconds s"
}

$chrome = Find-Chrome
if (-not $chrome) {
    Write-Host "::warning title=Chrome not started::No Chrome was found to start before the Selenium contract tests."
    return
}
$userDataDir = Join-Path ([System.IO.Path]::GetTempPath()) "chrome-first-start-$PID"
try {
    $seconds = Measure-Start -FilePath $chrome -Arguments @(
        '--headless', '--disable-gpu', '--no-first-run', '--use-mock-keychain',
        "--user-data-dir=$userDataDir", '--dump-dom', 'about:blank')
    if ($seconds -lt 0) {
        Write-Host "::warning title=Chrome slow to start::Chrome did not finish its first start within $LimitSeconds s, so it was stopped."
    } else {
        Write-Host "Started Chrome once before the Selenium contract tests in $seconds s"
    }
} finally {
    Remove-Item -Recurse -Force $userDataDir -ErrorAction SilentlyContinue
}
