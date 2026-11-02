param(
    [switch]$Installer
)
# 在 Windows 上构建 app-image；-Installer 额外构建 EXE 安装包（需要 WiX Toolset）。
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Version = "1.0.0"
& (Join-Path $Root "packaging/windows/build-binaries.ps1")
if ($LASTEXITCODE -ne 0) { throw "build binaries failed" }

Push-Location $Root
try {
    & .\gradlew.bat :desktop:createDistributable
    if ($LASTEXITCODE -ne 0) { throw "createDistributable failed" }

    $App = Get-ChildItem (Join-Path $Root "desktop/build/compose/binaries/main/app") -Directory |
        Where-Object { $_.Name -ieq "mexpa" } | Select-Object -First 1
    if (-not $App) { throw "app image not found" }
    # Windows jpackage app-image 的启动器在 app 根目录，mihomo.exe 必须和它同级；
    # 不要复制到 bin/（那是 Linux app-image 的布局）。
    $Target = $App.FullName
    Copy-Item (Join-Path $Root "desktop/build/windows-bin/mihomo.exe") $Target -Force
    Copy-Item (Join-Path $Root "desktop/build/windows-bin/mexpa-transform.exe") $Target -Force
    Copy-Item (Join-Path $Root "desktop/build/windows-bin/wintun.dll") $Target -Force
    Copy-Item (Join-Path $Root "packaging/windows/wintun-LICENSE.txt") $Target -Force

    $Zip = Join-Path $Root "desktop/build/Mexpa-windows-x64-$Version.zip"
    Remove-Item $Zip -Force -ErrorAction SilentlyContinue
    Compress-Archive -Path (Join-Path $App.FullName "*") -DestinationPath $Zip
    Write-Host "built: $Zip"

    if ($Installer) {
        & .\gradlew.bat :desktop:packageExe
        Write-Host "Installer output: desktop/build/compose/binaries/main/exe"
    }
} finally { Pop-Location }
