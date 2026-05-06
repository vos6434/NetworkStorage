$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PluginJar = Join-Path $Root "NetworkStorage\target\NetworkStorage-1.4.0.jar"
$PluginsDir = Join-Path $Root "dev-server\plugins"

if (-not (Test-Path $PluginJar)) {
    throw "Plugin jar not found. Run .\build-plugin.ps1 first."
}

if (-not (Test-Path $PluginsDir)) {
    New-Item -ItemType Directory -Path $PluginsDir | Out-Null
}

Copy-Item $PluginJar (Join-Path $PluginsDir "NetworkStorage-1.4.0.jar") -Force
Write-Host "Installed NetworkStorage into dev-server\plugins."
