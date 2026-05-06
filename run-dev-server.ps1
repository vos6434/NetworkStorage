$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServerDir = Join-Path $Root "dev-server"
$Java = Join-Path $Root "tools\jdk-25\bin\java.exe"
$PaperJar = Get-ChildItem $ServerDir -Filter "paper-*.jar" | Select-Object -First 1

if ($null -eq $PaperJar) {
    throw "Paper jar not found in dev-server."
}

Push-Location $ServerDir
try {
    & $Java -Xms1G -Xmx2G -jar $PaperJar.Name nogui
}
finally {
    Pop-Location
}
