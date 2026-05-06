$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

& (Join-Path $Root "build-plugin.ps1")
& (Join-Path $Root "install-plugin.ps1")

Start-Process `
    -FilePath "powershell.exe" `
    -WorkingDirectory $Root `
    -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        (Join-Path $Root "run-dev-server.ps1")
    )
