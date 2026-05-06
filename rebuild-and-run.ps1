$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $Root "build-plugin.ps1")
& (Join-Path $Root "install-plugin.ps1")
& (Join-Path $Root "run-dev-server.ps1")
