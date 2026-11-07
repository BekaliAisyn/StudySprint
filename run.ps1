param([switch]$Check)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$candidates = @()
if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
$onPath = Get-Command java.exe -ErrorAction SilentlyContinue
if ($onPath) { $candidates += $onPath.Source }
$jdks = Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $jdks) {
    $candidates += Get-ChildItem -LiteralPath $jdks -Directory | ForEach-Object { Join-Path $_.FullName 'bin/java.exe' }
}
foreach ($candidate in ($candidates | Select-Object -Unique)) {
    if (!(Test-Path -LiteralPath $candidate)) { continue }
    $version = (& $candidate -version 2>&1 | Out-String)
    if ($version -match 'version "(\d+)' -and [int]$Matches[1] -ge 17) {
        Write-Host "Using $candidate"
        if ($Check) { exit 0 }
        & $candidate -jar (Join-Path $PSScriptRoot 'StudySprint.jar')
        exit $LASTEXITCODE
    }
}
throw 'Java 17 or newer is required. Install a JDK and set JAVA_HOME to its folder.'
