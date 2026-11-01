$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
New-Item -ItemType Directory -Force -Path build/classes | Out-Null
$sources = @(Get-ChildItem src/main/java -Filter *.java -Recurse | ForEach-Object FullName)
& $javac --release 17 -encoding UTF-8 -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
if ($args -contains 'run') { & $java -cp build/classes studysprint.Main }
