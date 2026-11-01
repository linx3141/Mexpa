# 构建 Windows 版 mihomo / mexa-transform，并准备 wintun.dll。
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$Out = Join-Path $Root "desktop/build/windows-bin"
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$env:GOOS = "windows"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "0"

Push-Location (Join-Path $Root "mihomo")
try {
    go build -tags "with_gvisor mishka" -trimpath -buildvcs=false -o (Join-Path $Out "mihomo.exe") .
} finally { Pop-Location }

Push-Location (Join-Path $Root "desktop/transform")
try {
    go build -trimpath -o (Join-Path $Out "mexpa-transform.exe") .
} finally { Pop-Location }

$Wintun = Join-Path $Out "wintun.dll"
if (-not (Test-Path $Wintun)) {
    $Zip = Join-Path $env:TEMP "wintun-0.14.1.zip"
    Invoke-WebRequest -Uri "https://www.wintun.net/builds/wintun-0.14.1.zip" -OutFile $Zip
    $Extract = Join-Path $env:TEMP "wintun-0.14.1"
    Remove-Item -Recurse -Force $Extract -ErrorAction SilentlyContinue
    Expand-Archive $Zip -DestinationPath $Extract -Force
    Copy-Item (Join-Path $Extract "bin/amd64/wintun.dll") $Wintun
}
Write-Host "Windows binaries: $Out"
