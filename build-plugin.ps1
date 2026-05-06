$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = Join-Path $Root "tools\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$(Join-Path $Root "tools\apache-maven-3.9.15\bin");$env:Path"

Push-Location (Join-Path $Root "NetworkStorage")
try {
    mvn.cmd clean package
}
finally {
    Pop-Location
}
